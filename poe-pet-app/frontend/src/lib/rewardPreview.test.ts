import { describe, expect, it } from 'vitest'
import { mergeRewardPreview, parseRewardPreviewBlob, type Dashboard } from './dashboard'

describe('dashboard reward preview', () => {
  it('parses camelCase dashboard blob', () => {
    const rp = parseRewardPreviewBlob({
      coinMultiplier: 1.2,
      energyCosts: { puzzle_swap: 4 },
      higherLower: { hasActiveSession: false, streak: 2, coinsIfFinishNow: 3, fibonacciCap: 48 },
      puzzle_swap: { coinsBySampleScore: { score_8: 10 } },
      connect4_ai: { coinsByOutcome: { win: 9, draw: 4, loss: 1 } },
    })
    expect(rp?.coinMultiplier).toBe(1.2)
    expect(rp?.energyCosts?.puzzle_swap).toBe(4)
    expect(rp?.higherLower.streak).toBe(2)
    expect(rp?.puzzle_swap.coinsBySampleScore.score_8).toBe(10)
    expect(rp?.connect4_ai.coinsByOutcome.win).toBe(9)
  })

  it('mergeRewardPreview reads nested dashboard.rewardPreview', () => {
    const d = {
      pet: { hunger: 50, happiness: 50, energy: 50 },
      wallet: { coins: 0 },
      rewardPreview: { coinMultiplier: 1, higherLower: { streak: 0, coinsIfFinishNow: 0, fibonacciCap: 48, hasActiveSession: false } } as unknown,
    }
    const m = mergeRewardPreview(d as unknown as Dashboard)
    expect(m?.higherLower.streak).toBe(0)
  })
})
