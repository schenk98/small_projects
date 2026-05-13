// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CustomizePage } from './CustomizePage'
import type { Dashboard } from '../../lib/dashboard'

afterEach(() => {
  cleanup()
})

describe('CustomizePage', () => {
  it.each([
    ['Penguin', 'penguin'],
    ['Fox', 'fox'],
    ['Hamster', 'hamster'],
    ['Tiger', 'tiger'],
    ['Lion', 'lion'],
    ['Horse', 'horse'],
    ['Parrot', 'parrot'],
    ['Unicorn', 'unicorn'],
    ['Midnight Cat', 'midnight_cat'],
    ['Panda', 'panda'],
    ['Goldfish', 'goldfish'],
    ['Lizard', 'lizard'],
  ] as const)('lets the player select %s as a pet species', (_label, speciesCode) => {
    const onSetSpecies = vi.fn()
    const dashboard: Dashboard = {
      pet: {
        hunger: 100,
        happiness: 100,
        energy: 100,
        speciesCode: 'dog',
        ownedSpeciesCodes: ['dog', 'cat', speciesCode],
        moodAssetCodes: {},
        ownedVisualAssetCodes: [],
      },
      wallet: { coins: 0 },
    }

    render(
      <CustomizePage
        dashboard={dashboard}
        visualCatalog={[]}
        onSetName={vi.fn()}
        onSetSpecies={onSetSpecies}
        onSetMoodAsset={vi.fn()}
        onEquipVisualLayers={vi.fn()}
      />,
    )

    const select = screen.getByRole('combobox', { name: 'Pet species' }) as HTMLSelectElement
    fireEvent.change(select, { target: { value: speciesCode } })

    expect(onSetSpecies).toHaveBeenCalledWith(speciesCode)
  })

  it('does not show locked species until purchased', () => {
    const onSetSpecies = vi.fn()
    const dashboard: Dashboard = {
      pet: {
        hunger: 100,
        happiness: 100,
        energy: 100,
        speciesCode: 'dog',
        ownedSpeciesCodes: ['dog', 'cat'],
        moodAssetCodes: {},
        ownedVisualAssetCodes: [],
      },
      wallet: { coins: 0 },
    }

    render(
      <CustomizePage
        dashboard={dashboard}
        visualCatalog={[]}
        onSetName={vi.fn()}
        onSetSpecies={onSetSpecies}
        onSetMoodAsset={vi.fn()}
        onEquipVisualLayers={vi.fn()}
      />,
    )

    expect(screen.getByRole('combobox', { name: 'Pet species' })).toBeTruthy()
    expect(screen.queryByText('Goldfish')).toBeFalsy()
  })
})
