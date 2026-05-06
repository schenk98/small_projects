import { useState } from 'react'
import type { Dashboard } from '../../../lib/dashboard'
import type { MinigameEndSummary } from '../../../minigames/types'
import { AI_MOVE_DELAY_MS, delay } from '../../../minigames/constants'
import {
  checkWinner,
  createEmptyConnectBoard,
  dropInColumn,
  pickAiMove,
  type ConnectCell,
  type ConnectDifficulty,
} from '../../../minigames/connect4Ai'

export function useConnect4({
  apiJson,
  dashboard,
  refresh,
  setMessage,
  energyCostFor,
  assertEnergy,
  openModal,
  setActive,
  setEndSummary,
}: {
  apiJson: (path: string, init?: RequestInit) => Promise<unknown>
  dashboard: Dashboard
  refresh: () => Promise<Dashboard | undefined>
  setMessage: (m: string) => void
  energyCostFor: (code: string) => number
  assertEnergy: (need: number, have: number) => boolean
  openModal: () => void
  setActive: () => void
  setEndSummary: (s: MinigameEndSummary | null) => void
}) {
  const [difficulty, setDifficulty] = useState<ConnectDifficulty>('easy')
  const [board, setBoard] = useState<ConnectCell[][]>(createEmptyConnectBoard())
  const [turn, setTurn] = useState<'human' | 'ai'>('human')
  const [status, setStatus] = useState('Your move')
  const [humanMoves, setHumanMoves] = useState(0)

  /** Build Connect4 result sheets consistently for win/draw/loss. */
  const buildSummary = (
    title: string,
    dash: Dashboard,
    result: Record<string, unknown>,
    finalBoard: ConnectCell[][],
    detailLines: string[],
  ): MinigameEndSummary => ({
    title,
    coinsEarned: Number(result.coinsReward ?? 0),
    happinessDeltaPercent: Number(result.happinessDeltaPercent ?? 0),
    hunger: Math.round(dash.pet.hunger),
    happiness: Math.round(dash.pet.happiness),
    energy: Math.round(dash.pet.energy),
    connect4Board: finalBoard.map((r) => [...r]),
    detailLines,
  })

  const start = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('connect4_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setEndSummary(null)
      await apiJson('/api/minigames/connect4_ai/start-simple', { method: 'POST' })
      setHumanMoves(0)
      setBoard(createEmptyConnectBoard())
      setTurn('human')
      setStatus('Your move')
      setActive()
      openModal()
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Connect 4'
      if (/energy/i.test(msg)) window.alert(`Connect 4: ${msg}`)
      setMessage(msg)
    }
  }

  const play = async (col: number, active: boolean) => {
    if (!active || turn !== 'human') return
    const humanBoard = dropInColumn(board, col, 1)
    if (!humanBoard) return
    const humanMovesAfter = humanMoves + 1
    setHumanMoves(humanMovesAfter)
    setBoard(humanBoard)

    const finishBody = (score: number) => JSON.stringify({
      score,
      connectDifficulty: difficulty,
      connectHumanMoves: humanMovesAfter,
    })

    const humanWinner = checkWinner(humanBoard)
    if (humanWinner === 1 || humanWinner === 3) {
      const score = humanWinner === 1 ? 2 : 1
      const rawResult = await apiJson('/api/minigames/connect4_ai/finish-simple', { method: 'POST', body: finishBody(score) })
      const result = (rawResult ?? {}) as Record<string, unknown>
      const dash = await refresh()
      setStatus(humanWinner === 1 ? 'You win' : 'Draw')
      if (dash) {
        const detailLines = [
          `Difficulty: ${difficulty} • Your moves: ${humanMovesAfter}`,
          result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
            ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
            : undefined,
        ].filter(Boolean) as string[]
        setEndSummary(buildSummary(
          humanWinner === 1 ? 'Connect 4 — you win' : 'Connect 4 — draw',
          dash,
          result,
          humanBoard,
          detailLines,
        ))
      }
      return
    }

    setTurn('ai')
    setStatus('AI thinking...')
    await delay(AI_MOVE_DELAY_MS)

    let aiCol = pickAiMove(humanBoard, difficulty)
    if (aiCol < 0 || humanBoard[0][aiCol] !== 0) {
      const valid = Array.from({ length: 7 }, (_, c) => c).filter((c) => humanBoard[0][c] === 0)
      aiCol = valid[0] ?? -1
    }
    const aiBoardState = aiCol >= 0 ? (dropInColumn(humanBoard, aiCol, 2) || humanBoard) : humanBoard
    setBoard(aiBoardState)

    const aiWinner = checkWinner(aiBoardState)
    if (aiWinner === 2 || aiWinner === 3) {
      const score = aiWinner === 3 ? 1 : 0
      const rawResult = await apiJson('/api/minigames/connect4_ai/finish-simple', { method: 'POST', body: finishBody(score) })
      const result = (rawResult ?? {}) as Record<string, unknown>
      const dash = await refresh()
      setStatus(aiWinner === 2 ? 'AI wins' : 'Draw')
      if (dash) {
        const detailLines = [
          `Difficulty: ${difficulty} • Your moves: ${humanMovesAfter}`,
          result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
            ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
            : undefined,
        ].filter(Boolean) as string[]
        setEndSummary(buildSummary(
          aiWinner === 2 ? 'Connect 4 — AI wins' : 'Connect 4 — draw',
          dash,
          result,
          aiBoardState,
          detailLines,
        ))
      }
      return
    }

    setTurn('human')
    setStatus('Your move')
  }

  const abandon = async (active: boolean) => {
    if (!active) return false
    try {
      await apiJson('/api/minigames/connect4_ai/abandon-simple', { method: 'POST' })
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'Could not record abandon')
    }
    return true
  }

  const reset = () => {
    setHumanMoves(0)
    setBoard(createEmptyConnectBoard())
    setTurn('human')
    setStatus('Your move')
  }

  return {
    difficulty,
    setDifficulty,
    board,
    status,
    start,
    play,
    abandon,
    reset,
  }
}

