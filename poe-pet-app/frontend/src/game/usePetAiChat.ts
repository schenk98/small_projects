import { useState } from 'react'
import type { Dashboard } from '../lib/dashboard'
import type { ApiJson } from './types'

/** Shown when the request fails (proxy timeout, offline, etc.) so the chat thread is never empty. */
const CLIENT_AI_UNAVAILABLE =
  "Hmm—the chat line is taking too long or is unavailable right now. *nudges you playfully* Try again in a moment."

export function usePetAiChat(
  apiJson: ApiJson,
  dashboard: Dashboard | null,
  setMessage: (m: string) => void,
) {
  const [aiAnswer, setAiAnswer] = useState('')
  const [aiFallbackUsed, setAiFallbackUsed] = useState(false)
  const [aiPrompt, setAiPrompt] = useState('')
  const [aiConversation, setAiConversation] = useState<{ role: 'user' | 'assistant'; content: string }[]>([])
  const [aiLoading, setAiLoading] = useState(false)

  const sendAiPrompt = async () => {
    if (!dashboard) return
    const msg = aiPrompt.trim()
    if (!msg) return
    setAiPrompt('')
    const nextConv = [...aiConversation, { role: 'user' as const, content: msg }].slice(-6)
    setAiConversation(nextConv)
    try {
      setAiLoading(true)
      const raw = await apiJson('/api/ai/chat', { method: 'POST', body: JSON.stringify({ conversation: nextConv, message: msg }) })
      const data = (raw ?? {}) as Record<string, unknown>
      const text = typeof data.assistantText === 'string' ? data.assistantText : '…'
      setAiFallbackUsed(data.fallbackUsed === true)
      setAiAnswer(text)
      if (data.fallbackUsed === true && typeof data.fallbackReason === 'string') {
        setMessage(`AI fallback: ${data.fallbackReason}`)
      }
      setAiConversation([...nextConv, { role: 'assistant' as const, content: text }].slice(-6))
    } catch (e) {
      const err = e instanceof Error ? e.message : 'AI call failed'
      setMessage(err)
      setAiFallbackUsed(true)
      setAiAnswer(CLIENT_AI_UNAVAILABLE)
      setAiConversation([...nextConv, { role: 'assistant' as const, content: CLIENT_AI_UNAVAILABLE }].slice(-6))
    } finally {
      setAiLoading(false)
    }
  }

  const clearConversation = () => {
    setAiConversation([])
    setAiAnswer('')
    setAiFallbackUsed(false)
  }

  return {
    aiAnswer,
    setAiAnswer,
    aiFallbackUsed,
    aiPrompt,
    setAiPrompt,
    aiConversation,
    aiLoading,
    sendAiPrompt,
    clearConversation,
  }
}
