/**
 * Pet mood derivation, catalog types, and display labels.
 * Server uses the same mood strings in `pets.moodAssetCodes` and `pet_visual_assets.moodCode`.
 */
import type { Dashboard } from './dashboard'

export type SpeciesCode =
  | 'dog'
  | 'cat'
  | 'penguin'
  | 'fox'
  | 'hamster'
  | 'tiger'
  | 'lion'
  | 'horse'
  | 'parrot'
  | 'unicorn'
  | 'midnight_cat'
  | 'panda'
  | 'goldfish'
  | 'lizard'

export type SavedMoodCode = 'happy' | 'sad' | 'hungry' | 'tired' | 'playing_dead'
export type MoodCode = SavedMoodCode | 'thinking'

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
  thinking: 'Thinking',
}

/** All mood slot keys sent to `POST /api/pet-visuals/mood-assets`. */
export const MOOD_SLOT_ORDER: SavedMoodCode[] = ['happy', 'sad', 'hungry', 'tired', 'playing_dead']

export const SPECIES_LABELS: Record<SpeciesCode, string> = {
  dog: 'Dog',
  cat: 'Cat',
  penguin: 'Penguin',
  fox: 'Fox',
  hamster: 'Hamster',
  tiger: 'Tiger',
  lion: 'Lion',
  horse: 'Horse',
  parrot: 'Parrot',
  unicorn: 'Unicorn',
  midnight_cat: 'Midnight Cat',
  panda: 'Panda',
  goldfish: 'Goldfish',
  lizard: 'Lizard',
}

export const SPECIES_ORDER: SpeciesCode[] = [
  'dog',
  'cat',
  'penguin',
  'fox',
  'hamster',
  'tiger',
  'lion',
  'horse',
  'parrot',
  'unicorn',
  'midnight_cat',
  'panda',
  'goldfish',
  'lizard',
]

export function normalizeSpeciesCode(value: string | undefined): SpeciesCode {
  if (
    value === 'cat'
    || value === 'penguin'
    || value === 'fox'
    || value === 'hamster'
    || value === 'tiger'
    || value === 'lion'
    || value === 'horse'
    || value === 'parrot'
    || value === 'unicorn'
    || value === 'midnight_cat'
    || value === 'panda'
    || value === 'goldfish'
    || value === 'lizard'
  ) return value
  return 'dog'
}

export function deriveMood(pet: Dashboard['pet']): SavedMoodCode {
  if (pet.hunger <= 0) return 'playing_dead'
  if (pet.hunger < 20) return 'hungry'
  if (pet.energy < 20) return 'tired'
  if (pet.happiness < 30) return 'sad'
  return 'happy'
}
