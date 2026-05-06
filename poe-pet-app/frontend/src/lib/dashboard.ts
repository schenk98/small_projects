/**
 * Dashboard DTO + reward preview parsing.
 *
 * This is not "one minigame": the dashboard includes a cross-minigame payout/energy preview blob
 * used by the Minigames hub UI.
 */

export type RewardPreview = {
  coinMultiplier: number
  energyCosts?: Record<string, number>
  higherLower: { hasActiveSession: boolean; streak: number; coinsIfFinishNow: number; fibonacciCap: number }
  puzzle_swap: { coinsBySampleScore: Record<string, number> }
  connect4_ai: { coinsByOutcome: Record<string, number> }
  minesweep_ai?: { coinsByOutcome: Record<string, number> }
  checkers_ai?: { coinsByOutcome: Record<string, number> }
}

export type Dashboard = {
  pet: {
    hunger: number
    happiness: number
    energy: number
    speciesCode?: 'dog' | 'cat'
    moodAssetCodes?: Partial<Record<'happy' | 'sad' | 'hungry' | 'tired' | 'playing_dead', string>>
    ownedVisualAssetCodes?: string[]
    equippedBackgroundAssetCode?: string | null
    equippedForegroundAssetCode?: string | null
  }
  wallet: { coins: number }
  privileged?: boolean
  rewardPreview?: RewardPreview
}

/** Parse reward preview object from dashboard or from `GET /api/minigames/reward-preview`. */
export function parseRewardPreviewBlob(raw: unknown): RewardPreview | undefined {
  if (!raw || typeof raw !== 'object') return undefined
  const o = raw as Record<string, unknown>
  const hl = (o.higherLower ?? o.higher_lower) as Record<string, unknown> | undefined
  const ps = (o.puzzle_swap ?? o.puzzleSwap) as Record<string, unknown> | undefined
  const c4 = (o.connect4_ai ?? o.connect4Ai) as Record<string, unknown> | undefined
  const ms = (o.minesweep_ai ?? o.minesweepAi) as Record<string, unknown> | undefined
  const ck = (o.checkers_ai ?? o.checkersAi) as Record<string, unknown> | undefined
  const ec = (o.energyCosts ?? o.energy_costs) as Record<string, number> | undefined
  const coinsBy = (ps?.coinsBySampleScore ?? ps?.coins_by_sample_score) as Record<string, number> | undefined
  const outcomes = (c4?.coinsByOutcome ?? c4?.coins_by_outcome) as Record<string, number> | undefined
  const msOut = (ms?.coinsByOutcome ?? ms?.coins_by_outcome) as Record<string, number> | undefined
  const ckOut = (ck?.coinsByOutcome ?? ck?.coins_by_outcome) as Record<string, number> | undefined
  const higherLower = hl
    ? {
        hasActiveSession: Boolean(hl.hasActiveSession ?? hl.has_active_session),
        streak: Number(hl.streak ?? 0),
        coinsIfFinishNow: Number(hl.coinsIfFinishNow ?? hl.coins_if_finish_now ?? 0),
        fibonacciCap: Number(hl.fibonacciCap ?? hl.fibonacci_cap ?? 48),
      }
    : { hasActiveSession: false, streak: 0, coinsIfFinishNow: 0, fibonacciCap: 48 }
  return {
    coinMultiplier: Number(o.coinMultiplier ?? o.coin_multiplier ?? 1),
    energyCosts: ec,
    higherLower,
    puzzle_swap: { coinsBySampleScore: coinsBy && typeof coinsBy === 'object' ? coinsBy : {} },
    connect4_ai: { coinsByOutcome: outcomes && typeof outcomes === 'object' ? outcomes : { win: 0, draw: 0, loss: 0 } },
    minesweep_ai: msOut && typeof msOut === 'object' ? { coinsByOutcome: msOut } : undefined,
    checkers_ai: ckOut && typeof ckOut === 'object' ? { coinsByOutcome: ckOut } : undefined,
  }
}

/** Normalize dashboard JSON so payout + energy lines always render. */
export function mergeRewardPreview(d: Dashboard): RewardPreview | undefined {
  const raw = (d as unknown as { rewardPreview?: unknown; reward_preview?: unknown }).rewardPreview
    ?? (d as unknown as { reward_preview?: unknown }).reward_preview
  return parseRewardPreviewBlob(raw)
}

