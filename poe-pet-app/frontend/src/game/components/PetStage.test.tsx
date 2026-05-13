// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { PetStage } from './PetStage'
import type { Dashboard } from '../../lib/dashboard'
import type { PetVisualAsset, SpeciesCode } from '../../lib/petVisuals'

afterEach(() => {
  cleanup()
})

describe('PetStage', () => {
  it.each([
    ['penguin', 'Penguin'],
    ['fox', 'Fox'],
    ['hamster', 'Hamster'],
    ['tiger', 'Tiger'],
    ['lion', 'Lion'],
    ['horse', 'Horse'],
    ['parrot', 'Parrot'],
    ['unicorn', 'Unicorn'],
    ['midnight_cat', 'Midnight Cat'],
    ['panda', 'Panda'],
    ['goldfish', 'Goldfish'],
    ['lizard', 'Lizard'],
  ] as const)('renders %s mood assets as a first-class species', (speciesCode, label) => {
    const dashboard: Dashboard = {
      pet: {
        hunger: 100,
        happiness: 100,
        energy: 100,
        speciesCode,
        moodAssetCodes: {},
      },
      wallet: { coins: 0 },
    }
    const visualCatalog: PetVisualAsset[] = [
      {
        code: `${speciesCode}_happy_default`,
        assetType: 'PET_MOOD',
        speciesCode: speciesCode as SpeciesCode,
        moodCode: 'happy',
        label: `${label} Happy (Default)`,
        imagePath: `/pet-assets/${speciesCode.replace('_', '-')}/happy-default.png`,
        starter: true,
        active: true,
      },
    ]

    render(<PetStage dashboard={dashboard} visualCatalog={visualCatalog} />)

    expect(screen.getByText(`${label} — Happy`)).toBeTruthy()
    expect(screen.getByAltText(`${speciesCode} happy`).getAttribute('src')).toBe(`/pet-assets/${speciesCode.replace('_', '-')}/happy-default.png`)
  })
})
