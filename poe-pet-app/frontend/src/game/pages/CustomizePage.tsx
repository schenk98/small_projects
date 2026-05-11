import { MOOD_LABELS, MOOD_SLOT_ORDER, type MoodCode, type PetVisualAsset, type SpeciesCode } from '../../lib/petVisuals'
import type { Dashboard } from '../../lib/dashboard'

export function CustomizePage({
  dashboard,
  visualCatalog,
  onSetName,
  onSetSpecies,
  onSetMoodAsset,
  onEquipVisualLayers,
}: {
  dashboard: Dashboard
  visualCatalog: PetVisualAsset[]
  onSetName: (name: string) => void
  onSetSpecies: (next: SpeciesCode) => void
  onSetMoodAsset: (mood: MoodCode, code: string) => void
  onEquipVisualLayers: (backgroundAssetCode: string, foregroundAssetCode: string) => void
}) {
  const speciesCode: SpeciesCode = (dashboard.pet.speciesCode === 'cat' ? 'cat' : 'dog')
  const ownedVisuals = new Set(dashboard.pet.ownedVisualAssetCodes ?? [])
  const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<MoodCode, string>>
  const moodAssetsForSpecies = visualCatalog.filter((a) => a.assetType === 'PET_MOOD' && a.speciesCode === speciesCode)

  const equippedBg = dashboard.pet.equippedBackgroundAssetCode || ''
  const equippedFg = dashboard.pet.equippedForegroundAssetCode || ''

  return (
    <div className="card pane">
      <h3>Pet customization</h3>
      <p className="muted">One species on stage at a time. Pick default or owned mood art; set scene background and foreground (starters and purchased items only).</p>

      <label className="dev-stat-label" style={{ marginTop: 8 }}>
        Pet name
        <input
          value={dashboard.pet.name || ''}
          placeholder="Pet"
          onChange={(e) => onSetName(e.target.value)}
        />
      </label>

      <div className="subnav">
        <button type="button" className={speciesCode === 'dog' ? 'tab active' : 'tab'} onClick={() => onSetSpecies('dog')}>Dog</button>
        <button type="button" className={speciesCode === 'cat' ? 'tab active' : 'tab'} onClick={() => onSetSpecies('cat')}>Cat</button>
      </div>

      <h4 style={{ marginTop: '12px' }}>Scene</h4>
      <label className="dev-stat-label">
        Background
        <select
          value={equippedBg || 'none'}
          onChange={(e) => onEquipVisualLayers(e.target.value, equippedFg || 'none')}
        >
          <option value="none">Default (gradient)</option>
          {visualCatalog
            .filter((a) => a.assetType === 'BACKGROUND' && (a.starter || ownedVisuals.has(a.code)))
            .map((a) => (
              <option key={a.code} value={a.code}>{a.label}</option>
            ))}
        </select>
      </label>
      <label className="dev-stat-label">
        Foreground overlay
        <select
          value={equippedFg || 'none'}
          onChange={(e) => onEquipVisualLayers(equippedBg || 'none', e.target.value)}
        >
          <option value="none">None</option>
          {visualCatalog
            .filter((a) => a.assetType === 'FOREGROUND' && (a.starter || ownedVisuals.has(a.code)))
            .map((a) => (
              <option key={a.code} value={a.code}>{a.label}</option>
            ))}
        </select>
      </label>

      <h4 style={{ marginTop: '16px' }}>Mood images ({speciesCode})</h4>
      {MOOD_SLOT_ORDER.map((mood) => {
        const options = moodAssetsForSpecies.filter((a) => a.moodCode === mood && (a.starter || ownedVisuals.has(a.code)))
        const selected = moodSlots[mood] || 'none'
        return (
          <label key={mood} className="dev-stat-label">
            {MOOD_LABELS[mood]} image
            <select value={selected} onChange={(e) => onSetMoodAsset(mood, e.target.value)}>
              <option value="none">Default for this mood</option>
              {options.map((o) => <option key={o.code} value={o.code}>{o.label}</option>)}
            </select>
          </label>
        )
      })}
    </div>
  )
}

