import { useState } from 'react'
import type { Dashboard } from '../../../lib/dashboard'
import type { MinigameEndSummary } from '../../../minigames/types'

export function usePuzzleSwap({
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
  const [puzzleSize, setPuzzleSize] = useState(3)
  const [puzzleBoard, setPuzzleBoard] = useState<number[]>([])
  const [puzzleImage, setPuzzleImage] = useState('')
  const [puzzleFirst, setPuzzleFirst] = useState<number | null>(null)
  const [puzzleMoves, setPuzzleMoves] = useState(0)
  const [puzzleStartedAt, setPuzzleStartedAt] = useState<number>(0)

  const start = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('puzzle_swap')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setEndSummary(null)
      await apiJson('/api/minigames/puzzle_swap/start-simple', { method: 'POST' })
      const size = puzzleSize
      const arr = Array.from({ length: size * size }, (_, i) => i)
      arr.sort(() => Math.random() - 0.5)
      const images = [
        'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80',
      ]
      setPuzzleImage(images[Math.floor(Math.random() * images.length)])
      setPuzzleBoard(arr)
      setPuzzleFirst(null)
      setPuzzleMoves(0)
      setPuzzleStartedAt(Date.now())
      setActive()
      openModal()
      setMessage('Puzzle started.')
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start puzzle'
      if (/energy/i.test(msg)) window.alert(`Puzzle: ${msg}`)
      setMessage(msg)
    }
  }

  const swap = async (idx: number) => {
    if (puzzleFirst === null) {
      setPuzzleFirst(idx)
      return
    }
    if (puzzleFirst === idx) {
      setPuzzleFirst(null)
      return
    }
    const next = [...puzzleBoard]
    const a = next[puzzleFirst]
    next[puzzleFirst] = next[idx]
    next[idx] = a
    const newMoves = puzzleMoves + 1
    setPuzzleBoard(next)
    setPuzzleFirst(null)
    setPuzzleMoves(newMoves)
    const solved = next.every((v, i) => v === i)
    if (!solved) return

    const elapsedSeconds = Math.max(1, Math.floor((Date.now() - puzzleStartedAt) / 1000))
    const speedBonus = Math.max(0, 180 - elapsedSeconds) / 10
    const score = Math.max(1, Math.floor((puzzleSize * puzzleSize) - newMoves + 8 + speedBonus))
    const rawResult = await apiJson('/api/minigames/puzzle_swap/finish-simple', {
      method: 'POST',
      body: JSON.stringify({ score }),
    })
    const result = (rawResult ?? {}) as Record<string, unknown>
    const dash = await refresh()
    if (!dash) return
    const solvedTiles = Array.from({ length: puzzleSize * puzzleSize }, (_, i) => i)
    setEndSummary({
      title: 'Puzzle solved',
      coinsEarned: Number(result.coinsReward ?? 0),
      happinessDeltaPercent: Number(result.happinessDeltaPercent ?? 0),
      hunger: Math.round(dash.pet.hunger),
      happiness: Math.round(dash.pet.happiness),
      energy: Math.round(dash.pet.energy),
      puzzleSnap: { size: puzzleSize, tiles: solvedTiles, imageUrl: puzzleImage },
      detailLines: [
        `Score: ${score}`,
        result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
          ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
          : undefined,
      ].filter(Boolean) as string[],
    })
  }

  const abandon = async (active: boolean) => {
    if (!active) return false
    try {
      await apiJson('/api/minigames/puzzle_swap/abandon-simple', { method: 'POST' })
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'Could not record abandon')
    }
    return true
  }

  const reset = () => {
    setPuzzleBoard([])
    setPuzzleFirst(null)
    setPuzzleMoves(0)
    setPuzzleStartedAt(0)
    setPuzzleImage('')
  }

  return {
    puzzleSize,
    setPuzzleSize,
    puzzleBoard,
    puzzleFirst,
    puzzleMoves,
    puzzleImage,
    start,
    swap,
    abandon,
    reset,
  }
}

