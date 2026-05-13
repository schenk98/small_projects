// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SettingsPage } from './SettingsPage'

afterEach(() => {
  cleanup()
})

const dashboard = {
  pet: { hunger: 100, happiness: 100, energy: 100, name: 'Pet', speciesCode: 'dog' as const },
  wallet: { coins: 300 },
  privileged: false,
}

describe('SettingsPage', () => {
  it('loads notification preferences and saves updated toggles', async () => {
    const apiJson = vi.fn(async (path: string, init?: RequestInit) => {
      if (path === '/api/ai/info') {
        return { gatewayConfigured: false, maxUserMessageChars: 1800, maxConversationTurns: 6, maxAssistantChars: 3500 }
      }
      if (path === '/api/notification-preferences' && (!init || init.method === 'GET')) {
        return { lowHungerEnabled: false, dailyAiSummaryEnabled: true, updatedAt: '2026-05-12T12:00:00Z' }
      }
      if (path === '/api/notification-preferences' && init?.method === 'POST') {
        return { lowHungerEnabled: true, dailyAiSummaryEnabled: true, updatedAt: '2026-05-12T12:05:00Z' }
      }
      throw new Error('Unexpected request: ' + path)
    })
    const setTokens = vi.fn()
    const setMessage = vi.fn()

    render(
      <MemoryRouter>
        <SettingsPage
          setTokens={setTokens}
          dashboard={dashboard}
          apiJson={apiJson}
          setMessage={setMessage}
        />
      </MemoryRouter>,
    )

    await screen.findByText(/Last saved:/)
    const [lowHungerToggle] = screen.getAllByRole('checkbox')
    fireEvent.click(lowHungerToggle)
    fireEvent.click(screen.getByRole('button', { name: 'Save notification settings' }))

    await waitFor(() => expect(apiJson).toHaveBeenLastCalledWith('/api/notification-preferences', {
      method: 'POST',
      body: JSON.stringify({ lowHungerEnabled: true, dailyAiSummaryEnabled: true }),
    }))
    expect(setMessage).toHaveBeenCalledWith('Notification settings saved.')
    expect(screen.getByText(/Saved:/)).toBeTruthy()
  })

  it('shows a load error when notification settings request fails', async () => {
    const apiJson = vi.fn(async (path: string) => {
      if (path === '/api/ai/info') {
        return { gatewayConfigured: false, maxUserMessageChars: 1800, maxConversationTurns: 6, maxAssistantChars: 3500 }
      }
      throw new Error('notification load failed')
    })

    render(
      <MemoryRouter>
        <SettingsPage
          setTokens={vi.fn()}
          dashboard={dashboard}
          apiJson={apiJson}
          setMessage={vi.fn()}
        />
      </MemoryRouter>,
    )

    expect(await screen.findByText('notification load failed')).toBeTruthy()
  })
})
