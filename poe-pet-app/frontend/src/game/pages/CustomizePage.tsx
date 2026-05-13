import { MOOD_LABELS, MOOD_SLOT_ORDER, SPECIES_LABELS, SPECIES_ORDER, normalizeSpeciesCode, type PetVisualAsset, type SavedMoodCode, type SpeciesCode } from '../../lib/petVisuals'
import type { Dashboard } from '../../lib/dashboard'
import { useState } from 'react'

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
  onSetMoodAsset: (mood: SavedMoodCode, code: string) => void
  onEquipVisualLayers: (backgroundAssetCode: string, foregroundAssetCode: string) => void
}) {
  const speciesCode: SpeciesCode = normalizeSpeciesCode(dashboard.pet.speciesCode)
  const ownedVisuals = new Set(dashboard.pet.ownedVisualAssetCodes ?? [])
  const ownedSpecies = new Set(dashboard.pet.ownedSpeciesCodes ?? ['dog', 'cat'])
  const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<SavedMoodCode, string>>
  const moodAssetsForSpecies = visualCatalog.filter((a) => a.assetType === 'PET_MOOD' && a.speciesCode === speciesCode)

  const equippedBg = dashboard.pet.equippedBackgroundAssetCode || ''
  const equippedFg = dashboard.pet.equippedForegroundAssetCode || ''
  const ownedSpeciesOrdered = SPECIES_ORDER.filter((s) => ownedSpecies.has(s))

  return (
    <div className="card pane">
      <h3>Pet customization</h3>
      <p className="muted">Dog and Cat are starters. Unlock extra pets in the Shop, then pick default or owned mood art and scene layers here.</p>

      <PetNameEditor key={dashboard.pet.name ?? ''} name={dashboard.pet.name || ''} onSetName={onSetName} />

      <label className="dev-stat-label" style={{ marginTop: 6 }}>
        Pet species
        <select
          value={speciesCode}
          onChange={(e) => onSetSpecies(normalizeSpeciesCode(e.target.value))}
        >
          {ownedSpeciesOrdered.map((s) => (
            <option key={s} value={s}>{SPECIES_LABELS[s]}</option>
          ))}
        </select>
      </label>

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

function PetNameEditor({ name, onSetName }: { name: string; onSetName: (n: string) => void }) {
  const [draft, setDraft] = useState(name)
  return (
    <label className="dev-stat-label" style={{ marginTop: 8 }}>
      Pet name
      <input
        value={draft}
        placeholder="Pet"
        onChange={(e) => setDraft(e.target.value)}
        onBlur={() => {
          const next = (draft || '').trim()
          const current = (name || '').trim()
          if (next !== current) onSetName(next)
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            ;(e.target as HTMLInputElement).blur()
          }
        }}
      />
    </label>
  )
}

