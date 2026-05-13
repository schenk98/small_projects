import { describe, expect, it } from 'vitest'
import { parseNotificationPreferences } from './notificationPreferences'

describe('parseNotificationPreferences', () => {
  it('parses booleans and timestamp from backend payload', () => {
    expect(parseNotificationPreferences({
      lowHungerEnabled: true,
      dailyAiSummaryEnabled: false,
      updatedAt: '2026-05-12T12:00:00Z',
    })).toEqual({
      lowHungerEnabled: true,
      dailyAiSummaryEnabled: false,
      updatedAt: '2026-05-12T12:00:00Z',
    })
  })

  it('falls back safely when payload is malformed', () => {
    expect(parseNotificationPreferences(null)).toEqual({
      lowHungerEnabled: false,
      dailyAiSummaryEnabled: false,
      updatedAt: '',
    })
  })
})
