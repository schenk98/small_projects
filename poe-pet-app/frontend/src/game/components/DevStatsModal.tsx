import { useState } from 'react'

export function DevStatsModal({
  open,
  onClose,
  onApply,
}: {
  open: boolean
  onClose: () => void
  onApply: (hungerPercent: number, happinessPercent: number, energyPercent: number) => Promise<void> | void
}) {
  const [devH, setDevH] = useState('100')
  const [devHa, setDevHa] = useState('100')
  const [devE, setDevE] = useState('100')

  if (!open) return null

  const parsePct = (s: string) => {
    const n = Number(String(s).replace(',', '.').trim())
    if (!Number.isFinite(n) || n < 0 || n > 100) throw new Error('Each stat must be 0–100')
    return n / 100
  }

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div className="modal card dev-stats-panel" onClick={(e) => e.stopPropagation()}>
        <h3>Set pet stats (%)</h3>
        <label className="dev-stat-label">Hunger <input value={devH} onChange={(e) => setDevH(e.target.value)} /></label>
        <label className="dev-stat-label">Happiness <input value={devHa} onChange={(e) => setDevHa(e.target.value)} /></label>
        <label className="dev-stat-label">Energy <input value={devE} onChange={(e) => setDevE(e.target.value)} /></label>
        <div className="result-actions">
          <button
            type="button"
            onClick={async () => {
              try {
                await onApply(parsePct(devH), parsePct(devHa), parsePct(devE))
              } catch (e) {
                window.alert(e instanceof Error ? e.message : 'Invalid percentages')
              }
            }}
          >
            Apply
          </button>
          <button type="button" className="danger" onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

