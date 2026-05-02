/**
 * Pet mood derivation, catalog types, and display labels.
 * Server uses the same mood strings in `pets.moodAssetCodes` and `pet_visual_assets.moodCode`.
 */
import type { Dashboard } from './rewardPreview'

export type SpeciesCode = 'dog' | 'cat'

export type MoodCode = 'happy' | 'sad' | 'hungry' | 'tired' | 'playing_dead'

export type VisualAssetType = 'PET_MOOD' | 'BACKGROUND' | 'FOREGROUND'

export type PetVisualAsset = {
  code: string
  assetType: VisualAssetType
  speciesCode: SpeciesCode | 'all' | string
  moodCode: MoodCode | string
  label: string
  imagePath: string
  starter: boolean
  active: boolean
}

export const MOOD_LABELS: Record<MoodCode, string> = {
  happy: 'Happy',
  sad: 'Sad',
  hungry: 'Hungry',
  tired: 'Tired',
  playing_dead: 'Playing dead',
}

/** All mood slot keys sent to `POST /api/pet-visuals/mood-assets`. */
export const MOOD_SLOT_ORDER: MoodCode[] = ['happy', 'sad', 'hungry', 'tired', 'playing_dead']

export function deriveMood(pet: Dashboard['pet']): MoodCode {
  if (pet.hunger <= 0) return 'playing_dead'
  if (pet.hunger < 20) return 'hungry'
  if (pet.energy < 20) return 'tired'
  if (pet.happiness < 30) return 'sad'
  return 'happy'
}
