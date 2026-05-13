// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ShopPage } from './ShopPage'
import type { Dashboard } from '../../lib/dashboard'
import type { GameShopItem } from '../../lib/gameTypes'
import type { PetVisualAsset } from '../../lib/petVisuals'

afterEach(() => {
  cleanup()
})

const thumbDogHappy: PetVisualAsset = {
  code: 'mood_dog_happy',
  assetType: 'PET_MOOD',
  speciesCode: 'dog',
  moodCode: 'happy',
  label: 'Dog happy',
  imagePath: '/pet-assets/dog/happy.png',
  starter: true,
  active: true,
}

const thumbCosmetic: PetVisualAsset = {
  code: 'bg_test',
  assetType: 'BACKGROUND',
  speciesCode: 'all',
  moodCode: '',
  label: 'Test BG',
  imagePath: '/cosmetic-staging/backgrounds/test.svg',
  starter: false,
  active: true,
}

describe('ShopPage', () => {
  it('renders species unlocks and disables already owned pets', () => {
    const onBuy = vi.fn()
    const dashboard: Dashboard = {
      pet: {
        hunger: 100,
        happiness: 100,
        energy: 100,
        speciesCode: 'dog',
        ownedSpeciesCodes: ['dog', 'cat', 'panda'],
      },
      wallet: { coins: 2500 },
    }
    const speciesShopItems: GameShopItem[] = [
      speciesItem('species_panda', 'Pet: Chubby Clueless Panda', 'panda', 1000),
      speciesItem('species_goldfish', 'Pet: Goldfish Aquarium', 'goldfish', 1000),
      speciesItem('species_unicorn', 'Legendary Pet: Unicorn', 'unicorn', 3000),
    ]

    render(
      <ShopPage
        consumableShopItems={[]}
        cosmeticShopItems={[]}
        speciesShopItems={speciesShopItems}
        visualCatalog={[thumbDogHappy]}
        dashboard={dashboard}
        onBuy={onBuy}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /pets/i }))
    expect(screen.getByRole('button', { name: 'Owned' }).hasAttribute('disabled')).toBe(true)

    fireEvent.click(screen.getAllByRole('button', { name: 'Buy' })[0])

    expect(onBuy).toHaveBeenCalledWith('species_goldfish')
    expect(screen.getByText('Legendary Pet: Unicorn')).toBeTruthy()
  })

  it('disables buy for cosmetics already in ownedVisualAssetCodes', () => {
    const onBuy = vi.fn()
    const dashboard: Dashboard = {
      pet: {
        hunger: 100,
        happiness: 100,
        energy: 100,
        speciesCode: 'dog',
        ownedVisualAssetCodes: ['bg_test'],
      },
      wallet: { coins: 2500 },
    }
    const cosmetic: GameShopItem = {
      code: 'shop_bg_test',
      type: 'COSMETIC',
      shopSection: 'COSMETICS',
      name: 'Test background',
      description: 'A test bg.',
      priceCoins: 100,
      oneTimePurchase: true,
      effects: [{ kind: 'GRANT_VISUAL', visualAssetCode: 'bg_test' }],
    }

    render(
      <ShopPage
        consumableShopItems={[]}
        cosmeticShopItems={[cosmetic]}
        speciesShopItems={[]}
        visualCatalog={[thumbCosmetic]}
        dashboard={dashboard}
        onBuy={onBuy}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /cosmetics/i }))
    const ownedBtn = screen.getByRole('button', { name: 'Owned' })
    expect(ownedBtn.hasAttribute('disabled')).toBe(true)
    fireEvent.click(ownedBtn)
    expect(onBuy).not.toHaveBeenCalled()
  })
})

function speciesItem(code: string, name: string, speciesCode: string, priceCoins: number): GameShopItem {
  return {
    code,
    type: 'SPECIES',
    shopSection: 'SPECIES',
    name,
    description: `Unlocks ${speciesCode}.`,
    priceCoins,
    oneTimePurchase: true,
    effects: [{ kind: 'GRANT_SPECIES', speciesCode }],
  }
}
