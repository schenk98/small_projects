import type { GameShopItem } from '../../lib/gameTypes'

export function ShopPage({
  consumableShopItems,
  cosmeticShopItems,
  onBuy,
}: {
  consumableShopItems: GameShopItem[]
  cosmeticShopItems: GameShopItem[]
  onBuy: (itemCode: string) => void
}) {
  return (
    <div className="card pane">
      <h3>Consumables</h3>
      {consumableShopItems.map((item) => (
        <div key={item.code} className="row shop-row">
          <div><b>{item.name}</b> ({item.priceCoins})<br />{item.description}</div>
          <div className="shop-row-actions">
            <button type="button" onClick={() => onBuy(item.code)}>Buy</button>
          </div>
        </div>
      ))}
      <h3 style={{ marginTop: '1.25rem' }}>Cosmetics</h3>
      <p className="muted">Backgrounds, foreground overlays, and future alternate pet looks. One-time purchase; equip in Customize.</p>
      {cosmeticShopItems.length === 0 ? <p className="muted">No cosmetics in catalog.</p> : cosmeticShopItems.map((item) => (
        <div key={item.code} className="row shop-row">
          <div><b>{item.name}</b> ({item.priceCoins})<br />{item.description}</div>
          <div className="shop-row-actions">
            <button type="button" onClick={() => onBuy(item.code)}>Buy</button>
          </div>
        </div>
      ))}
    </div>
  )
}

