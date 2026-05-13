// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { GameApp } from './GameApp'
import { useGameData } from './useGameData'

vi.mock('./useGameData', () => ({
  useGameData: vi.fn(),
}))

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('GameApp AI thinking mood', () => {
  it('switches pet stage to thinking while the AI request is pending and returns after completion', async () => {
    let resolveChat: ((value: unknown) => void) | undefined
    const apiJson = vi.fn().mockImplementation((path: string) => {
      if (path !== '/api/ai/chat') {
        throw new Error(`Unexpected path ${path}`)
      }
      return new Promise((resolve) => {
        resolveChat = resolve
      })
    })

    vi.mocked(useGameData).mockReturnValue({
      apiJson,
      dashboard: {
        pet: {
          hunger: 100,
          happiness: 100,
          energy: 100,
          name: 'Miki',
          speciesCode: 'dog',
          moodAssetCodes: {},
          ownedVisualAssetCodes: [],
          equippedBackgroundAssetCode: null,
          equippedForegroundAssetCode: null,
        },
        wallet: { coins: 25 },
        privileged: false,
      },
      visualCatalog: [
        {
          code: 'dog_happy_default',
          assetType: 'PET_MOOD',
          speciesCode: 'dog',
          moodCode: 'happy',
          label: 'Dog Happy (Default)',
          imagePath: '/pet-assets/dog/happy-default.png',
          starter: true,
          active: true,
        },
        {
          code: 'dog_thinking_default',
          assetType: 'PET_MOOD',
          speciesCode: 'dog',
          moodCode: 'thinking',
          label: 'Dog Thinking (Default)',
          imagePath: '/pet-assets/dog/thinking-default.png',
          starter: true,
          active: true,
        },
      ],
      shopItems: [],
      minigames: [],
      inventory: [],
      rewardPreview: undefined,
      loadError: '',
      setLoadError: vi.fn(),
      refresh: vi.fn().mockResolvedValue(undefined),
      setDashboard: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/app/placeholder']}>
        <Routes>
          <Route
            path="/app/*"
            element={
              <GameApp
                authHeaders={{ Authorization: 'Bearer test', 'Content-Type': 'application/json' }}
                setTokens={vi.fn()}
                locationPath="/app/placeholder"
                setMessage={vi.fn()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByPlaceholderText('Message Miki…'), { target: { value: 'Hello pet' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Dog — Thinking')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Thinking…' }).hasAttribute('disabled')).toBe(true)

    resolveChat?.({ assistantText: 'Woof!', fallbackUsed: false })

    await waitFor(() => expect(screen.getByText('Dog — Happy')).toBeTruthy())
    expect(screen.getByText('Woof!')).toBeTruthy()
  })
})
