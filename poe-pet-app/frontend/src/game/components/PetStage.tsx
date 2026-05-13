import { MOOD_LABELS, SPECIES_LABELS, deriveMood, normalizeSpeciesCode, type MoodCode, type PetVisualAsset, type SavedMoodCode, type SpeciesCode } from '../../lib/petVisuals'
import type { Dashboard } from '../../lib/dashboard'

export function PetStage({
  dashboard,
  visualCatalog,
  transientMood,
}: {
  dashboard: Dashboard
  visualCatalog: PetVisualAsset[]
  transientMood?: MoodCode
}) {
  /**
   * PetStage renders a simple 3-layer scene:
   * 1) background image (optional)
   * 2) the pet mood PNG (required for the stage to look "alive")
   * 3) foreground overlay (optional)
   */
  const speciesCode: SpeciesCode = normalizeSpeciesCode(dashboard.pet.speciesCode)
  const baseMood: SavedMoodCode = deriveMood(dashboard.pet)
  const activeMood: MoodCode = transientMood ?? baseMood

  const moodAssetsForSpecies = visualCatalog.filter((a) => a.assetType === 'PET_MOOD' && a.speciesCode === speciesCode)
  const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<SavedMoodCode, string>>
  const pathForVisualCode = (code: string | null | undefined) =>
    (code && visualCatalog.find((a) => a.code === code)?.imagePath) || ''

  const equippedBg = dashboard.pet.equippedBackgroundAssetCode || ''
  const equippedFg = dashboard.pet.equippedForegroundAssetCode || ''
  const backgroundLayerUrl = pathForVisualCode(equippedBg)
  const foregroundLayerUrl = pathForVisualCode(equippedFg)

  const moodImagePath = (() => {
    // Slot override wins for saved gameplay moods. Transient moods like "thinking"
    // intentionally use the default starter visual so chat can animate without
    // mutating the persisted pet customization map.
    const selectedCode = activeMood === baseMood ? moodSlots[activeMood] : undefined
    if (selectedCode) {
      const sel = moodAssetsForSpecies.find((a) => a.code === selectedCode)
      if (sel) return sel.imagePath
    }
    return moodAssetsForSpecies.find((a) => a.moodCode === activeMood)?.imagePath
      || moodAssetsForSpecies.find((a) => a.moodCode === 'happy')?.imagePath
      || ''
  })()

  return (
    <div className="pet-stage card">
      <h3>{SPECIES_LABELS[speciesCode]} — {MOOD_LABELS[activeMood]}</h3>
      {moodImagePath ? (
        <div
          className="pet-stage-layers"
          style={backgroundLayerUrl ? { backgroundImage: `url(${backgroundLayerUrl})` } : undefined}
        >
          <img src={moodImagePath} alt={`${speciesCode} ${activeMood}`} className="pet-stage-image" />
          {foregroundLayerUrl ? (
            <img src={foregroundLayerUrl} alt="" className="pet-stage-fg" aria-hidden />
          ) : null}
        </div>
      ) : (
        <p className="muted">No image found for current mood/species.</p>
      )}
    </div>
  )
}

