const API = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080'
const MAILHOG = process.env.PLAYWRIGHT_MAILHOG_URL ?? 'http://localhost:8025'

type MailhogItem = {
  Content?: { Headers?: { To?: string[] }; Body?: string }
}

type MailhogList = { items?: MailhogItem[] }

export async function registerUser(email: string, password: string): Promise<void> {
  const reg = await fetch(`${API}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!reg.ok) {
    throw new Error(`register failed: ${reg.status} ${await reg.text()}`)
  }
}

export async function fetchVerificationTokenForEmail(email: string): Promise<string> {
  await new Promise((r) => setTimeout(r, 800))
  const res = await fetch(`${MAILHOG}/api/v2/messages`)
  if (!res.ok) {
    throw new Error(`mailhog: ${res.status}`)
  }
  const data = (await res.json()) as MailhogList
  const msg = data.items?.find((it) => {
    const to = it.Content?.Headers?.To
    return Array.isArray(to) && to.some((t) => t.includes(email))
  })
  if (!msg?.Content?.Body) {
    throw new Error('Verification email not found in MailHog')
  }
  const tokenMatch = /token=([^\s"'<>]+)/.exec(msg.Content.Body)
  if (!tokenMatch?.[1]) {
    throw new Error('Token not found in email body')
  }
  return decodeURIComponent(tokenMatch[1].trim())
}

export async function verifyEmail(token: string): Promise<void> {
  const v = await fetch(`${API}/auth/verify-email?token=${encodeURIComponent(token)}`)
  if (!v.ok) {
    throw new Error(`verify-email failed: ${v.status} ${await v.text()}`)
  }
}

export async function loginAccessToken(email: string, password: string): Promise<string> {
  const r = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!r.ok) {
    throw new Error(`login failed: ${r.status} ${await r.text()}`)
  }
  const j = (await r.json()) as { accessToken?: string }
  if (!j.accessToken) {
    throw new Error('login response missing accessToken')
  }
  return j.accessToken
}

export async function waitForMailhogSubjectContaining(substr: string, maxAttempts = 30): Promise<void> {
  for (let i = 0; i < maxAttempts; i++) {
    const res = await fetch(`${MAILHOG}/api/v2/messages`)
    if (!res.ok) throw new Error(`mailhog poll: ${res.status}`)
    const data = (await res.json()) as { items?: { Content?: { Headers?: { Subject?: string[] } } }[] }
    const hit = data.items?.some((it) => {
      const subj = it.Content?.Headers?.Subject?.[0] ?? ''
      return subj.toLowerCase().includes(substr.toLowerCase())
    })
    if (hit) return
    await new Promise((r) => setTimeout(r, 500))
  }
  throw new Error(`No MailHog message with subject containing: ${substr}`)
}
