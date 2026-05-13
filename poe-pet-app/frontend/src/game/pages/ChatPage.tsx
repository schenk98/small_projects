import type { Dashboard } from '../../lib/dashboard'

export function ChatPage({
  dashboard,
  aiAnswer,
  aiFallbackUsed,
  aiPrompt,
  setAiPrompt,
  aiConversation,
  aiLoading,
  sendAiPrompt,
  clearConversation,
}: {
  dashboard: Dashboard
  aiAnswer: string
  aiFallbackUsed: boolean
  aiPrompt: string
  setAiPrompt: (s: string) => void
  aiConversation: { role: 'user' | 'assistant'; content: string }[]
  aiLoading: boolean
  sendAiPrompt: () => void | Promise<void>
  clearConversation: () => void
}) {
  const petLabel = dashboard.pet.name || 'Pet'

  return (
    <div className="card pane">
      <h3>Chat</h3>
      <p className="muted">
        Talk with {petLabel} in character. Replies use the{' '}
        <strong>Local SLM Gateway</strong> when it is configured on the server; otherwise you see playful fallback text.
        See <strong>Settings</strong> for gateway status and model hint.
      </p>
      <div className="chat-log" data-testid="chat-log">
        {aiConversation.length === 0 ? (
          <p className="muted">No messages yet. Say hello below.</p>
        ) : (
          aiConversation.map((turn, i) => (
            <div key={i} className={`chat-turn chat-turn-${turn.role}`}>
              <div className="chat-turn-label">{turn.role === 'user' ? 'You' : petLabel}</div>
              <div className="chat-turn-body">{turn.content}</div>
            </div>
          ))
        )}
      </div>
      <div
        className="chat-latest"
        style={{
          fontFamily: aiFallbackUsed ? 'ui-sans-serif, system-ui, sans-serif' : 'ui-serif, Georgia, serif',
          fontStyle: aiFallbackUsed ? 'italic' : 'normal',
          opacity: aiFallbackUsed ? 0.85 : 1,
          whiteSpace: 'pre-wrap',
        }}
      >
        {aiLoading ? (
          <span>
            {petLabel} is thinking
            <span className="thinking-dots" aria-hidden="true">
              …
            </span>
          </span>
        ) : (
          aiAnswer || '—'
        )}
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
        <input
          style={{ flex: '1 1 200px' }}
          data-testid="chat-input"
          value={aiPrompt}
          onChange={(e) => setAiPrompt(e.target.value)}
          placeholder={`Message ${petLabel}…`}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              void sendAiPrompt()
            }
          }}
        />
        <button type="button" data-testid="chat-send" onClick={() => void sendAiPrompt()} disabled={aiLoading}>
          {aiLoading ? 'Thinking…' : 'Send'}
        </button>
        <button type="button" className="muted-button" onClick={clearConversation} disabled={aiLoading || aiConversation.length === 0}>
          Clear thread
        </button>
      </div>
    </div>
  )
}
