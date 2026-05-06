import { useState } from 'react'
import type { Dashboard } from '../../../lib/dashboard'
import type { MinigameEndSummary } from '../../../minigames/types'

export function useHigherLower({
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
  const [currentNumber, setCurrentNumber] = useState<number | null>(null)
  const [streak, setStreak] = useState(0)

  /**
   * Build the Higher/Lower result sheet in one place so we don't drift between
   * "round over" (loss) and "finished" (quit/claim) variants.
   */
  const buildSummary = (
    title: string,
    dash: Dashboard,
    data: Record<string, unknown>,
    snap: MinigameEndSummary['hlSnap'] | undefined,
    detailLines: string[],
  ): MinigameEndSummary => ({
    title,
    coinsEarned: Number(data.coinsReward ?? 0),
    happinessDeltaPercent: Number(data.happinessDeltaPercent ?? 0),
    hunger: Math.round(dash.pet.hunger),
    happiness: Math.round(dash.pet.happiness),
    energy: Math.round(dash.pet.energy),
    hlSnap: snap,
    detailLines,
  })

  const start = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('higher_lower')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setEndSummary(null)
      const data = await apiJson('/api/minigames/higher-lower/start', { method: 'POST' })
      const d = (data ?? {}) as Record<string, unknown>
      if (typeof d.error === 'string') return setMessage(d.error)
      setCurrentNumber(Number(d.currentNumber ?? 0))
      setStreak(Number(d.streak ?? 0))
      setActive()
      openModal()
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start minigame'
      if (/energy/i.test(msg)) window.alert(`Higher/Lower: ${msg}`)
      setMessage(msg)
    }
  }

  const guess = async (direction: 'HIGHER' | 'LOWER') => {
    try {
      const data = await apiJson('/api/minigames/higher-lower/guess', { method: 'POST', body: JSON.stringify({ guess: direction }) })
      const d = (data ?? {}) as Record<string, unknown>
      if (typeof d.error === 'string') return setMessage(d.error)
      setCurrentNumber(Number(d.next ?? 0))
      setStreak(Number(d.streak ?? 0))
      const dash = await refresh()
      if (Boolean(d.gameOver) && dash) {
        const detailLines = [
          `Streak credited: ${Number(d.streak ?? 0)}`,
          d.coinsBaseBeforeMultiplier != null && d.coinsBaseBeforeMultiplier !== d.coinsReward
            ? `Base coins: ${d.coinsBaseBeforeMultiplier} × multiplier ${d.coinMultiplierApplied}`
            : undefined,
        ].filter(Boolean) as string[]
        setEndSummary(buildSummary(
          'Higher / Lower — round over',
          dash,
          d,
          { previous: Number(d.previous ?? 0), next: Number(d.next ?? 0), streak: Number(d.streak ?? 0) },
          detailLines,
        ))
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Guess failed')
    }
  }

  const quit = async (active: boolean) => {
    if (!active) return false
    try {
      const data = await apiJson('/api/minigames/higher-lower/quit', { method: 'POST' })
      const d = (data ?? {}) as Record<string, unknown>
      const dash = await refresh()
      if (Boolean(d.noActiveSession) || !dash) {
        setCurrentNumber(null)
        setStreak(0)
        setEndSummary(null)
        return true
      }
      const detailLines = d.coinsBaseBeforeMultiplier != null && d.coinsBaseBeforeMultiplier !== d.coinsReward
        ? [`Base coins: ${d.coinsBaseBeforeMultiplier} × ${d.coinMultiplierApplied}`]
        : []
      const snap = currentNumber != null ? { previous: currentNumber, next: currentNumber, streak } : undefined
      setEndSummary(buildSummary('Higher / Lower — finished', dash, d, snap, detailLines))
      setCurrentNumber(null)
      setStreak(0)
      return true
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'Could not quit minigame')
      setEndSummary(null)
      return true
    }
  }

  const reset = () => {
    setCurrentNumber(null)
    setStreak(0)
  }

  return { currentNumber, streak, start, guess, quit, reset }
}

