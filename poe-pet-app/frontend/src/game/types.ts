import type { Tokens } from '../auth/AuthScreens'
import type { Dashboard, RewardPreview } from '../lib/dashboard'
import type { GameInventoryItem, GameShopItem, MinigameInfo } from '../lib/gameTypes'
import type { PetVisualAsset } from '../lib/petVisuals'

export type ApiJson = (path: string, init?: RequestInit) => Promise<unknown>

export type GameData = {
  dashboard: Dashboard
  rewardPreview?: RewardPreview
  visualCatalog: PetVisualAsset[]
  shopItems: GameShopItem[]
  minigames: MinigameInfo[]
  inventory: GameInventoryItem[]
}

export type GameAppProps = {
  authHeaders: Record<string, string>
  setTokens: (t: Tokens | null) => void
  locationPath: string
  setMessage: (m: string) => void
}

