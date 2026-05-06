import type { GameInventoryItem, GameShopItem } from '../../lib/gameTypes'

export function InventoryPage({
  inventory,
  shopItems,
  onUseItem,
}: {
  inventory: GameInventoryItem[]
  shopItems: GameShopItem[]
  onUseItem: (itemCode: string) => void
}) {
  const itemNameMap = Object.fromEntries(shopItems.map((item) => [item.code, item.name]))

  return (
    <div className="card pane">
      {inventory.length === 0
        ? 'No consumables.'
        : inventory.map((i) => (
          <details key={i.itemCode} className="inv-item">
            <summary>{itemNameMap[i.itemCode] || i.itemCode} x{i.quantity}</summary>
            <p>{shopItems.find((s) => s.code === i.itemCode)?.description || 'No description'}</p>
            <button type="button" onClick={() => onUseItem(i.itemCode)}>Use</button>
          </details>
        ))}
    </div>
  )
}

