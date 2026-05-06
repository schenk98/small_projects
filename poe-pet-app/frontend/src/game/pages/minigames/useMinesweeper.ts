import { useRef, useState } from 'react'
import type { Dashboard } from '../../../lib/dashboard'
import type { MinigameEndSummary } from '../../../minigames/types'
import {
  MINESWEEP_DIMS,
  createEmptyField,
  exposeAllMinesAfterLoss,
  isWinningBoard,
  placeMines,
  revealCell,
  type MineDifficulty,
} from '../../../minigames/minesweeperLogic'

export function useMinesweeper({
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
  const [difficulty, setDifficulty] = useState<MineDifficulty>('easy')
  const [field, setField] = useState<number[][]>([])
  const [grid, setGrid] = useState<boolean[][] | null>(null)
  const [gameOver, setGameOver] = useState(false)
  const roundEndedRef = useRef(false)

  const start = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('minesweep_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setEndSummary(null)
      await apiJson('/api/minigames/minesweep_ai/start-simple', { method: 'POST' })
      const dim = MINESWEEP_DIMS[difficulty]
      setField(createEmptyField(dim.rows, dim.cols))
      setGrid(null)
      setGameOver(false)
      roundEndedRef.current = false
      setActive()
      openModal()
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Minesweeper'
      if (/energy/i.test(msg)) window.alert(`Minesweeper: ${msg}`)
      setMessage(msg)
    }
  }

  const finish = async (score: number) => {
    if (roundEndedRef.current) return
    roundEndedRef.current = true
    const rawResult = await apiJson('/api/minigames/minesweep_ai/finish-simple', {
      method: 'POST',
      body: JSON.stringify({ score, difficulty }),
    })
    const result = (rawResult ?? {}) as Record<string, unknown>
    const dash = await refresh()
    if (!dash) return
    setEndSummary({
      title: score > 0 ? 'Minesweeper — cleared' : 'Minesweeper — hit a mine',
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

  const click = async (r: number, c: number, active: boolean, locked: boolean) => {
    if (!active || locked || gameOver) return
    const dim = MINESWEEP_DIMS[difficulty]
    const minesPlaced = grid ?? placeMines(dim.rows, dim.cols, dim.mines, r, c)
    if (!grid) setGrid(minesPlaced)
    const baseField = field.length ? field : createEmptyField(dim.rows, dim.cols)
    const { field: nextField, hitMine } = revealCell(baseField, minesPlaced, r, c)
    if (hitMine) {
      const lossField = exposeAllMinesAfterLoss(nextField, minesPlaced, r, c)
      setField(lossField)
      setGameOver(true)
      await finish(0)
      return
    }
    setField(nextField)
    if (isWinningBoard(nextField, minesPlaced)) await finish(2)
  }

  const flag = (r: number, c: number, active: boolean, locked: boolean) => {
    if (!active || locked || gameOver || grid === null) return
    // Toggle flag for unknown cells only.
    setField((f) => {
      const next = f.map((row) => [...row])
      const v = next[r][c]
      if (v === -1) next[r][c] = -2
      else if (v === -2) next[r][c] = -1
      return next
    })
  }

  const abandon = async (active: boolean) => {
    if (!active) return false
    try {
      await apiJson('/api/minigames/minesweep_ai/abandon-simple', { method: 'POST' })
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'Could not record abandon')
    }
    return true
  }

  const reset = () => {
    setField([])
    setGrid(null)
    setGameOver(false)
    roundEndedRef.current = false
  }

  return { difficulty, setDifficulty, field, start, click, flag, abandon, reset }
}

