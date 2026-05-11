import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { Tokens } from '../../auth/AuthScreens'
import type { Dashboard } from '../../lib/dashboard'
import type { ApiJson } from '../types'

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
  const [aiBaseUrl, setAiBaseUrl] = useState('http://localhost:8090')
  const [aiApiKey, setAiApiKey] = useState('dev-secret')
  const [aiPetName, setAiPetName] = useState('Miki')
  const [aiTestMessage, setAiTestMessage] = useState('Say hi in one short sentence.')
  const [aiLastResult, setAiLastResult] = useState('')
  const [aiConfigSummary, setAiConfigSummary] = useState('')

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

  return (
    <div className="card">
      <h3>Account</h3>
      <p><Link to="/forgot-password">Reset password</Link></p>
      <button type="button" onClick={() => setTokens(null)}>Logout</button>
      <h3 id="developer-tools">Developer tools</h3>
      <p>
        The <strong>Developer</strong> dropdown in the top bar (grant coins, refill stats, set stat %) is hidden unless your account is flagged as privileged.
      </p>
      <ul>
        <li>
          <strong>MongoDB:</strong> in database <code>poe_pet</code>, collection <code>users</code>, set <code>privileged: true</code> on your user document (same <code>_id</code> as JWT subject), then restart nothing — just refresh the app after the next dashboard load.
        </li>
        <li>
          <strong>Or env / config:</strong> add your login email to <code>APP_PRIVILEGED_EMAILS</code> (comma-separated) or <code>app.privilegedEmails</code> in <code>backend/src/main/resources/application.yml</code>, then restart the Spring Boot server.
        </li>
      </ul>

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
          </div>
        </>
      )}
    </div>
  )
}

