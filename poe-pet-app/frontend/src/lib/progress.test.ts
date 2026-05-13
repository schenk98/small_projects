import { describe, expect, it } from 'vitest'
import { parseProgressSummary } from './progress'

describe('parseProgressSummary', () => {
  it('parses API payloads into safe frontend defaults', () => {
    const parsed = parseProgressSummary({
      dailyChallenges: [
        {
          id: 5,
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
          completedAt: '2026-05-12T12:01:00Z',
          rewardCoins: 20,
          rewardGranted: true,
          rewardGrantedAt: '2026-05-12T12:01:00Z',
        },
      ],
      achievements: [
        {
          code: 'pet_namer',
          title: 'Name Tag',
          description: 'Name your pet.',
          category: 'pet',
          requiredCount: 1,
          progressCount: 2,
          progressPercent: 140,
          unlocked: true,
          unlockedAt: '2026-05-12T12:00:00Z',
        },
      ],
      recentActivity: [
        {
          id: 12,
          eventType: 'PET_RENAMED',
          source: 'pet',
          happenedAt: '2026-05-12T12:05:00Z',
          petName: 'Miki',
          speciesCode: 'dog',
          hunger: 80,
          happiness: 90,
          energy: 70,
          coinBalance: 123,
          details: { newName: 'Miki' },
        },
      ],
    })

    expect(parsed.dailyChallenges[0]).toMatchObject({
      title: 'Puzzle refresh',
      progressPercent: 100,
      rewardCoins: 20,
      rewardGranted: true,
    })
    expect(parsed.achievements[0]).toMatchObject({
      code: 'pet_namer',
      progressCount: 1,
      progressPercent: 100,
      unlocked: true,
    })
    expect(parsed.recentActivity[0]).toMatchObject({
      eventType: 'PET_RENAMED',
      coinBalance: 123,
      details: { newName: 'Miki' },
    })
  })

  it('falls back safely for malformed payloads', () => {
    const parsed = parseProgressSummary({ dailyChallenges: [null], achievements: [null], recentActivity: ['bad'] })

    expect(parsed.dailyChallenges[0]).toMatchObject({
      id: 0,
      title: '',
      requiredCount: 1,
      progressCount: 0,
      progressPercent: 0,
      completed: false,
      rewardCoins: 0,
    })
    expect(parsed.achievements[0]).toMatchObject({
      code: '',
      requiredCount: 1,
      progressCount: 0,
      progressPercent: 0,
      unlocked: false,
    })
    expect(parsed.recentActivity[0]).toMatchObject({
      id: 0,
      eventType: '',
      details: {},
    })
  })
})
