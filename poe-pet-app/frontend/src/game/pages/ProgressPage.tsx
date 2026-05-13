import { useCallback, useEffect, useState } from 'react'
import { parseProgressSummary, type ActivityEvent, type ProgressSummary } from '../../lib/progress'
import type { Dashboard } from '../../lib/dashboard'
import type { ApiJson } from '../types'

function formatWhen(value: string) {
  if (!value) return 'Unknown time'
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString()
}

function titleForEvent(eventType: string) {
  switch (eventType) {
    case 'ACCOUNT_REGISTERED': return 'Account created'
    case 'USER_LOGGED_IN': return 'Logged in'
    case 'PET_RENAMED': return 'Pet renamed'
    case 'PET_SPECIES_SET': return 'Species changed'
    case 'SHOP_PURCHASED': return 'Shop purchase'
    case 'CONSUMABLE_USED': return 'Consumable used'
    case 'MINIGAME_FINISHED': return 'Minigame finished'
    case 'AI_CHAT_SENT': return 'AI chat used'
    default: return eventType.replaceAll('_', ' ').toLowerCase()
  }
}

function describeEvent(event: ActivityEvent) {
  const d = event.details
  switch (event.eventType) {
    case 'PET_RENAMED':
      return `${String(d.previousName || 'Pet')} -> ${String(d.newName || event.petName || 'Pet')}`
    case 'PET_SPECIES_SET':
      return `Species: ${String(d.speciesCode || event.speciesCode || 'unknown')}`
    case 'SHOP_PURCHASED':
      return `${String(d.itemCode || 'item')} for ${String(d.priceCoins || '?')} coins`
    case 'CONSUMABLE_USED':
      return `${String(d.itemCode || 'item')} (${String(d.result || 'used')})`
    case 'MINIGAME_FINISHED':
      return `${String(d.gameCode || 'game')} | reward ${String(d.coinsReward || 0)}`
    case 'AI_CHAT_SENT':
      return d.fallbackUsed === true
        ? `Fallback used (${String(d.fallbackReason || 'unknown')})`
        : 'Assistant answered normally'
    case 'ACCOUNT_REGISTERED':
      return 'Verification required after signup'
    default:
      return event.source ? `Source: ${event.source}` : 'Recorded activity'
  }
}

function statsSnapshot(event: ActivityEvent) {
  const stats = [
    event.hunger != null ? `Hunger ${Math.round(event.hunger)}` : '',
    event.happiness != null ? `Happiness ${Math.round(event.happiness)}` : '',
    event.energy != null ? `Energy ${Math.round(event.energy)}` : '',
    event.coinBalance != null ? `Coins ${event.coinBalance}` : '',
  ].filter(Boolean)
  return stats.join(' | ')
}

function loadUnlockedAtCache(): Record<string, string> {
  try {
    const raw = localStorage.getItem('poe_achievement_unlocked_at')
    return raw ? (JSON.parse(raw) as Record<string, string>) : {}
  } catch {
    return {}
  }
}

function saveUnlockedAtCache(map: Record<string, string>) {
  try {
    localStorage.setItem('poe_achievement_unlocked_at', JSON.stringify(map))
  } catch {
    /* ignore */
  }
}

