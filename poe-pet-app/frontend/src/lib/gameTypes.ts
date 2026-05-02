/**
 * Shared DTO shapes for game API responses (shop, inventory, minigames hub).
 * Keep in sync with backend models / Mongo seed field names.
 */

export type GameShopItem = {
  code: string
  type: string
  name: string
  description: string
  priceCoins: number
  oneTimePurchase: boolean
  shopSection?: 'CONSUMABLES' | 'COSMETICS'
  effects?: { kind: string; value?: string | number; visualAssetCode?: string }[]
  playerVisible?: boolean
}

export type GameInventoryItem = { itemCode: string; quantity: number }

export type MinigameInfo = {
  code: string
  name: string
  description: string
  energyCost: number
  rewardStrategy?: { type?: string }
}
