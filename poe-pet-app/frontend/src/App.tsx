import { useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import {
  AuthLogin,
  AuthRegister,
  ForgotPassword,
  ResetPassword,
  VerifyEmail,
  type Tokens,
} from './auth/AuthScreens'
import './App.css'
import { GameApp } from './game/GameApp'

function App() {
  const [tokens, setTokens] = useState<Tokens | null>(() => {
    const raw = localStorage.getItem('poe_tokens')
    return raw ? JSON.parse(raw) : null
  })
  const [message, setMessage] = useState('')
  const location = useLocation()

  useEffect(() => {
    if (tokens) localStorage.setItem('poe_tokens', JSON.stringify(tokens))
    else localStorage.removeItem('poe_tokens')
  }, [tokens])

  const authHeaders = useMemo(() => ({
    'Content-Type': 'application/json',
    Authorization: `Bearer ${tokens?.accessToken ?? ''}`,
  }), [tokens])

  const authed = Boolean(tokens?.accessToken)
  return (
    <div className="page">
      <h1>Poe Pet</h1>
      {message && <p className="info">{message}</p>}
      <Routes>
        <Route path="/" element={authed ? <Navigate to="/app/shop" /> : <Navigate to="/login" />} />
        <Route path="/login" element={<AuthLogin setTokens={setTokens} setMessage={setMessage} />} />
        <Route path="/register" element={<AuthRegister setMessage={setMessage} />} />
        <Route path="/verify-email" element={<VerifyEmail setMessage={setMessage} />} />
        <Route path="/forgot-password" element={<ForgotPassword setMessage={setMessage} />} />
        <Route path="/reset-password" element={<ResetPassword setMessage={setMessage} />} />
        <Route
          path="/app/*"
          element={authed ? <GameApp authHeaders={authHeaders} setTokens={setTokens} locationPath={location.pathname} setMessage={setMessage} /> : <Navigate to="/login" />}
        />
      </Routes>
    </div>
  )
}
export default App
