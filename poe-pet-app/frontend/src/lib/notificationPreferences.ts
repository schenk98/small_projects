function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export type NotificationPreferences = {
  lowHungerEnabled: boolean
  dailyAiSummaryEnabled: boolean
  updatedAt: string
}

export function parseNotificationPreferences(raw: unknown): NotificationPreferences {
  const item = isRecord(raw) ? raw : {}
  return {
    lowHungerEnabled: item.lowHungerEnabled === true,
    dailyAiSummaryEnabled: item.dailyAiSummaryEnabled === true,
    updatedAt: typeof item.updatedAt === 'string' ? item.updatedAt : '',
  }
}
