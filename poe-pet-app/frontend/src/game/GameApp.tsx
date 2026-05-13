import { useEffect, useState } from 'react'
import { Route, Routes, useNavigate } from 'react-router-dom'
import type { Tokens } from '../auth/AuthScreens'
import { API_BASE_URL } from '../config'
import { isSessionExpiredMessage } from '../lib/session'
import type { SavedMoodCode, SpeciesCode } from '../lib/petVisuals'
import { Topbar } from './components/Topbar'
import { PetStage } from './components/PetStage'
import { NavTabs } from './components/NavTabs'
import { DevStatsModal } from './components/DevStatsModal'
import { useGameData } from './useGameData'
import { ShopPage } from './pages/ShopPage'
import { InventoryPage } from './pages/InventoryPage'
import { CustomizePage } from './pages/CustomizePage'
import { MinigamesPage } from './pages/MinigamesPage'
import { ProgressPage } from './pages/ProgressPage'
import { SettingsPage } from './pages/SettingsPage'
import { usePetAiChat } from './usePetAiChat'

const API = API_BASE_URL
const DASHBOARD_POLL_MS = 10 * 60 * 1000

export function GameApp({
  authHeaders,
  setTokens,
  locationPath,
  setMessage,
}: {
  authHeaders: Record<string, string>
  setTokens: (t: Tokens | null) => void
  locationPath: string
  setMessage: (m: string) => void
}) {
  const navigate = useNavigate()
  const {
    apiJson,
    dashboard,
    visualCatalog,
    shopItems,
    minigames,
    inventory,
    rewardPreview,
    loadError,
    setLoadError,
    refresh,
  } = useGameData(API, authHeaders)

  const [devStatsOpen, setDevStatsOpen] = useState(false)
  const {
    aiAnswer,
    aiFallbackUsed,
    aiPrompt,
    setAiPrompt,
    aiLoading,
    sendAiPrompt,
  } = usePetAiChat(apiJson, dashboard, setMessage)

  /**
   * Refresh wrapper with a consistent auth/session policy:
   * - if tokens are invalid/expired => logout + redirect to login
   * - otherwise surface the error to the UI
   */
  const safeRefresh = async () => {
    try {
      return await refresh()
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load game data'
      if (isSessionExpiredMessage(message)) {
        setTokens(null)
        setLoadError('')
        setMessage('Session expired. Please sign in again.')
        navigate('/login', { replace: true })
        return undefined
      }
      setMessage(message)
      setLoadError(message)
      return undefined
    }
  }

  // Initial load only; `authHeaders` is stable per login from parent `useMemo`.
  useEffect(() => {
    void safeRefresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  /**
   * While the tab is active, refresh occasionally so the UI doesn't drift too far from the server-side simulation.
   * This is intentionally low-frequency: simulation also runs on-demand whenever the user performs an action.
   */
  useEffect(() => {
    if (!dashboard) return

    const tick = () => {
      if (document.visibilityState !== 'visible') return
      void safeRefresh()
    }

    const onVisibility = () => {
      if (document.visibilityState === 'visible') void safeRefresh()
    }

    window.addEventListener('visibilitychange', onVisibility)
    const id = window.setInterval(tick, DASHBOARD_POLL_MS)
    return () => {
      window.removeEventListener('visibilitychange', onVisibility)
      window.clearInterval(id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dashboard])

  if (!dashboard) return <div className="card">{loadError ? `Loading failed: ${loadError}` : 'Loading...'}</div>

  const nav: [string, string][] = [
    ['/app/shop', 'Shop'],
    ['/app/minigames', 'Minigames'],
    ['/app/inventory', 'Inventory'],
    ['/app/customize', 'Customize'],
    ['/app/progress', 'Progress'],
    ['/app/settings', 'Settings'],
  ]

  /** Shop purchase: backend validates coins and one-time purchase rules. */
  const buy = async (itemCode: string) => {
    const raw = await apiJson('/api/shop/purchase', { method: 'POST', body: JSON.stringify({ itemCode }) })
    const data = (raw ?? {}) as Record<string, unknown>
    setMessage((typeof data.error === 'string' && data.error) || 'Purchased')
    await safeRefresh()
  }

  /** Inventory usage: some items may require explicit overwrite confirmation. */
  const consumeInventoryItem = async (itemCode: string) => {
    let raw = await apiJson('/api/inventory/use', { method: 'POST', body: JSON.stringify({ itemCode }) })
    let data = (raw ?? {}) as Record<string, unknown>
    if (data.needsConfirmation === true) {
      raw = await apiJson('/api/inventory/use', { method: 'POST', body: JSON.stringify({ itemCode, confirmOverwrite: true }) })
      data = (raw ?? {}) as Record<string, unknown>
    }
    const msg =
      (typeof data.error === 'string' && data.error)
      || (typeof data.message === 'string' && data.message)
      || 'Used'
    setMessage(msg)
    await safeRefresh()
  }

  /** Pet cosmetics: species determines which mood assets are eligible. */
  const setSpecies = async (next: SpeciesCode) => {
    await apiJson('/api/pet-visuals/species', {
      method: 'POST',
      body: JSON.stringify({ speciesCode: next }),
    })
    setMessage(`Species switched to ${next}.`)
    await safeRefresh()
  }

  /**
   * Override a specific mood slot with an owned asset.
   * `none` means "use the default starter asset for that mood/species".
   */
  const setMoodAsset = async (mood: SavedMoodCode, code: string) => {
    const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<SavedMoodCode, string>>
    const next = { ...moodSlots, [mood]: code === 'none' ? undefined : code }
    await apiJson('/api/pet-visuals/mood-assets', {
      method: 'POST',
      body: JSON.stringify({ moodAssetCodes: next }),
    })
    setMessage('Updated mood image.')
    await safeRefresh()
  }

  /**
   * Equip scene layers around the center pet.
   * Backend enforces starter/ownership; `"none"` clears a layer.
   */
  const equipVisualLayers = async (backgroundAssetCode: string, foregroundAssetCode: string) => {
    await apiJson('/api/pet-visuals/equip-layers', {
      method: 'POST',
      body: JSON.stringify({
        backgroundAssetCode: backgroundAssetCode === 'none' || !backgroundAssetCode ? 'none' : backgroundAssetCode,
        foregroundAssetCode: foregroundAssetCode === 'none' || !foregroundAssetCode ? 'none' : foregroundAssetCode,
      }),
    })
    setMessage('Updated scene layers.')
    await safeRefresh()
  }

  const devGrantCoins = async () => {
    try {
      await apiJson('/api/dev/grant-coins', { method: 'POST', body: JSON.stringify({ amount: 1000 }) })
      await safeRefresh()
      setMessage('Developer: coins granted.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Dev action failed')
    }
  }

  const devRefillStats = async () => {
    try {
      await apiJson('/api/dev/refill-stats', { method: 'POST' })
      await safeRefresh()
      setMessage('Developer: stats refilled.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Dev action failed')
    }
  }

  const devApplyStats = async (hungerPercent: number, happinessPercent: number, energyPercent: number) => {
    await apiJson('/api/dev/set-stats', {
      method: 'POST',
      body: JSON.stringify({ hungerPercent, happinessPercent, energyPercent }),
    })
    setDevStatsOpen(false)
    await safeRefresh()
    setMessage('Developer: stats updated.')
  }

  /** Update pet name (saved on backend; used for AI prefix). */
  const setPetName = async (name: string) => {
    await apiJson('/api/pet/name', {
      method: 'POST',
      body: JSON.stringify({ name }),
    })
    await safeRefresh()
  }

  const consumableShopItems = shopItems.filter((item) => item.type === 'CONSUMABLE' || (item.shopSection || '') === 'CONSUMABLES')
  const cosmeticShopItems = shopItems.filter((item) => item.type === 'COSMETIC' || (item.shopSection || '') === 'COSMETICS')
  const speciesShopItems = shopItems.filter((item) => item.type === 'SPECIES' || (item.shopSection || '') === 'SPECIES')

  return (
    <div className="game">
      <Topbar
        dashboard={dashboard}
        onDevGrantCoins={() => void devGrantCoins()}
        onDevRefillStats={() => void devRefillStats()}
        onOpenDevStats={() => setDevStatsOpen(true)}
      />

      <div className="card" style={{ width: '80%', margin: '12px auto 8px auto' }}>
        <div style={{ fontSize: 14, opacity: 0.7 }}>
          {dashboard.pet.name || 'Pet'}
          {aiLoading ? ' is thinking' : ' says'}
          {aiLoading ? <span className="thinking-dots" aria-hidden="true">...</span> : null}
        </div>
        <div style={{
          fontSize: 18,
          fontFamily: aiFallbackUsed ? 'ui-sans-serif, system-ui, sans-serif' : 'ui-serif, Georgia, serif',
          fontStyle: aiFallbackUsed ? 'italic' : 'normal',
          opacity: aiFallbackUsed ? 0.8 : 1,
          lineHeight: 1.35,
          whiteSpace: 'pre-wrap',
        }}
        >
          {aiLoading ? (aiAnswer || '…') : (aiAnswer || '—')}
        </div>
      </div>

      <PetStage dashboard={dashboard} visualCatalog={visualCatalog} transientMood={aiLoading ? 'thinking' : undefined} />

      <div className="card" style={{ width: '80%', margin: '8px auto 12px auto' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            style={{ flex: 1 }}
            value={aiPrompt}
            onChange={(e) => setAiPrompt(e.target.value)}
            placeholder={`Message ${dashboard.pet.name || 'your pet'}…`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                void sendAiPrompt()
              }
            }}
          />
          <button type="button" onClick={() => void sendAiPrompt()} disabled={aiLoading}>
            {aiLoading ? 'Thinking…' : 'Send'}
          </button>
        </div>
      </div>

      <NavTabs locationPath={locationPath} nav={nav} />

      <Routes>
        <Route path="shop" element={<ShopPage consumableShopItems={consumableShopItems} cosmeticShopItems={cosmeticShopItems} speciesShopItems={speciesShopItems} visualCatalog={visualCatalog} dashboard={dashboard} onBuy={(c) => void buy(c)} />} />
        <Route path="inventory" element={<InventoryPage inventory={inventory} shopItems={shopItems} onUseItem={(c) => void consumeInventoryItem(c)} />} />
        <Route path="customize" element={<CustomizePage dashboard={dashboard} visualCatalog={visualCatalog} onSetName={(n) => void setPetName(n)} onSetSpecies={(s) => void setSpecies(s)} onSetMoodAsset={(m, c) => void setMoodAsset(m, c)} onEquipVisualLayers={(bg, fg) => void equipVisualLayers(bg, fg)} />} />
        <Route path="minigames" element={<MinigamesPage apiJson={apiJson} dashboard={dashboard} refresh={safeRefresh} minigames={minigames} rewardPreview={rewardPreview} setMessage={setMessage} />} />
        <Route path="progress" element={<ProgressPage apiJson={apiJson} dashboard={dashboard} />} />
        <Route path="settings" element={<SettingsPage setTokens={setTokens} dashboard={dashboard} apiJson={apiJson} setMessage={setMessage} />} />
      </Routes>

      <DevStatsModal
        open={devStatsOpen}
        onClose={() => setDevStatsOpen(false)}
        onApply={devApplyStats}
      />
    </div>
  )
}

