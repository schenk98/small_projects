function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function asString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function asNumberOrNull(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function asBoolean(value: unknown): boolean {
  return value === true
}

function asDetails(value: unknown): Record<string, unknown> {
  return isRecord(value) ? value : {}
}

export type AchievementProgress = {
  code: string
  title: string
  description: string
  category: string
  requiredCount: number
  progressCount: number
  progressPercent: number
  unlocked: boolean
  unlockedAt: string
  lastEventAt: string
}

export type DailyChallenge = {
  id: number
  challengeDate: string
  slotOrder: number
  challengeType: string
  title: string
  description: string
  matchValue: string
  requiredCount: number
  progressCount: number
  progressPercent: number
  completed: boolean
  completedAt: string
  rewardCoins: number
  rewardGranted: boolean
  rewardGrantedAt: string
  lastEventAt: string
}

export type ActivityEvent = {
  id: number
  eventType: string
  source: string
  happenedAt: string
  petName: string
  speciesCode: string
  hunger: number | null
  happiness: number | null
  energy: number | null
  coinBalance: number | null
  details: Record<string, unknown>
}

export type ProgressSummary = {
  dailyChallenges: DailyChallenge[]
  achievements: AchievementProgress[]
  recentActivity: ActivityEvent[]
}

export function parseProgressSummary(raw: unknown): ProgressSummary {
  const root = isRecord(raw) ? raw : {}
  const dailyChallenges = Array.isArray(root.dailyChallenges) ? root.dailyChallenges : []
  const achievements = Array.isArray(root.achievements) ? root.achievements : []
  const recentActivity = Array.isArray(root.recentActivity) ? root.recentActivity : []

  return {
    dailyChallenges: dailyChallenges.map((entry) => {
      const item = isRecord(entry) ? entry : {}
      return {
        id: asNumberOrNull(item.id) ?? 0,
        challengeDate: asString(item.challengeDate),
        slotOrder: Math.max(0, asNumberOrNull(item.slotOrder) ?? 0),
        challengeType: asString(item.challengeType),
        title: asString(item.title),
        description: asString(item.description),
        matchValue: asString(item.matchValue),
        requiredCount: Math.max(1, asNumberOrNull(item.requiredCount) ?? 1),
        progressCount: Math.max(0, asNumberOrNull(item.progressCount) ?? 0),
        progressPercent: Math.max(0, Math.min(100, asNumberOrNull(item.progressPercent) ?? 0)),
        completed: asBoolean(item.completed),
        completedAt: asString(item.completedAt),
        rewardCoins: Math.max(0, asNumberOrNull(item.rewardCoins) ?? 0),
        rewardGranted: asBoolean(item.rewardGranted),
        rewardGrantedAt: asString(item.rewardGrantedAt),
        lastEventAt: asString(item.lastEventAt),
      }
    }),
    achievements: achievements.map((entry) => {
      const item = isRecord(entry) ? entry : {}
      const requiredCount = Math.max(1, asNumberOrNull(item.requiredCount) ?? 1)
      const rawProgressCount = Math.max(0, asNumberOrNull(item.progressCount) ?? 0)
      const unlocked = asBoolean(item.unlocked)
      const progressCount = unlocked ? Math.min(requiredCount, rawProgressCount) : rawProgressCount
      return {
        code: asString(item.code),
        title: asString(item.title),
        description: asString(item.description),
        category: asString(item.category),
        requiredCount,
        progressCount,
        progressPercent: Math.max(0, Math.min(100, asNumberOrNull(item.progressPercent) ?? 0)),
        unlocked,
        unlockedAt: asString(item.unlockedAt),
        lastEventAt: asString(item.lastEventAt),
      }
    }),
    recentActivity: recentActivity.map((entry) => {
      const item = isRecord(entry) ? entry : {}
      return {
        id: asNumberOrNull(item.id) ?? 0,
        eventType: asString(item.eventType),
        source: asString(item.source),
        happenedAt: asString(item.happenedAt),
        petName: asString(item.petName),
        speciesCode: asString(item.speciesCode),
        hunger: asNumberOrNull(item.hunger),
        happiness: asNumberOrNull(item.happiness),
        energy: asNumberOrNull(item.energy),
        coinBalance: asNumberOrNull(item.coinBalance),
        details: asDetails(item.details),
      }
    }),
  }
}
