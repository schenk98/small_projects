import { MOOD_LABELS, deriveMood, type MoodCode, type PetVisualAsset, type SpeciesCode } from '../../lib/petVisuals'
import type { Dashboard } from '../../lib/dashboard'

export function PetStage({
  dashboard,
  visualCatalog,
}: {
  dashboard: Dashboard
  visualCatalog: PetVisualAsset[]
}) {
  /**
   * PetStage renders a simple 3-layer scene:
   * 1) background image (optional)
   * 2) the pet mood PNG (required for the stage to look "alive")
   * 3) foreground overlay (optional)
   */
  const speciesCode: SpeciesCode = (dashboard.pet.speciesCode === 'cat' ? 'cat' : 'dog')
  const activeMood: MoodCode = deriveMood(dashboard.pet)

  const moodAssetsForSpecies = visualCatalog.filter((a) => a.assetType === 'PET_MOOD' && a.speciesCode === speciesCode)
  const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<MoodCode, string>>
  const pathForVisualCode = (code: string | null | undefined) =>
    (code && visualCatalog.find((a) => a.code === code)?.imagePath) || ''

  const equippedBg = dashboard.pet.equippedBackgroundAssetCode || ''
  const equippedFg = dashboard.pet.equippedForegroundAssetCode || ''
  const backgroundLayerUrl = pathForVisualCode(equippedBg)
  const foregroundLayerUrl = pathForVisualCode(equippedFg)

  const moodImagePath = (() => {
    // Slot override wins if present; otherwise fallback to the default asset for the derived mood.
    const selectedCode = moodSlots[activeMood]
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
      <h3>{speciesCode === 'cat' ? 'Cat' : 'Dog'} — {MOOD_LABELS[activeMood]}</h3>
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

