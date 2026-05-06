import { useRef, useState } from 'react'
import type { Dashboard } from '../../../lib/dashboard'
import type { MinigameEndSummary } from '../../../minigames/types'
import {
  advanceCheckersState,
  createInitialCheckersState,
  generateMoves as checkersGenerateMoves,
  pickAiMove as checkersPickAiMove,
  repairJumpContinuation,
  winnerFromMoves as checkersWinner,
  type CheckersDifficulty,
  type CheckersMove,
  type CheckersState,
} from '../../../minigames/checkersAi'
import { AI_MOVE_DELAY_MS, delay } from '../../../minigames/constants'

export function useCheckers({
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
  const [difficulty, setDifficulty] = useState<CheckersDifficulty>('easy')
  const [state, setState] = useState<CheckersState>(() => createInitialCheckersState())
  const [pick, setPick] = useState<{ r: number; c: number } | null>(null)
  const roundEndedRef = useRef(false)

  const start = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('checkers_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setEndSummary(null)
      await apiJson('/api/minigames/checkers_ai/start-simple', { method: 'POST' })
      setState(createInitialCheckersState())
      setPick(null)
      roundEndedRef.current = false
      setActive()
      openModal()
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Checkers'
      if (/energy/i.test(msg)) window.alert(`Checkers: ${msg}`)
      setMessage(msg)
    }
  }

  const finish = async (score: number, title: string) => {
    if (roundEndedRef.current) return
    roundEndedRef.current = true
    const rawResult = await apiJson('/api/minigames/checkers_ai/finish-simple', {
      method: 'POST',
      body: JSON.stringify({ score, difficulty }),
    })
    const result = (rawResult ?? {}) as Record<string, unknown>
    const dash = await refresh()
    if (!dash) return
    setEndSummary({
      title,
      coinsEarned: Number(result.coinsReward ?? 0),
      happinessDeltaPercent: Number(result.happinessDeltaPercent ?? 0),
      hunger: Math.round(dash.pet.hunger),
      happiness: Math.round(dash.pet.happiness),
      energy: Math.round(dash.pet.energy),
      detailLines: [
        `Difficulty: ${difficulty}`,
        result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
          ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
          : undefined,
      ].filter(Boolean) as string[],
    })
  }

  const runAi = async (startSt: CheckersState) => {
    let cur = startSt
    while (cur.turn === 'ai' && !roundEndedRef.current) {
      await delay(AI_MOVE_DELAY_MS)
      const mv = checkersPickAiMove(cur, difficulty)
      if (!mv) {
        const w = checkersWinner(cur)
        if (w === 'human') await finish(2, 'Checkers — you win')
        else if (w === 'ai') await finish(0, 'Checkers — AI wins')
        return
      }
      cur = advanceCheckersState(cur, mv)
      setState(cur)
      const w = checkersWinner(cur)
      if (w === 'ai') return void finish(0, 'Checkers — AI wins')
      if (w === 'human') return void finish(2, 'Checkers — you win')
    }
  }

  const applyHumanMove = (st: CheckersState, chosen: CheckersMove) => {
    const next = advanceCheckersState(st, chosen)
    setState(next)
    setPick(null)
    const win = checkersWinner(next)
    if (win === 'human') return void finish(2, 'Checkers — you win')
    if (win === 'ai') return void finish(0, 'Checkers — AI wins')
    if (next.turn === 'human') return
    void runAi(next)
  }

  const click = (r: number, c: number, active: boolean, locked: boolean) => {
    if (!active || locked) return
    const st = repairJumpContinuation(state)
    if (st !== state) setState(st)
    if (st.turn === 'ai' && !roundEndedRef.current) return void runAi(st)
    if (st.turn !== 'human') return

    const moves = checkersGenerateMoves(st)
    if (st.jumpContinuation) {
      const { r: jr, c: jc } = st.jumpContinuation
      const chosen = moves.find((mv) => mv.tr === r && mv.tc === c && mv.fr === jr && mv.fc === jc)
      if (chosen) applyHumanMove(st, chosen)
      return
    }

    const legalFrom = new Set(moves.map((m) => `${m.fr},${m.fc}`))
    if (pick === null) {
      if (legalFrom.has(`${r},${c}`)) setPick({ r, c })
      return
    }
    const from = pick
    const chosen = moves.find((mv) => mv.fr === from.r && mv.fc === from.c && mv.tr === r && mv.tc === c)
    if (!chosen) {
      setPick(legalFrom.has(`${r},${c}`) ? { r, c } : null)
      return
    }
    applyHumanMove(st, chosen)
  }

  const abandon = async (active: boolean) => {
    if (!active) return false
    try {
      await apiJson('/api/minigames/checkers_ai/abandon-simple', { method: 'POST' })
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'Could not record abandon')
    }
    return true
  }

  const reset = () => {
    setState(createInitialCheckersState())
    setPick(null)
    roundEndedRef.current = false
  }

  return { difficulty, setDifficulty, state, pick, start, click, abandon, reset }
}

