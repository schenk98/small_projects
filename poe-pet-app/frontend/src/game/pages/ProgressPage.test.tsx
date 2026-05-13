// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ProgressPage } from './ProgressPage'
import type { Dashboard } from '../../lib/dashboard'

afterEach(() => {
  cleanup()
})

describe('ProgressPage', () => {
  it('renders achievements and recent activity from the API payload', async () => {
    const apiJson = vi.fn().mockResolvedValue({
      dailyChallenges: [
        {
          id: 3,
          challengeDate: '2026-05-12',
          slotOrder: 1,
          challengeType: 'FINISH_MINIGAME',
          title: 'Puzzle refresh',
          description: 'Finish Puzzle Swap once today.',
          matchValue: 'puzzle_swap',
          requiredCount: 1,
          progressCount: 1,
          progressPercent: 100,
          completed: true,
          completedAt: '2026-05-12T12:00:00Z',
          rewardCoins: 20,
          rewardGranted: true,
          rewardGrantedAt: '2026-05-12T12:00:00Z',
        },
      ],
      achievements: [
        {
          code: 'pet_namer',
          title: 'Name Tag',
          description: 'Give your pet a custom name.',
          category: 'pet',
          requiredCount: 1,
          progressCount: 1,
          progressPercent: 100,
          unlocked: true,
          unlockedAt: '2026-05-12T12:00:00Z',
        },
      ],
      recentActivity: [
        {
          id: 1,
          eventType: 'SHOP_PURCHASED',
          source: 'shop',
          happenedAt: '2026-05-12T12:05:00Z',
          petName: 'Miki',
          speciesCode: 'dog',
          hunger: 80,
          happiness: 90,
          energy: 70,
          coinBalance: 200,
          details: { itemCode: 'apple', priceCoins: 10 },
        },
      ],
    })

    const dashboard: Dashboard = {
      pet: { hunger: 100, happiness: 100, energy: 100, speciesCode: 'dog' },
      wallet: { coins: 0 },
      privileged: true,
    }

    render(<ProgressPage apiJson={apiJson} dashboard={dashboard} />)

    expect(await screen.findByText('Puzzle refresh')).toBeTruthy()
    expect(screen.getByText('Finish Puzzle Swap once today.')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Achievements' }))
    expect(await screen.findByText('Name Tag')).toBeTruthy()
    expect(screen.getByText('Give your pet a custom name.')).toBeTruthy()
    expect(screen.getByText('Shop purchase')).toBeTruthy()
    expect(screen.getByText('apple for 10 coins')).toBeTruthy()
  })

  it('reloads when refresh is clicked after an error', async () => {
    const apiJson = vi.fn()
      .mockRejectedValueOnce(new Error('progress failed'))
      .mockResolvedValueOnce({ dailyChallenges: [], achievements: [], recentActivity: [] })

    const dashboard: Dashboard = {
      pet: { hunger: 100, happiness: 100, energy: 100, speciesCode: 'dog' },
      wallet: { coins: 0 },
      privileged: true,
    }

    render(<ProgressPage apiJson={apiJson} dashboard={dashboard} />)

    expect(await screen.findByText('progress failed')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }))

    await waitFor(() => expect(apiJson).toHaveBeenCalledTimes(2))
    fireEvent.click(screen.getByRole('button', { name: 'Achievements' }))
    expect(await screen.findByText('No achievements yet.')).toBeTruthy()
  })
})