export function ProgressPage({ apiJson, dashboard }: { apiJson: ApiJson; dashboard: Dashboard }) {
  const [summary, setSummary] = useState<ProgressSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tab, setTab] = useState<'daily' | 'achievements'>(() => 'daily')
  const [unlockedAtCache, setUnlockedAtCache] = useState<Record<string, string>>(() => loadUnlockedAtCache())

  const fetchSummary = useCallback(
    async () => parseProgressSummary(await apiJson('/api/progress/summary')),
    [apiJson],
  )

  const applySummary = useCallback((next: ProgressSummary) => {
    setSummary(next)
    setUnlockedAtCache((prev) => {
      const merged = { ...prev }
      let changed = false
      for (const a of next.achievements) {
        if (!a.code) continue
        if (a.unlocked && !merged[a.code]) {
          merged[a.code] = a.unlockedAt || new Date().toISOString()
          changed = true
        }
      }
      if (changed) {
        saveUnlockedAtCache(merged)
        return merged
      }
      return prev
    })
  }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError('')
      applySummary(await fetchSummary())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load progress')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let cancelled = false

    void (async () => {
      try {
        const next = await fetchSummary()
        if (cancelled) return
        applySummary(next)
        setError('')
      } catch (e) {
        if (cancelled) return
        setError(e instanceof Error ? e.message : 'Failed to load progress')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [fetchSummary, applySummary])

  if (loading && !summary) return <div className="card pane">Loading progress...</div>

  return (
    <div className="card pane">
      <div className="row" style={{ alignItems: 'flex-start' }}>
        <div>
          <h3 style={{ marginTop: 0, marginBottom: 6 }}>Progress</h3>
          {dashboard.privileged ? (
            <p className="muted" style={{ marginTop: 0 }}>
              Daily challenges, permanent achievements, and recent activity recorded in the SQL progress system.
            </p>
          ) : null}
        </div>
        {dashboard.privileged ? <button type="button" onClick={() => void load()} disabled={loading}>Refresh</button> : null}
      </div>

      <div className="subnav" style={{ marginTop: 6 }}>
        <button type="button" className={tab === 'daily' ? 'tab active' : 'tab'} onClick={() => setTab('daily')}>Daily</button>
        <button type="button" className={tab === 'achievements' ? 'tab active' : 'tab'} onClick={() => setTab('achievements')}>Achievements</button>
        {dashboard.privileged ? <span className="muted" style={{ alignSelf: 'center' }}>Dev: activity stream visible below</span> : null}
      </div>

      {error ? <div className="info" style={{ marginBottom: 12 }}>{error}</div> : null}

      {tab === 'daily' ? (
        <>
          <h3>Daily challenges</h3>
          {!summary || summary.dailyChallenges.length === 0 ? (
            <p className="muted">Today&apos;s challenges have not been generated yet.</p>
          ) : summary.dailyChallenges.map((challenge) => (
            <div key={challenge.id || `${challenge.challengeDate}-${challenge.slotOrder}`} className="row" style={{ display: 'block' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'baseline' }}>
                <div>
                  <b>{challenge.title}</b>
                  <div className="muted">{challenge.description}</div>
                </div>
                <div style={{ textAlign: 'right', flexShrink: 0 }}>
                  <div>{challenge.completed ? 'Completed' : 'Active today'}</div>
                  <div className="muted">{challenge.progressCount}/{challenge.requiredCount} | +{challenge.rewardCoins} coins</div>
                </div>
              </div>
              <div className="bar" style={{ width: '100%', marginTop: 8 }}>
                <div className="fill bar-energy" style={{ width: `${challenge.progressPercent}%` }} />
              </div>
              <div className="muted" style={{ marginTop: 6 }}>
                {challenge.rewardGranted ? 'Reward granted' : 'Reward waiting for completion'}
                {challenge.completedAt ? ` | completed ${formatWhen(challenge.completedAt)}` : ''}
                {!challenge.completed && challenge.lastEventAt ? ` | last progress ${formatWhen(challenge.lastEventAt)}` : ''}
              </div>
            </div>
          ))}
        </>
      ) : null}

      {tab === 'achievements' ? (
        <>
          <h3>Achievements</h3>
          {!summary || summary.achievements.length === 0 ? (
            <p className="muted">No achievements yet.</p>
          ) : summary.achievements.map((achievement) => {
            const cachedUnlockedAt = achievement.code ? unlockedAtCache[achievement.code] : ''
            const unlockedWhen = achievement.unlockedAt || cachedUnlockedAt
            return (
              <div key={achievement.code} className="row" style={{ display: 'block' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'baseline' }}>
                  <div>
                    <b>{achievement.title}</b>
                    {dashboard.privileged ? <div className="muted">{achievement.description}</div> : null}
                  </div>
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <div>{achievement.unlocked ? 'Unlocked' : 'In progress'}</div>
                    <div className="muted">{achievement.progressCount}/{achievement.requiredCount}</div>
                  </div>
                </div>
                <div className="bar" style={{ width: '100%', marginTop: 8 }}>
                  <div className="fill bar-happiness" style={{ width: `${achievement.progressPercent}%` }} />
                </div>
                <div className="muted" style={{ marginTop: 6 }}>
                  Category: {achievement.category || 'general'}
                  {achievement.unlocked && unlockedWhen ? ` | unlocked ${formatWhen(unlockedWhen)}` : ''}
                  {!achievement.unlocked && achievement.lastEventAt ? ` | last progress ${formatWhen(achievement.lastEventAt)}` : ''}
                </div>
              </div>
            )
          })}
        </>
      ) : null}

      {dashboard.privileged ? (
        <>
          <h3 style={{ marginTop: '1.25rem' }}>Recent activity</h3>
          {!summary || summary.recentActivity.length === 0 ? (
            <p className="muted">No activity recorded yet.</p>
          ) : summary.recentActivity.map((event) => (
            <div key={event.id} className="row" style={{ display: 'block' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'baseline' }}>
                <b>{titleForEvent(event.eventType)}</b>
                <span className="muted">{formatWhen(event.happenedAt)}</span>
              </div>
              <div>{describeEvent(event)}</div>
              {statsSnapshot(event) ? <div className="muted" style={{ marginTop: 4 }}>{statsSnapshot(event)}</div> : null}
            </div>
          ))}
        </>
      ) : null}
    </div>
  )
}
