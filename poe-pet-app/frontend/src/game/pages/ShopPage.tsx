import type { GameShopItem } from '../../lib/gameTypes'
import type { Dashboard } from '../../lib/dashboard'
import type { PetVisualAsset } from '../../lib/petVisuals'
import { useMemo, useState } from 'react'

function visualCodeFromCosmetic(item: GameShopItem): string | undefined {
  return item.effects?.find((e) => e.kind === 'GRANT_VISUAL')?.visualAssetCode
}

function speciesCodeFromItem(item: GameShopItem): string | undefined {
  return item.effects?.find((e) => e.kind === 'GRANT_SPECIES')?.speciesCode
}

function catalogImageForVisual(catalog: PetVisualAsset[], code: string | undefined): string | null {
  if (!code) return null
  const row = catalog.find((a) => a.code === code && a.active !== false)
  return row?.imagePath ? row.imagePath : null
}

/** Prefer a happy starter mood image for shop preview. */
function catalogImageForSpecies(catalog: PetVisualAsset[], species: string | undefined): string | null {
  if (!species) return null
  const happy = catalog.find(
    (a) =>
      a.assetType === 'PET_MOOD'
      && (a.speciesCode === species || a.speciesCode === String(species).replace('_', ''))
      && (a.moodCode === 'happy' || a.moodCode === ''),
  )
  if (happy?.imagePath) return happy.imagePath
  const anyMood = catalog.find((a) => a.assetType === 'PET_MOOD' && a.speciesCode === species)
  return anyMood?.imagePath ?? null
}

export function ShopPage({
  consumableShopItems,
  cosmeticShopItems,
  speciesShopItems,
  visualCatalog,
  dashboard,
  onBuy,
}: {
  consumableShopItems: GameShopItem[]
  cosmeticShopItems: GameShopItem[]
  speciesShopItems: GameShopItem[]
  visualCatalog: PetVisualAsset[]
  dashboard: Dashboard
  onBuy: (itemCode: string) => void
}) {
  const ownedSpecies = new Set(dashboard.pet.ownedSpeciesCodes ?? ['dog', 'cat'])
  const ownedVisual = new Set(dashboard.pet.ownedVisualAssetCodes ?? [])
  const [tab, setTab] = useState<'CONSUMABLES' | 'COSMETICS' | 'PETS'>(() => 'CONSUMABLES')
  const totals = useMemo(() => ({
    consumables: consumableShopItems.length,
    cosmetics: cosmeticShopItems.length,
    pets: speciesShopItems.length,
  }), [consumableShopItems.length, cosmeticShopItems.length, speciesShopItems.length])

  return (
    <div className="card pane">
      <h3 style={{ marginTop: 0 }}>Shop</h3>
      <div className="subnav" style={{ marginTop: 6 }}>
        <button type="button" className={tab === 'CONSUMABLES' ? 'tab active' : 'tab'} onClick={() => setTab('CONSUMABLES')}>
          Consumables <span className="muted">({totals.consumables})</span>
        </button>
        <button type="button" className={tab === 'COSMETICS' ? 'tab active' : 'tab'} onClick={() => setTab('COSMETICS')}>
          Cosmetics <span className="muted">({totals.cosmetics})</span>
        </button>
        <button type="button" className={tab === 'PETS' ? 'tab active' : 'tab'} onClick={() => setTab('PETS')}>
          Pets <span className="muted">({totals.pets})</span>
        </button>
      </div>

      {tab === 'CONSUMABLES' ? (
        <>
          <h3 data-testid="shop-heading-consumables">Consumables</h3>
          <p className="muted">Food and boosts. Purchased items go to Inventory.</p>
          {consumableShopItems.length === 0 ? <p className="muted">No consumables in catalog.</p> : consumableShopItems.map((item) => (
            <div key={item.code} className="row shop-row" data-testid={`shop-item-${item.code}`}>
              <div className="shop-row-main">
                <div className="shop-row-text">
                  <b>{item.name}</b> <span className="muted">({item.priceCoins} coins)</span>
                  <br />
                  <span className="muted">{item.description}</span>
                </div>
              </div>
              <div className="shop-row-actions">
                <button type="button" onClick={() => onBuy(item.code)}>Buy</button>
              </div>
            </div>
          ))}
        </>
      ) : null}

      {tab === 'PETS' ? (
        <>
          <h3 data-testid="shop-heading-pets">Pets</h3>
          <p className="muted">
            <strong>Starter pets</strong> (dog and cat) are free. <strong>Extra species</strong> are one-time unlocks; after purchase, pick them in <strong>Customize</strong>.
          </p>
          {speciesShopItems.length === 0 ? <p className="muted">No pets in catalog.</p> : speciesShopItems.map((item) => {
            const speciesCode = speciesCodeFromItem(item)
            const owned = speciesCode ? ownedSpecies.has(speciesCode) : false
            const thumb = catalogImageForSpecies(visualCatalog, speciesCode)
            return (
              <div key={item.code} className="row shop-row" data-testid={`shop-item-${item.code}`}>
                <div className="shop-row-main">
                  {thumb ? <img src={thumb} alt="" className="shop-thumb" /> : <div className="shop-thumb shop-thumb-placeholder" aria-hidden />}
                  <div className="shop-row-text">
                    <b>{item.name}</b> <span className="muted">({item.priceCoins} coins)</span>
                    {owned ? <span className="shop-owned-badge">Owned</span> : null}
                    <br />
                    <span className="muted">{item.description}</span>
                  </div>
                </div>
                <div className="shop-row-actions">
                  <button type="button" onClick={() => onBuy(item.code)} disabled={owned}>
                    {owned ? 'Owned' : 'Buy'}
                  </button>
                </div>
              </div>
            )
          })}
        </>
      ) : null}

      {tab === 'COSMETICS' ? (
        <>
          <h3 data-testid="shop-heading-cosmetics">Cosmetics</h3>
          <p className="muted">
            <strong>Backgrounds, foregrounds, and extra looks.</strong> One-time purchase; duplicates are blocked on the server.
            Equip in <strong>Customize</strong>.
          </p>
          {cosmeticShopItems.length === 0 ? <p className="muted">No cosmetics in catalog.</p> : cosmeticShopItems.map((item) => {
            const vCode = visualCodeFromCosmetic(item)
            const owned = vCode ? ownedVisual.has(vCode) : false
            const thumb = catalogImageForVisual(visualCatalog, vCode)
            return (
              <div key={item.code} className="row shop-row" data-testid={`shop-item-${item.code}`}>
                <div className="shop-row-main">
                  {thumb ? <img src={thumb} alt="" className="shop-thumb" /> : <div className="shop-thumb shop-thumb-placeholder" aria-hidden />}
                  <div className="shop-row-text">
                    <b>{item.name}</b> <span className="muted">({item.priceCoins} coins)</span>
                    {owned ? <span className="shop-owned-badge">Owned</span> : null}
                    <br />
                    <span className="muted">{item.description}</span>
                  </div>
                </div>
                <div className="shop-row-actions">
                  <button type="button" onClick={() => onBuy(item.code)} disabled={owned}>
                    {owned ? 'Owned' : 'Buy'}
                  </button>
                </div>
              </div>
            )
          })}
        </>
      ) : null}
    </div>
  )
}
