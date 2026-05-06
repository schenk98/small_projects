import { useMemo, useState } from 'react'
import { createJsonApiClient } from '../lib/gameApi'
import type { GameInventoryItem, GameShopItem, MinigameInfo } from '../lib/gameTypes'
import { mergeRewardPreview, parseRewardPreviewBlob, type Dashboard } from '../lib/dashboard'
import type { PetVisualAsset } from '../lib/petVisuals'

export function useGameData(apiBaseUrl: string, authHeaders: Record<string, string>) {
  /**
   * Preconfigured API fetch helper.
   *
   * Stable as long as `apiBaseUrl` + `authHeaders` are stable; `GameApp` controls those.
   */
  const apiJson = useMemo(() => createJsonApiClient(apiBaseUrl, authHeaders), [apiBaseUrl, authHeaders])

  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [visualCatalog, setVisualCatalog] = useState<PetVisualAsset[]>([])
  const [shopItems, setShopItems] = useState<GameShopItem[]>([])
  const [minigames, setMinigames] = useState<MinigameInfo[]>([])
  const [inventory, setInventory] = useState<GameInventoryItem[]>([])
  const [loadError, setLoadError] = useState<string>('')

  const rewardPreview = useMemo(() => (dashboard ? mergeRewardPreview(dashboard) : undefined), [dashboard])

  /**
   * Load all core game data in parallel and update React state.
   *
   * Back-compat: some endpoints are optional (older backend), so we treat them as best-effort.
   * Auth/session handling is done in `GameApp.safeRefresh()` so this hook stays reusable.
   */
  const refresh = async (): Promise<Dashboard | undefined> => {
    try {
      setLoadError('')
      const [d, s, mg, i] = await Promise.all([
        apiJson('/api/dashboard'),
        apiJson('/api/shop/items'),
        apiJson('/api/minigames'),
        apiJson('/api/inventory'),
      ])
      let catalog: PetVisualAsset[] = []
      try {
        const raw = await apiJson('/api/pet-visuals/catalog')
        catalog = Array.isArray(raw) ? raw : []
      } catch {
        /* Older backend without pet visuals — app still loads; stage falls back to defaults. */
      }

      let dash = d as Dashboard
      if (!mergeRewardPreview(dash)) {
        try {
          const blob = await apiJson('/api/minigames/reward-preview')
          const parsed = parseRewardPreviewBlob(blob)
          if (parsed) dash = { ...dash, rewardPreview: parsed }
        } catch {
          /* Older backend without this route — payout preview stays empty until API is updated. */
        }
      }

      setDashboard(dash)
      setVisualCatalog(catalog)
      setShopItems(s as GameShopItem[])
      setMinigames(mg as MinigameInfo[])
      setInventory(i as GameInventoryItem[])
      return dash
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load game data'
      setLoadError(message)
      throw new Error(message, { cause: err })
    }
  }

  return {
    apiJson,
    dashboard,
    setDashboard,
    visualCatalog,
    shopItems,
    minigames,
    inventory,
    rewardPreview,
    loadError,
    setLoadError,
    refresh,
  }
}

