import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { API_BASE_URL } from '../config'

export type Tokens = { accessToken: string; refreshToken: string; email: string }

const API = API_BASE_URL

export function AuthLogin({ setTokens, setMessage }: { setTokens: (t: Tokens) => void; setMessage: (m: string) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    const res = await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
    const data = await res.json()
    if (!res.ok) return setMessage(data.error || 'Login failed')
    setTokens(data)
    setMessage('Logged in.')
    navigate('/app/shop')
  }
  return (
    <form className="card" onSubmit={onSubmit}>
      <h2>Login</h2>
      <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <input placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button type="submit">Login</button>
      <p><Link to="/register">Create account</Link> | <Link to="/forgot-password">Forgot password</Link></p>
    </form>
  )
}

export function AuthRegister({ setMessage }: { setMessage: (m: string) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    const res = await fetch(`${API}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
    const data = await res.json()
    const msg = data.message || data.error || 'Done'
    setMessage(msg)
    if (res.ok && typeof msg === 'string' && msg.includes('log in now')) navigate('/login')
  }
  return (
    <form className="card" onSubmit={onSubmit}>
      <h2>Register</h2>
      <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <input placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button type="submit">Register</button>
      <p><Link to="/login">Back to login</Link></p>
    </form>
  )
}

export function VerifyEmail({ setMessage }: { setMessage: (m: string) => void }) {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')
  // Avoid setting state synchronously inside the effect body (eslint rule).
  const [status, setStatus] = useState<'pending' | 'ok' | 'error'>(token ? 'pending' : 'error')
  useEffect(() => {
    if (!token) {
      setMessage('Missing verification token.')
      return
    }
    fetch(`${API}/auth/verify-email?token=${encodeURIComponent(token)}`).then(async (res) => {
      const data = await res.json()
      if (!res.ok || data.error) {
        setStatus('error')
        setMessage(data.error || 'Verification failed.')
        return
      }
      setStatus('ok')
      setMessage(data.message || 'Email verified.')
    }).catch(() => {
      setStatus('error')
      setMessage('Verification request failed.')
    })
  }, [token, setMessage])
  return (
    <div className="card">
      <h2>Email verification</h2>
      {status === 'pending' && <p>Verifying your account...</p>}
      {status === 'ok' && <p>Email verified successfully. You can now <Link to="/login">log in</Link>.</p>}
      {status === 'error' && <p>Verification failed. Please request a new verification email.</p>}
    </div>
  )
}

export function ForgotPassword({ setMessage }: { setMessage: (m: string) => void }) {
  const [email, setEmail] = useState('')
  return <form className="card" onSubmit={async (e) => {
    e.preventDefault()
    const res = await fetch(`${API}/auth/forgot-password`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) })
    const data = await res.json()
    setMessage(data.message || data.error)
  }}>
    <h2>Forgot password</h2>
    <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
    <button type="submit">Send reset email</button>
  </form>
}

export function ResetPassword({ setMessage }: { setMessage: (m: string) => void }) {
  const [pw, setPw] = useState('')
  const params = new URLSearchParams(window.location.search)
  return <form className="card" onSubmit={async (e) => {
    e.preventDefault()
    const token = params.get('token')
    const res = await fetch(`${API}/auth/reset-password`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token, newPassword: pw }) })
    const data = await res.json()
    setMessage(data.message || data.error)
  }}>
    <h2>Reset password</h2>
    <input placeholder="New password" type="password" value={pw} onChange={(e) => setPw(e.target.value)} />
    <button type="submit">Reset</button>
  </form>
}
