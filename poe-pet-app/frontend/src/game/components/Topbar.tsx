import { Link } from 'react-router-dom'
import type { Dashboard } from '../../lib/dashboard'

export function Topbar({
  dashboard,
  onDevGrantCoins,
  onDevRefillStats,
  onOpenDevStats,
}: {
  dashboard: Dashboard
  onDevGrantCoins: () => void
  onDevRefillStats: () => void
  onOpenDevStats: () => void
}) {
  return (
    <div className="topbar">
      <span>Coins: {dashboard.wallet.coins}</span>
      {dashboard.privileged && (
        <details className="dev-dropdown">
          <summary>Developer</summary>
          <div className="dev-dropdown-body">
            <button type="button" onClick={onDevGrantCoins}>+1000 coins</button>
            <button type="button" onClick={onDevRefillStats}>Refill hunger / happiness / energy</button>
            <button type="button" onClick={onOpenDevStats}>Set stats (%)…</button>
            <p><small>Unlocked via your account flag or APP_PRIVILEGED_EMAILS.</small></p>
          </div>
        </details>
      )}
      {!dashboard.privileged && (
        <Link className="dev-hint" to="/app/settings#developer-tools">How to enable Developer</Link>
      )}
      {[
        { label: 'Hunger', value: Math.round(dashboard.pet.hunger), cls: 'bar-hunger' },
        { label: 'Happiness', value: Math.round(dashboard.pet.happiness), cls: 'bar-happiness' },
        { label: 'Energy', value: Math.round(dashboard.pet.energy), cls: 'bar-energy' },
      ].map((s) => (
        <div key={s.label} className="stat">
          <small>{s.label}: {s.value}</small>
          <div className="bar"><div className={`fill ${s.cls}`} style={{ width: `${s.value}%` }} /></div>
        </div>
      ))}
    </div>
  )
}

