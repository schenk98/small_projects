import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { Tokens } from '../../auth/AuthScreens'
import type { Dashboard } from '../../lib/dashboard'
import { parseNotificationPreferences } from '../../lib/notificationPreferences'
import type { ApiJson } from '../types'

function formatGatewayHealthForUser(raw: unknown): string {
  if (raw == null || typeof raw !== 'object') return ''
  const o = raw as Record<string, unknown>
  if (o.ok === true) return 'AI link from the game server: OK.'
  const err = o.error
  if (typeof err === 'string') return `AI link from the game server: not reachable (${err}).`
  return 'AI link from the game server: not reachable.'
}

export function SettingsPage({
  setTokens,
  dashboard,
  apiJson,
  setMessage,
}: {
  setTokens: (t: Tokens | null) => void
  dashboard: Dashboard
  apiJson: ApiJson
  setMessage: (m: string) => void
}) {
  const [aiInfo, setAiInfo] = useState<Record<string, unknown> | null>(null)
  const [aiInfoStatus, setAiInfoStatus] = useState('Loading AI status…')
  const [aiBaseUrl, setAiBaseUrl] = useState('http://localhost:8090')
  const [aiApiKey, setAiApiKey] = useState('dev-secret')
  const [aiPetName, setAiPetName] = useState('Miki')
  const [aiTestMessage, setAiTestMessage] = useState('Say hi in one short sentence.')
  const [aiLastResult, setAiLastResult] = useState('')
  const [aiConfigSummary, setAiConfigSummary] = useState('')
  const [notificationDevResult, setNotificationDevResult] = useState('')
  const [lowHungerEnabled, setLowHungerEnabled] = useState(false)
  const [dailyAiSummaryEnabled, setDailyAiSummaryEnabled] = useState(false)
  const [notificationStatus, setNotificationStatus] = useState('')
  const [notificationLoading, setNotificationLoading] = useState(true)
  const [notificationSaving, setNotificationSaving] = useState(false)

  const loadNotificationPreferences = useCallback(async () => {
    const raw = await apiJson('/api/notification-preferences')
    const parsed = parseNotificationPreferences(raw)
    setLowHungerEnabled(parsed.lowHungerEnabled)
    setDailyAiSummaryEnabled(parsed.dailyAiSummaryEnabled)
    setNotificationStatus(parsed.updatedAt ? `Last saved: ${new Date(parsed.updatedAt).toLocaleString()}` : '')
  }, [apiJson])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        await loadNotificationPreferences()
      } catch (e) {
        if (cancelled) return
        setNotificationStatus(e instanceof Error ? e.message : 'Failed to load notification settings')
      } finally {
        if (!cancelled) setNotificationLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [loadNotificationPreferences])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const raw = await apiJson('/api/ai/info')
        if (!cancelled) {
          setAiInfo((raw ?? {}) as Record<string, unknown>)
          setAiInfoStatus('')
        }
      } catch (e) {
        if (!cancelled) {
          setAiInfo(null)
          setAiInfoStatus(e instanceof Error ? e.message : 'Failed to load AI status')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [apiJson])

  useEffect(() => {
    if (!dashboard.privileged) return
    const load = async () => {
      try {
        const raw = await apiJson('/api/dev/ai/config')
        setAiConfigSummary(JSON.stringify(raw ?? {}, null, 2))
      } catch {
        setAiConfigSummary('')
      }
    }
    void load()
  }, [dashboard.privileged, apiJson])

  const saveNotificationPreferences = async () => {
    try {
      setNotificationSaving(true)
      const raw = await apiJson('/api/notification-preferences', {
        method: 'POST',
        body: JSON.stringify({ lowHungerEnabled, dailyAiSummaryEnabled }),
      })
      const parsed = parseNotificationPreferences(raw)
      setLowHungerEnabled(parsed.lowHungerEnabled)
      setDailyAiSummaryEnabled(parsed.dailyAiSummaryEnabled)
      setNotificationStatus(parsed.updatedAt ? `Saved: ${new Date(parsed.updatedAt).toLocaleString()}` : 'Saved.')
      setMessage('Notification settings saved.')
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Failed to save notification settings'
      setNotificationStatus(msg)
      setMessage(msg)
    } finally {
      setNotificationSaving(false)
    }
  }

  const devAiSaveConfig = async () => {
    try {
      const raw = await apiJson('/api/dev/ai/config', { method: 'POST', body: JSON.stringify({ baseUrl: aiBaseUrl, apiKey: aiApiKey }) })
      const txt = JSON.stringify(raw ?? {}, null, 2)
      setAiLastResult(txt)
      setAiConfigSummary(txt)
      setMessage('Developer AI config saved.')
      window.alert('Saved.\n\n' + txt)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Failed to save AI config'
      setAiLastResult(msg)
      setMessage(msg)
      window.alert(msg)
    }
  }

  const devAiHealth = async () => {
    try {
      const raw = await apiJson('/api/dev/ai/health')
      const txt = JSON.stringify(raw ?? {}, null, 2)
      setAiLastResult(txt)
      setMessage('Developer AI health checked.')
      window.alert(txt)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'AI health check failed'
      setAiLastResult(msg)
      setMessage(msg)
      window.alert(msg)
    }
  }

  const devAiReady = async () => {
    try {
      const raw = await apiJson('/api/dev/ai/ready')
      const txt = JSON.stringify(raw ?? {}, null, 2)
      setAiLastResult(txt)
      setMessage('Developer AI readiness checked.')
      window.alert(txt)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'AI ready check failed'
      setAiLastResult(msg)
      setMessage(msg)
      window.alert(msg)
    }
  }

  const devAiChatTest = async () => {
    try {
      const raw = await apiJson('/api/dev/ai/chat-test', { method: 'POST', body: JSON.stringify({ petName: aiPetName, message: aiTestMessage }) })
      const txt = JSON.stringify(raw ?? {}, null, 2)
      setAiLastResult(txt)
      setMessage('Developer AI chat test completed.')
      window.alert(txt)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Developer AI chat test failed'
      setAiLastResult(msg)
      setMessage(msg)
      window.alert(msg)
    }
  }

  const devNotificationTest = async (path: string, successMessage: string) => {
    try {
      const raw = await apiJson(path, { method: 'POST' })
      const txt = JSON.stringify(raw ?? {}, null, 2)
      setNotificationDevResult(txt)
      setMessage(successMessage)
      window.alert(txt)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Notification test failed'
      setNotificationDevResult(msg)
      setMessage(msg)
      window.alert(msg)
    }
  }

  return (
    <div className="card">
      <h3>Account</h3>
      <p><Link to="/forgot-password">Reset password</Link></p>
      <button type="button" onClick={() => setTokens(null)}>Logout</button>
      <h3>AI chat</h3>
      {dashboard.privileged ? (
        <p className="muted">
          Pet chat calls the <strong>Local SLM Gateway</strong> from the Java backend (API key never ships to the browser).
          Model selection is configured on the gateway (for example <code>OLLAMA_MODEL</code> in <code>local-slm-gateway</code>).
        </p>
      ) : (
        <p className="muted">Pet chat runs through the game server. If the AI link is down, chat will not work.</p>
      )}
      {aiInfoStatus ? <p className="muted">{aiInfoStatus}</p> : null}
      {aiInfo ? (
        dashboard.privileged ? (
          <ul style={{ lineHeight: 1.6 }}>
            <li>
              <strong>Gateway configured (backend)</strong>: {String(aiInfo.gatewayConfigured)}
            </li>
            <li>
              <strong>Server guardrails</strong>: up to {String(aiInfo.maxUserMessageChars)} chars per message,{' '}
              {String(aiInfo.maxConversationTurns)} conversation turns, {String(aiInfo.maxAssistantChars)} chars max reply.
            </li>
            {aiInfo.gatewayHealth && typeof aiInfo.gatewayHealth === 'object' ? (
              <li>
                <strong>Gateway health</strong>:{' '}
                <code style={{ fontSize: '0.85em' }}>{JSON.stringify(aiInfo.gatewayHealth)}</code>
              </li>
            ) : null}
          </ul>
        ) : (
          <ul style={{ lineHeight: 1.6 }}>
            <li>
              <strong>Pet chat available</strong>: {String(aiInfo.gatewayConfigured)}
            </li>
            {aiInfo.gatewayHealth && typeof aiInfo.gatewayHealth === 'object' ? (
              <li>{formatGatewayHealthForUser(aiInfo.gatewayHealth)}</li>
            ) : null}
            <li className="muted">
              Messages are limited per turn so replies stay short and safe.
            </li>
          </ul>
        )
      ) : null}

      <h3>Notifications</h3>
      <p className="muted">
        These settings are the first app-facing part of the planned notification system. The first delivery targets are low-hunger reminders and a daily AI summary.
      </p>
      <label className="settings-toggle">
        <input
          type="checkbox"
          checked={lowHungerEnabled}
          disabled={notificationLoading || notificationSaving}
          onChange={(e) => setLowHungerEnabled(e.target.checked)}
        />
        <span className="settings-toggle-copy">
          <strong>Low-hunger reminder</strong>
          <br />
          <span className="muted">Send a notification when the pet drops below the low-hunger threshold.</span>
        </span>
      </label>
      <label className="settings-toggle">
        <input
          type="checkbox"
          checked={dailyAiSummaryEnabled}
          disabled={notificationLoading || notificationSaving}
          onChange={(e) => setDailyAiSummaryEnabled(e.target.checked)}
        />
        <span className="settings-toggle-copy">
          <strong>Daily AI summary</strong>
          <br />
          <span className="muted">Reserve a daily pet/AI summary notification for the upcoming notification service.</span>
        </span>
      </label>
      <div style={{ marginTop: 10 }}>
        <button type="button" disabled={notificationLoading || notificationSaving} onClick={() => void saveNotificationPreferences()}>
          {notificationSaving ? 'Saving...' : 'Save notification settings'}
        </button>
        <span className="muted" style={{ marginLeft: 8 }}>
          {notificationLoading ? 'Loading settings...' : notificationStatus || 'Ready.'}
        </span>
      </div>
      {dashboard.privileged && (
        <>
          <h3 id="developer-tools">Developer tools</h3>
          <p>
            The <strong>Developer</strong> dropdown in the top bar (grant coins, refill stats, set stat %) is only shown for privileged accounts.
          </p>
          <ul>
            <li>
              <strong>MongoDB:</strong> in database <code>poe_pet</code>, collection <code>users</code>, set <code>privileged: true</code> on your user document (same <code>_id</code> as JWT subject), then refresh after the next dashboard load.
            </li>
            <li>
              <strong>Or env / config:</strong> add your login email to <code>APP_PRIVILEGED_EMAILS</code> (comma-separated) or <code>app.privilegedEmails</code> in <code>backend/src/main/resources/application.yml</code>, then restart the Spring Boot server.
            </li>
          </ul>
        </>
      )}

      {dashboard.privileged && (
        <>
          <h3 id="developer-ai">Developer AI</h3>
          <p>
            Dev-only wiring for the standalone <code>local-slm-gateway</code>. The frontend talks to the backend; the backend calls the gateway (API key stays server-side).
          </p>

          <div style={{ display: 'grid', gap: 8, maxWidth: 720 }}>
            {aiConfigSummary && (
              <details>
                <summary>Backend AI gateway config (current)</summary>
                <pre style={{ whiteSpace: 'pre-wrap', background: '#111', color: '#ddd', padding: 12, borderRadius: 8 }}>
                  {aiConfigSummary}
                </pre>
              </details>
            )}
            <label>
              Gateway base URL
              <input value={aiBaseUrl} onChange={(e) => setAiBaseUrl(e.target.value)} placeholder="http://localhost:8090" />
            </label>
            <label>
              Gateway API key
              <input value={aiApiKey} onChange={(e) => setAiApiKey(e.target.value)} placeholder="dev-secret" />
            </label>
            <label>
              Pet name (for prefix)
              <input value={aiPetName} onChange={(e) => setAiPetName(e.target.value)} placeholder="Miki" />
            </label>
            <label>
              Test message
              <textarea value={aiTestMessage} onChange={(e) => setAiTestMessage(e.target.value)} rows={3} />
            </label>

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <button type="button" onClick={() => void devAiSaveConfig()}>Save config</button>
              <button type="button" onClick={() => void devAiHealth()}>Health</button>
              <button type="button" onClick={() => void devAiReady()}>Ready</button>
              <button type="button" onClick={() => void devAiChatTest()}>Chat test</button>
            </div>

            {aiLastResult && (
              <pre style={{ whiteSpace: 'pre-wrap', background: '#111', color: '#ddd', padding: 12, borderRadius: 8 }}>
                {aiLastResult}
              </pre>
            )}

            <h3 id="developer-notifications">Developer notifications</h3>
            <p>
              Trigger the first notification types manually against the SOAP side-service. This is useful before relying on scheduled delivery.
            </p>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <button type="button" onClick={() => void devNotificationTest('/api/dev/notifications/test-low-hunger', 'Developer low-hunger notification attempted.')}>
                Test low-hunger email
              </button>
              <button type="button" onClick={() => void devNotificationTest('/api/dev/notifications/test-daily-summary', 'Developer daily summary attempted.')}>
                Test daily summary email
              </button>
            </div>
            {notificationDevResult && (
              <pre style={{ whiteSpace: 'pre-wrap', background: '#111', color: '#ddd', padding: 12, borderRadius: 8 }}>
                {notificationDevResult}
              </pre>
            )}
          </div>
        </>
      )}
    </div>
  )
}

