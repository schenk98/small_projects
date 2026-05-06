import { warnNotEnoughEnergy } from '../../../lib/energy'
import type { RewardPreview } from '../../../lib/dashboard'
import type { MinigameInfo } from '../../../lib/gameTypes'

export function useEnergyGate({
  rewardPreview,
  minigames,
}: {
  rewardPreview?: RewardPreview
  minigames: MinigameInfo[]
}) {
  const energyCostFor = (code: string) =>
    rewardPreview?.energyCosts?.[code] ?? minigames.find((x) => x.code === code)?.energyCost ?? 99999

  /** Returns false if browser energy check fails (server still enforces). */
  const assertEnergy = (need: number, have: number) => {
    if (have < need) {
      warnNotEnoughEnergy(need, have, 'This minigame')
      return false
    }
    return true
  }

  return { energyCostFor, assertEnergy }
}

