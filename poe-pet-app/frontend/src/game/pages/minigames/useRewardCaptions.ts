import type { RewardPreview } from '../../../lib/dashboard'
import type { MinigameInfo } from '../../../lib/gameTypes'

export function useRewardCaptions(rewardPreview?: RewardPreview) {
  /** Used to show "payout preview" text on the minigame list page. */
  const rewardCaptionFor = (g: MinigameInfo): string[] => {
    const rp = rewardPreview
    if (!rp) return []
    const m = rp.coinMultiplier
    if (g.code === 'higher_lower') {
      const h = rp.higherLower
      const lines = [
        `Quit or lose with your current streak (${h.streak}): ${h.coinsIfFinishNow} coins (cap ${h.fibonacciCap}; ×${m} multiplier already applied).`,
      ]
      if (h.hasActiveSession) lines.push('You have an active session — payouts reflect this account’s streak.')
      return lines
    }
    if (g.code === 'puzzle_swap') {
      const by = rp.puzzle_swap?.coinsBySampleScore ?? {}
      const parts = Object.entries(by).map(([k, coins]) => `${k.replace('score_', 'score ')} → ${coins} coins`)
      return [`Sample payouts (your ×${m} multiplier):`, ...parts]
    }
    const o = rp.connect4_ai?.coinsByOutcome
    if (g.code === 'connect4_ai' && o) {
      return [`Win: ${o.win} coins • Draw: ${o.draw} • Loss: ${o.loss} (×${m} multiplier applied).`]
    }
    const ms = rp.minesweep_ai?.coinsByOutcome
    if (g.code === 'minesweep_ai' && ms) {
      return [`Win: ${ms.win} coins • Loss: ${ms.loss} (×${m} multiplier applied).`]
    }
    const ck = rp.checkers_ai?.coinsByOutcome
    if (g.code === 'checkers_ai' && ck) {
      return [`Win: ${ck.win} coins • Draw: ${ck.draw} • Loss: ${ck.loss} (×${m} multiplier applied).`]
    }
    return []
  }

  return { rewardCaptionFor }
}

