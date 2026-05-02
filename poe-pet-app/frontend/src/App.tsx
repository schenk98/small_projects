import { useEffect, useMemo, useRef, useState } from 'react'
import type { MouseEvent as ReactMouseEvent } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  AuthLogin,
  AuthRegister,
  ForgotPassword,
  ResetPassword,
  VerifyEmail,
  type Tokens,
} from './auth/AuthScreens'
import { API_BASE_URL } from './config'
import { createJsonApiClient } from './lib/gameApi'
import type { GameInventoryItem, GameShopItem, MinigameInfo } from './lib/gameTypes'
import { warnNotEnoughEnergy } from './lib/energy'
import { mergeRewardPreview, parseRewardPreviewBlob, type Dashboard } from './lib/rewardPreview'
import { isSessionExpiredMessage } from './lib/session'
import {
  deriveMood,
  MOOD_LABELS,
  MOOD_SLOT_ORDER,
  type MoodCode,
  type PetVisualAsset,
  type SpeciesCode,
} from './lib/petVisuals'
import './App.css'
import {
  advanceCheckersState,
  createInitialCheckersState,
  generateMoves as checkersGenerateMoves,
  pickAiMove as checkersPickAiMove,
  repairJumpContinuation,
  winnerFromMoves as checkersWinner,
  type CheckersDifficulty,
  type CheckersMove,
  type CheckersState,
} from './minigames/checkersAi'
import { AI_MOVE_DELAY_MS, delay } from './minigames/constants'
import {
  MINESWEEP_DIMS,
  createEmptyField,
  exposeAllMinesAfterLoss,
  isWinningBoard,
  placeMines,
  revealCell,
  toggleFlag,
  type MineDifficulty,
} from './minigames/minesweeperLogic'
import type { MinigameEndSummary } from './minigames/types'
import { CheckersBoardView } from './minigames/ui/CheckersBoardView'

const API = API_BASE_URL

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

function GameApp({ authHeaders, setTokens, locationPath, setMessage }:
{ authHeaders: Record<string, string>; setTokens: (t: Tokens | null) => void; locationPath: string; setMessage: (m: string) => void }) {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [visualCatalog, setVisualCatalog] = useState<PetVisualAsset[]>([])
  const [shopItems, setShopItems] = useState<GameShopItem[]>([])
  const [minigames, setMinigames] = useState<MinigameInfo[]>([])
  const [inv, setInv] = useState<GameInventoryItem[]>([])
  const [currentNumber, setCurrentNumber] = useState<number | null>(null)
  const [streak, setStreak] = useState(0)
  const [minigameOpen, setMinigameOpen] = useState(false)
  const [activeMinigame, setActiveMinigame] = useState<
    'higher_lower' | 'puzzle_swap' | 'connect4_ai' | 'minesweep_ai' | 'checkers_ai' | null
  >(null)
  const [puzzleSize, setPuzzleSize] = useState(3)
  const [puzzleBoard, setPuzzleBoard] = useState<number[]>([])
  const [puzzleImage, setPuzzleImage] = useState('')
  const [puzzleFirst, setPuzzleFirst] = useState<number | null>(null)
  const [puzzleMoves, setPuzzleMoves] = useState(0)
  const [puzzleStartedAt, setPuzzleStartedAt] = useState<number>(0)
  const [connectDifficulty, setConnectDifficulty] = useState<'easy' | 'medium' | 'hard'>('easy')
  const [connectBoard, setConnectBoard] = useState<number[][]>(Array.from({ length: 6 }, () => Array(7).fill(0)))
  const [connectTurn, setConnectTurn] = useState<'human' | 'ai'>('human')
  const [connectStatus, setConnectStatus] = useState('Your move')
  const [connectHumanMoves, setConnectHumanMoves] = useState(0)
  const [mineDifficulty, setMineDifficulty] = useState<MineDifficulty>('easy')
  const [mineField, setMineField] = useState<number[][]>([])
  const [mineGrid, setMineGrid] = useState<boolean[][] | null>(null)
  /** True after the player hits a mine (round lost; UI locked until summary). */
  const [mineGameOver, setMineGameOver] = useState(false)
  const [checkersDifficulty, setCheckersDifficulty] = useState<CheckersDifficulty>('easy')
  const [checkersState, setCheckersState] = useState<CheckersState>(() => createInitialCheckersState())
  const [checkersPick, setCheckersPick] = useState<{ r: number; c: number } | null>(null)
  const mineRoundEndedRef = useRef(false)
  const checkersRoundEndedRef = useRef(false)
  /** Stubs overwritten each render once handlers exist; must live above loading guard (Rules of Hooks). */
  const dismissMinigameRef = useRef({
    closeMinigameFully: async () => {},
    quitGame: async () => {},
    minigameEndSummary: null as MinigameEndSummary | null,
    activeMinigame: null as typeof activeMinigame,
  })
  const [loadError, setLoadError] = useState<string>('')
  const [minigameEndSummary, setMinigameEndSummary] = useState<MinigameEndSummary | null>(null)
  const [devStatsOpen, setDevStatsOpen] = useState(false)
  const [devH, setDevH] = useState('100')
  const [devHa, setDevHa] = useState('100')
  const [devE, setDevE] = useState('100')

  const apiJson = useMemo(() => createJsonApiClient(API, authHeaders), [authHeaders])

  const refresh = async (): Promise<Dashboard | undefined> => {
    try {
      setLoadError('')
      const [d, s, mg, i] = await Promise.all([
        apiJson('/api/dashboard'),
        apiJson('/api/shop/items'),
        apiJson('/api/minigames'),
        apiJson('/api/inventory'),
      ])
      let catalog: PetVisualAsset[] = []
      try {
        const raw = await apiJson('/api/pet-visuals/catalog')
        catalog = Array.isArray(raw) ? raw : []
      } catch {
        /* Older backend without pet visuals — app still loads; center pet uses no catalog rows. */
      }
      let dash = d as Dashboard
      if (!mergeRewardPreview(dash)) {
        try {
          const blob = await apiJson('/api/minigames/reward-preview')
          const parsed = parseRewardPreviewBlob(blob)
          if (parsed) dash = { ...dash, rewardPreview: parsed }
        } catch {
          /* Older backend without this route — payouts stay empty until API is updated. */
        }
      }
      setDashboard(dash)
      setVisualCatalog(catalog)
      setShopItems(s as GameShopItem[])
      setMinigames(mg as MinigameInfo[])
      setInv(i as GameInventoryItem[])
      return dash
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load game data'
      if (isSessionExpiredMessage(message)) {
        setTokens(null)
        setLoadError('')
        setMessage('Session expired. Please sign in again.')
        navigate('/login', { replace: true })
        return undefined
      }
      setLoadError(message)
      setMessage(message)
      return undefined
    }
  }
  // Initial load only; `authHeaders` is stable per login from parent `useMemo`.
  useEffect(() => {
    void refresh()
  }, [])

  const rewardPreview = useMemo(() => (dashboard ? mergeRewardPreview(dashboard) : undefined), [dashboard])

  useEffect(() => {
    if (!minigameOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return
      e.preventDefault()
      const d = dismissMinigameRef.current
      if (d.minigameEndSummary) {
        void d.closeMinigameFully()
        return
      }
      const msg = d.activeMinigame === 'higher_lower'
        ? 'Quit Higher/Lower and claim coins for your current streak?'
        : 'Leave this minigame? Half the energy cost will be refunded; no coins or happiness change.'
      if (window.confirm(msg)) void d.quitGame()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [minigameOpen])

  if (!dashboard) return <div className="card">{loadError ? `Loading failed: ${loadError}` : 'Loading...'}</div>

  const speciesCode: SpeciesCode = (dashboard.pet.speciesCode === 'cat' ? 'cat' : 'dog')
  const activeMood = deriveMood(dashboard.pet)
  const ownedVisuals = new Set(dashboard.pet.ownedVisualAssetCodes ?? [])
  const moodAssetsForSpecies = visualCatalog.filter((a) => a.assetType === 'PET_MOOD' && a.speciesCode === speciesCode)
  const moodSlots = (dashboard.pet.moodAssetCodes || {}) as Partial<Record<MoodCode, string>>
  const pathForVisualCode = (code: string | null | undefined) =>
    (code && visualCatalog.find((a) => a.code === code)?.imagePath) || ''
  const equippedBg = dashboard.pet.equippedBackgroundAssetCode || ''
  const equippedFg = dashboard.pet.equippedForegroundAssetCode || ''
  const backgroundLayerUrl = pathForVisualCode(equippedBg)
  const foregroundLayerUrl = pathForVisualCode(equippedFg)
  const moodImagePath = (() => {
    const selectedCode = moodSlots[activeMood]
    if (selectedCode) {
      const sel = moodAssetsForSpecies.find((a) => a.code === selectedCode)
      if (sel) return sel.imagePath
    }
    return moodAssetsForSpecies.find((a) => a.moodCode === activeMood)?.imagePath
      || moodAssetsForSpecies.find((a) => a.moodCode === 'happy')?.imagePath
      || ''
  })()

  const nav = [
    ['/app/shop', 'Shop'],
    ['/app/minigames', 'Minigames'],
    ['/app/inventory', 'Inventory'],
    ['/app/customize', 'Customize'],
    ['/app/settings', 'Settings'],
  ]

  const buy = async (itemCode: string) => {
    const data = await apiJson('/api/shop/purchase', { method: 'POST', body: JSON.stringify({ itemCode }) })
    setMessage(data.error || 'Purchased')
    await refresh()
  }

  const useItem = async (itemCode: string) => {
    let data = await apiJson('/api/inventory/use', { method: 'POST', body: JSON.stringify({ itemCode }) })
    if (data.needsConfirmation) {
      data = await apiJson('/api/inventory/use', { method: 'POST', body: JSON.stringify({ itemCode, confirmOverwrite: true }) })
    }
    setMessage(data.error || data.message || 'Used')
    await refresh()
  }

  const setSpecies = async (next: SpeciesCode) => {
    await apiJson('/api/pet-visuals/species', {
      method: 'POST',
      body: JSON.stringify({ speciesCode: next }),
    })
    setMessage(`Species switched to ${next}.`)
    await refresh()
  }

  const setMoodAsset = async (mood: MoodCode, code: string) => {
    const next = { ...moodSlots, [mood]: code === 'none' ? undefined : code }
    await apiJson('/api/pet-visuals/mood-assets', {
      method: 'POST',
      body: JSON.stringify({ moodAssetCodes: next }),
    })
    setMessage(`Updated ${MOOD_LABELS[mood]} image.`)
    await refresh()
  }

  const equipVisualLayers = async (backgroundAssetCode: string, foregroundAssetCode: string) => {
    await apiJson('/api/pet-visuals/equip-layers', {
      method: 'POST',
      body: JSON.stringify({
        backgroundAssetCode: backgroundAssetCode === 'none' || !backgroundAssetCode ? 'none' : backgroundAssetCode,
        foregroundAssetCode: foregroundAssetCode === 'none' || !foregroundAssetCode ? 'none' : foregroundAssetCode,
      }),
    })
    setMessage('Updated scene layers.')
    await refresh()
  }

  const energyCostFor = (code: string) =>
    rewardPreview?.energyCosts?.[code] ?? minigames.find((x) => x.code === code)?.energyCost ?? 99999

  const startGame = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('higher_lower')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setMinigameEndSummary(null)
      const data = await apiJson('/api/minigames/higher-lower/start', { method: 'POST' })
      if (data.error) return setMessage(data.error)
      setCurrentNumber(data.currentNumber)
      setStreak(data.streak)
      setActiveMinigame('higher_lower')
      setMinigameOpen(true)
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start minigame'
      if (/energy/i.test(msg)) window.alert(`Higher/Lower: ${msg}`)
      setMessage(msg)
    }
  }

  /** Returns false if browser energy check fails (server still enforces). */
  function assertEnergy(need: number, have: number) {
    if (have < need) {
      warnNotEnoughEnergy(need, have, 'This minigame')
      return false
    }
    return true
  }

  const guess = async (direction: 'HIGHER' | 'LOWER') => {
    try {
      const data = await apiJson('/api/minigames/higher-lower/guess', { method: 'POST', body: JSON.stringify({ guess: direction }) })
      if (data.error) return setMessage(data.error)
      setCurrentNumber(data.next); setStreak(data.streak)
      const dash = await refresh()
      if (data.gameOver && dash) {
        setMinigameEndSummary({
          title: 'Higher / Lower — round over',
          coinsEarned: data.coinsReward ?? 0,
          happinessDeltaPercent: data.happinessDeltaPercent ?? 0,
          hunger: Math.round(dash.pet.hunger),
          happiness: Math.round(dash.pet.happiness),
          energy: Math.round(dash.pet.energy),
          hlSnap: { previous: data.previous, next: data.next, streak: data.streak },
          detailLines: [`Streak credited: ${data.streak}`, data.coinsBaseBeforeMultiplier != null && data.coinsBaseBeforeMultiplier !== data.coinsReward
            ? `Base coins: ${data.coinsBaseBeforeMultiplier} × multiplier ${data.coinMultiplierApplied}`
            : undefined].filter(Boolean) as string[],
        })
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Guess failed')
    }
  }

  /** Close voluntarily (with payout) during Higher/Lower, or discard modal for puzzle/Connect 4. */
  const quitGame = async () => {
    if (activeMinigame === 'higher_lower') {
      try {
        const data = await apiJson('/api/minigames/higher-lower/quit', { method: 'POST' })
        const dash = await refresh()
        if (data.noActiveSession || !dash) {
          setMinigameOpen(false)
          setActiveMinigame(null)
          setCurrentNumber(null)
          setStreak(0)
          setMinigameEndSummary(null)
          return
        }
        setMinigameEndSummary({
          title: 'Higher / Lower — finished',
          coinsEarned: data.coinsReward ?? 0,
          happinessDeltaPercent: data.happinessDeltaPercent ?? 0,
          hunger: Math.round(dash.pet.hunger),
          happiness: Math.round(dash.pet.happiness),
          energy: Math.round(dash.pet.energy),
          hlSnap: currentNumber != null ? { previous: currentNumber, next: currentNumber, streak } : undefined,
          detailLines: data.coinsBaseBeforeMultiplier != null && data.coinsBaseBeforeMultiplier !== data.coinsReward
            ? [`Base coins: ${data.coinsBaseBeforeMultiplier} × ${data.coinMultiplierApplied}`]
            : [],
        })
        setCurrentNumber(null)
        setStreak(0)
      } catch (err) {
        window.alert(err instanceof Error ? err.message : 'Could not quit minigame')
        setMinigameOpen(false)
        setActiveMinigame(null)
        setMinigameEndSummary(null)
      }
      return
    }
    const abandonCodes = ['puzzle_swap', 'minesweep_ai', 'connect4_ai', 'checkers_ai'] as const
    const code = activeMinigame
    if (code && (abandonCodes as readonly string[]).includes(code)) {
      try {
        await apiJson(`/api/minigames/${code}/abandon-simple`, { method: 'POST' })
      } catch (err) {
        window.alert(err instanceof Error ? err.message : 'Could not record abandon')
      }
    }
    setMinigameOpen(false)
    setActiveMinigame(null)
    setMinigameEndSummary(null)
    await refresh()
  }

  const startPuzzle = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('puzzle_swap')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setMinigameEndSummary(null)
      await apiJson('/api/minigames/puzzle_swap/start-simple', { method: 'POST' })
      const size = puzzleSize
      const arr = Array.from({ length: size * size }, (_, i) => i)
      arr.sort(() => Math.random() - 0.5)
      const images = [
        'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80',
        'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80',
      ]
      setPuzzleImage(images[Math.floor(Math.random() * images.length)])
      setPuzzleBoard(arr)
      setPuzzleFirst(null)
      setPuzzleMoves(0)
      setPuzzleStartedAt(Date.now())
      setActiveMinigame('puzzle_swap')
      setMinigameOpen(true)
      setMessage('Puzzle started.')
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start puzzle'
      if (/energy/i.test(msg)) window.alert(`Puzzle: ${msg}`)
      setMessage(msg)
    }
  }

  const puzzleSwap = async (idx: number) => {
    if (puzzleFirst === null) {
      setPuzzleFirst(idx)
      return
    }
    if (puzzleFirst === idx) {
      setPuzzleFirst(null)
      return
    }
    const next = [...puzzleBoard]
    const a = next[puzzleFirst]
    next[puzzleFirst] = next[idx]
    next[idx] = a
    const newMoves = puzzleMoves + 1
    setPuzzleBoard(next)
    setPuzzleFirst(null)
    setPuzzleMoves(newMoves)
    const solved = next.every((v, i) => v === i)
    if (solved) {
      const elapsedSeconds = Math.max(1, Math.floor((Date.now() - puzzleStartedAt) / 1000))
      const speedBonus = Math.max(0, 180 - elapsedSeconds) / 10
      const score = Math.max(1, Math.floor((puzzleSize * puzzleSize) - newMoves + 8 + speedBonus))
      const result = await apiJson('/api/minigames/puzzle_swap/finish-simple', {
        method: 'POST',
        body: JSON.stringify({ score }),
      })
      const dash = await refresh()
      if (dash) {
        const solvedTiles = Array.from({ length: puzzleSize * puzzleSize }, (_, i) => i)
        setMinigameEndSummary({
          title: 'Puzzle solved',
          coinsEarned: result.coinsReward ?? 0,
          happinessDeltaPercent: result.happinessDeltaPercent ?? 0,
          hunger: Math.round(dash.pet.hunger),
          happiness: Math.round(dash.pet.happiness),
          energy: Math.round(dash.pet.energy),
          puzzleSnap: { size: puzzleSize, tiles: solvedTiles, imageUrl: puzzleImage },
          detailLines: [`Score: ${score}`, result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
            ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
            : undefined].filter(Boolean) as string[],
        })
      }
    }
  }

  const createEmptyConnect = () => Array.from({ length: 6 }, () => Array(7).fill(0))
  const dropInColumn = (board: number[][], col: number, player: number) => {
    const next = board.map((r) => [...r])
    for (let row = 5; row >= 0; row--) {
      if (next[row][col] === 0) {
        next[row][col] = player
        return next
      }
    }
    return null
  }
  const checkWinner = (board: number[][]) => {
    const dirs = [[1, 0], [0, 1], [1, 1], [1, -1]]
    for (let r = 0; r < 6; r++) {
      for (let c = 0; c < 7; c++) {
        if (board[r][c] === 0) continue
        for (const [dr, dc] of dirs) {
          let ok = true
          for (let k = 1; k < 4; k++) {
            const rr = r + dr * k
            const cc = c + dc * k
            if (rr < 0 || rr >= 6 || cc < 0 || cc >= 7 || board[rr][cc] !== board[r][c]) {
              ok = false
              break
            }
          }
          if (ok) return board[r][c]
        }
      }
    }
    return board.flat().every((v) => v !== 0) ? 3 : 0
  }
  const scoreWindow = (windowCells: number[]) => {
    const ai = windowCells.filter((v) => v === 2).length
    const human = windowCells.filter((v) => v === 1).length
    const empty = windowCells.filter((v) => v === 0).length
    if (ai === 4) return 10000
    if (human === 4) return -10000
    if (ai === 3 && empty === 1) return 120
    if (ai === 2 && empty === 2) return 12
    if (human === 3 && empty === 1) return -180
    if (human === 2 && empty === 2) return -20
    return 0
  }
  const evalBoard = (board: number[][]) => {
    const winner = checkWinner(board)
    if (winner === 2) return 100000
    if (winner === 1) return -100000
    let score = 0
    const centerCol = board.map((r) => r[3])
    score += centerCol.filter((v) => v === 2).length * 6
    for (let r = 0; r < 6; r++) {
      for (let c = 0; c < 7; c++) {
        if (c + 3 < 7) score += scoreWindow([board[r][c], board[r][c + 1], board[r][c + 2], board[r][c + 3]])
        if (r + 3 < 6) score += scoreWindow([board[r][c], board[r + 1][c], board[r + 2][c], board[r + 3][c]])
        if (r + 3 < 6 && c + 3 < 7) score += scoreWindow([board[r][c], board[r + 1][c + 1], board[r + 2][c + 2], board[r + 3][c + 3]])
        if (r - 3 >= 0 && c + 3 < 7) score += scoreWindow([board[r][c], board[r - 1][c + 1], board[r - 2][c + 2], board[r - 3][c + 3]])
      }
    }
    return score
  }
  const minimax = (board: number[][], depth: number, alpha: number, beta: number, maximizing: boolean): { score: number; col: number } => {
    const winner = checkWinner(board)
    if (depth === 0 || winner !== 0) return { score: evalBoard(board), col: -1 }
    const validCols = Array.from({ length: 7 }, (_, c) => c).filter((c) => board[0][c] === 0)
    if (maximizing) {
      let best = { score: -Infinity, col: validCols[0] ?? -1 }
      for (const col of validCols) {
        const next = dropInColumn(board, col, 2)
        if (!next) continue
        const res = minimax(next, depth - 1, alpha, beta, false)
        if (res.score > best.score) best = { score: res.score, col }
        alpha = Math.max(alpha, best.score)
        if (alpha >= beta) break
      }
      return best
    }
    let best = { score: Infinity, col: validCols[0] ?? -1 }
    for (const col of validCols) {
      const next = dropInColumn(board, col, 1)
      if (!next) continue
      const res = minimax(next, depth - 1, alpha, beta, true)
      if (res.score < best.score) best = { score: res.score, col }
      beta = Math.min(beta, best.score)
      if (alpha >= beta) break
    }
    return best
  }
  const aiMove = async (board: number[][]) => {
    const valid = Array.from({ length: 7 }, (_, c) => c).filter((c) => board[0][c] === 0)
    if (valid.length === 0) return board
    let col = valid[Math.floor(Math.random() * valid.length)]
    if (connectDifficulty === 'medium') col = minimax(board, 1, -Infinity, Infinity, true).col
    if (connectDifficulty === 'hard') col = minimax(board, 3, -Infinity, Infinity, true).col
    if (col < 0 || board[0][col] !== 0) col = valid[0]
    return dropInColumn(board, col, 2) || board
  }

  const startConnect4 = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('connect4_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setMinigameEndSummary(null)
      await apiJson('/api/minigames/connect4_ai/start-simple', { method: 'POST' })
      setConnectHumanMoves(0)
      setConnectBoard(createEmptyConnect())
      setConnectTurn('human')
      setConnectStatus('Your move')
      setActiveMinigame('connect4_ai')
      setMinigameOpen(true)
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Connect 4'
      if (/energy/i.test(msg)) window.alert(`Connect 4: ${msg}`)
      setMessage(msg)
    }
  }

  const playConnect = async (col: number) => {
    if (activeMinigame !== 'connect4_ai' || connectTurn !== 'human') return
    const humanBoard = dropInColumn(connectBoard, col, 1)
    if (!humanBoard) return
    const humanMovesAfter = connectHumanMoves + 1
    setConnectHumanMoves(humanMovesAfter)
    setConnectBoard(humanBoard)
    const finishBody = (score: number) => JSON.stringify({
      score,
      connectDifficulty,
      connectHumanMoves: humanMovesAfter,
    })
    const humanWinner = checkWinner(humanBoard)
    if (humanWinner === 1 || humanWinner === 3) {
      const score = humanWinner === 1 ? 2 : 1
      const result = await apiJson('/api/minigames/connect4_ai/finish-simple', { method: 'POST', body: finishBody(score) })
      const dash = await refresh()
      setConnectStatus(humanWinner === 1 ? 'You win' : 'Draw')
      if (dash) {
        setMinigameEndSummary({
          title: humanWinner === 1 ? 'Connect 4 — you win' : 'Connect 4 — draw',
          coinsEarned: result.coinsReward ?? 0,
          happinessDeltaPercent: result.happinessDeltaPercent ?? 0,
          hunger: Math.round(dash.pet.hunger),
          happiness: Math.round(dash.pet.happiness),
          energy: Math.round(dash.pet.energy),
          connect4Board: humanBoard.map((r) => [...r]),
          detailLines: [
            `Difficulty: ${connectDifficulty} • Your moves: ${humanMovesAfter}`,
            result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
              ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
              : undefined,
          ].filter(Boolean) as string[],
        })
      }
      return
    }
    setConnectTurn('ai')
    setConnectStatus('AI thinking...')
    await delay(AI_MOVE_DELAY_MS)
    const aiBoardState = await aiMove(humanBoard)
    setConnectBoard(aiBoardState)
    const aiWinner = checkWinner(aiBoardState)
    if (aiWinner === 2 || aiWinner === 3) {
      const score = aiWinner === 3 ? 1 : 0
      const result = await apiJson('/api/minigames/connect4_ai/finish-simple', { method: 'POST', body: finishBody(score) })
      const dash = await refresh()
      setConnectStatus(aiWinner === 2 ? 'AI wins' : 'Draw')
      if (dash) {
        setMinigameEndSummary({
          title: aiWinner === 2 ? 'Connect 4 — AI wins' : 'Connect 4 — draw',
          coinsEarned: result.coinsReward ?? 0,
          happinessDeltaPercent: result.happinessDeltaPercent ?? 0,
          hunger: Math.round(dash.pet.hunger),
          happiness: Math.round(dash.pet.happiness),
          energy: Math.round(dash.pet.energy),
          connect4Board: aiBoardState.map((r) => [...r]),
          detailLines: [
            `Difficulty: ${connectDifficulty} • Your moves: ${humanMovesAfter}`,
            result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
              ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
              : undefined,
          ].filter(Boolean) as string[],
        })
      }
      return
    }
    setConnectTurn('human')
    setConnectStatus('Your move')
  }

  const startMineSweep = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('minesweep_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setMinigameEndSummary(null)
      await apiJson('/api/minigames/minesweep_ai/start-simple', { method: 'POST' })
      const dim = MINESWEEP_DIMS[mineDifficulty]
      setMineField(createEmptyField(dim.rows, dim.cols))
      setMineGrid(null)
      setMineGameOver(false)
      mineRoundEndedRef.current = false
      setActiveMinigame('minesweep_ai')
      setMinigameOpen(true)
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Minesweeper'
      if (/energy/i.test(msg)) window.alert(`Minesweeper: ${msg}`)
      setMessage(msg)
    }
  }

  const finishMineOutcome = async (score: number) => {
    if (mineRoundEndedRef.current) return
    mineRoundEndedRef.current = true
    const result = await apiJson('/api/minigames/minesweep_ai/finish-simple', {
      method: 'POST',
      body: JSON.stringify({ score, difficulty: mineDifficulty }),
    })
    const dash = await refresh()
    if (dash) {
      setMinigameEndSummary({
        title: score > 0 ? 'Minesweeper — cleared' : 'Minesweeper — hit a mine',
        coinsEarned: result.coinsReward ?? 0,
        happinessDeltaPercent: result.happinessDeltaPercent ?? 0,
        hunger: Math.round(dash.pet.hunger),
        happiness: Math.round(dash.pet.happiness),
        energy: Math.round(dash.pet.energy),
        detailLines: [
          `Difficulty: ${mineDifficulty}`,
          result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
            ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
            : undefined,
        ].filter(Boolean) as string[],
      })
    }
  }

  const mineCellClick = async (r: number, c: number) => {
    if (activeMinigame !== 'minesweep_ai' || mineGameOver || minigameEndSummary) return
    const dim = MINESWEEP_DIMS[mineDifficulty]
    const minesPlaced = mineGrid ?? placeMines(dim.rows, dim.cols, dim.mines, r, c)
    if (!mineGrid) setMineGrid(minesPlaced)
    const baseField = mineField.length ? mineField : createEmptyField(dim.rows, dim.cols)
    const { field: nextField, hitMine } = revealCell(baseField, minesPlaced, r, c)
    if (hitMine) {
      const lossField = exposeAllMinesAfterLoss(nextField, minesPlaced, r, c)
      setMineField(lossField)
      setMineGameOver(true)
      await finishMineOutcome(0)
      return
    }
    setMineField(nextField)
    if (isWinningBoard(nextField, minesPlaced)) await finishMineOutcome(2)
  }

  const mineCellFlag = (e: ReactMouseEvent, r: number, c: number) => {
    e.preventDefault()
    if (activeMinigame !== 'minesweep_ai' || mineGameOver || minigameEndSummary || mineGrid === null) return
    setMineField((f) => toggleFlag(f, r, c))
  }

  const startCheckers = async () => {
    const dashNow = await refresh()
    const pet = dashNow?.pet ?? dashboard.pet
    const cost = energyCostFor('checkers_ai')
    if (!assertEnergy(cost, pet.energy)) return
    try {
      setMinigameEndSummary(null)
      await apiJson('/api/minigames/checkers_ai/start-simple', { method: 'POST' })
      setCheckersState(createInitialCheckersState())
      setCheckersPick(null)
      checkersRoundEndedRef.current = false
      setActiveMinigame('checkers_ai')
      setMinigameOpen(true)
      await refresh()
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start Checkers'
      if (/energy/i.test(msg)) window.alert(`Checkers: ${msg}`)
      setMessage(msg)
    }
  }

  const finishCheckersOutcome = async (score: number, title: string) => {
    if (checkersRoundEndedRef.current) return
    checkersRoundEndedRef.current = true
    const result = await apiJson('/api/minigames/checkers_ai/finish-simple', {
      method: 'POST',
      body: JSON.stringify({ score, difficulty: checkersDifficulty }),
    })
    const dash = await refresh()
    if (dash) {
      setMinigameEndSummary({
        title,
        coinsEarned: result.coinsReward ?? 0,
        happinessDeltaPercent: result.happinessDeltaPercent ?? 0,
        hunger: Math.round(dash.pet.hunger),
        happiness: Math.round(dash.pet.happiness),
        energy: Math.round(dash.pet.energy),
        detailLines: [
          `Difficulty: ${checkersDifficulty}`,
          result.coinsBaseBeforeMultiplier != null && result.coinsBaseBeforeMultiplier !== result.coinsReward
            ? `Base coins: ${result.coinsBaseBeforeMultiplier} × ${result.coinMultiplierApplied}`
            : undefined,
        ].filter(Boolean) as string[],
      })
    }
  }

  const runCheckersAiSequence = async (startSt: CheckersState) => {
    let cur = startSt
    while (cur.turn === 'ai' && !checkersRoundEndedRef.current) {
      await delay(AI_MOVE_DELAY_MS)
      const mv = checkersPickAiMove(cur, checkersDifficulty)
      if (!mv) {
        const w = checkersWinner(cur)
        if (w === 'human') await finishCheckersOutcome(2, 'Checkers — you win')
        else if (w === 'ai') await finishCheckersOutcome(0, 'Checkers — AI wins')
        return
      }
      cur = advanceCheckersState(cur, mv)
      setCheckersState(cur)
      const w = checkersWinner(cur)
      if (w === 'ai') {
        await finishCheckersOutcome(0, 'Checkers — AI wins')
        return
      }
      if (w === 'human') {
        await finishCheckersOutcome(2, 'Checkers — you win')
        return
      }
    }
  }

  const applyHumanHopCheckers = (st: CheckersState, chosen: CheckersMove) => {
    const next = advanceCheckersState(st, chosen)
    setCheckersState(next)
    setCheckersPick(null)
    const win = checkersWinner(next)
    if (win === 'human') {
      void finishCheckersOutcome(2, 'Checkers — you win')
      return
    }
    if (win === 'ai') {
      void finishCheckersOutcome(0, 'Checkers — AI wins')
      return
    }
    if (next.turn === 'human') return
    void runCheckersAiSequence(next)
  }

  const tryHumanCheckers = (r: number, c: number) => {
    if (activeMinigame !== 'checkers_ai' || minigameEndSummary) return
    let st = repairJumpContinuation(checkersState)
    if (st !== checkersState) setCheckersState(st)
    if (st.turn === 'ai' && !checkersRoundEndedRef.current) {
      void runCheckersAiSequence(st)
      return
    }
    if (st.turn !== 'human') return

    const moves = checkersGenerateMoves(st)

    if (st.jumpContinuation) {
      const { r: jr, c: jc } = st.jumpContinuation
      const chosen = moves.find((mv) => mv.tr === r && mv.tc === c && mv.fr === jr && mv.fc === jc)
      if (!chosen) return
      applyHumanHopCheckers(st, chosen)
      return
    }

    const legalFrom = new Set(moves.map((m) => `${m.fr},${m.fc}`))
    if (checkersPick === null) {
      if (legalFrom.has(`${r},${c}`)) setCheckersPick({ r, c })
      return
    }
    const from = checkersPick
    const chosen = moves.find((mv) => mv.fr === from.r && mv.fc === from.c && mv.tr === r && mv.tc === c)
    if (!chosen) {
      setCheckersPick(legalFrom.has(`${r},${c}`) ? { r, c } : null)
      return
    }
    applyHumanHopCheckers(st, chosen)
  }

  /** Next lines populate the Minigames tab with live payout numbers from the dashboard (includes consumable coin multiplier). */
  const rewardCaptionFor = (g: MinigameInfo): string[] => {
    const rp = rewardPreview
    if (!rp) return []
    const m = rp.coinMultiplier
    if (g.code === 'higher_lower') {
      const h = rp.higherLower
      const lines = [
        `Quit or lose with your current streak (${h.streak}): ${h.coinsIfFinishNow} coins (cap ${h.fibonacciCap}; ×${m} multiplier already applied).`,
      ]
      if (h.hasActiveSession) lines.push('You have an active session — payouts reflect this account’s streak.')
      return lines
    }
    if (g.code === 'puzzle_swap') {
      const by = rp.puzzle_swap?.coinsBySampleScore ?? {}
      const parts = Object.entries(by).map(([k, coins]) => `${k.replace('score_', 'score ')} → ${coins} coins`)
      return [`Sample payouts (your ×${m} multiplier):`, ...parts]
    }
    const o = rp.connect4_ai?.coinsByOutcome
    if (g.code === 'connect4_ai' && o) {
      return [`Win: ${o.win} coins • Draw: ${o.draw} • Loss: ${o.loss} (×${m} multiplier applied).`]
    }
    const ms = rp.minesweep_ai?.coinsByOutcome
    if (g.code === 'minesweep_ai' && ms) {
      return [`Win: ${ms.win} coins • Loss: ${ms.loss} (×${m} multiplier applied).`]
    }
    const ck = rp.checkers_ai?.coinsByOutcome
    if (g.code === 'checkers_ai' && ck) {
      return [`Win: ${ck.win} coins • Draw: ${ck.draw} • Loss: ${ck.loss} (×${m} multiplier applied).`]
    }
    return []
  }

  const playAgainMinigame = async () => {
    setMinigameEndSummary(null)
    if (activeMinigame === 'higher_lower') await startGame()
    else if (activeMinigame === 'puzzle_swap') await startPuzzle()
    else if (activeMinigame === 'connect4_ai') await startConnect4()
    else if (activeMinigame === 'minesweep_ai') await startMineSweep()
    else if (activeMinigame === 'checkers_ai') await startCheckers()
  }

  const closeMinigameFully = async () => {
    setMinigameEndSummary(null)
    setMinigameOpen(false)
    setActiveMinigame(null)
    setCurrentNumber(null)
    setStreak(0)
    setConnectHumanMoves(0)
    setMineField([])
    setMineGrid(null)
    setMineGameOver(false)
    setCheckersState(createInitialCheckersState())
    setCheckersPick(null)
    await refresh()
  }

  dismissMinigameRef.current = { closeMinigameFully, quitGame, minigameEndSummary, activeMinigame }

  const onMinigameBackdropMouseDown = () => {
    const d = dismissMinigameRef.current
    if (d.minigameEndSummary) {
      void d.closeMinigameFully()
      return
    }
    const msg = d.activeMinigame === 'higher_lower'
      ? 'Quit Higher/Lower and claim coins for your current streak?'
      : 'Leave this minigame? Half the energy cost will be refunded; no coins or happiness change.'
    if (window.confirm(msg)) void d.quitGame()
  }

  const devGrantCoins = async () => {
    try {
      await apiJson('/api/dev/grant-coins', { method: 'POST', body: JSON.stringify({ amount: 1000 }) })
      await refresh()
      setMessage('Developer: coins granted.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Dev action failed')
    }
  }

  const devRefillStats = async () => {
    try {
      await apiJson('/api/dev/refill-stats', { method: 'POST' })
      await refresh()
      setMessage('Developer: stats refilled.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Dev action failed')
    }
  }

  const devApplyStats = async () => {
    const parsePct = (s: string) => {
      const n = Number(String(s).replace(',', '.').trim())
      if (!Number.isFinite(n) || n < 0 || n > 100) throw new Error('Each stat must be 0–100')
      return n / 100
    }
    try {
      await apiJson('/api/dev/set-stats', {
        method: 'POST',
        body: JSON.stringify({
          hungerPercent: parsePct(devH),
          happinessPercent: parsePct(devHa),
          energyPercent: parsePct(devE),
        }),
      })
      setDevStatsOpen(false)
      await refresh()
      setMessage('Developer: stats updated.')
    } catch (e) {
      window.alert(e instanceof Error ? e.message : 'Invalid percentages')
    }
  }

  const itemNameMap = Object.fromEntries(shopItems.map((item) => [item.code, item.name]))
  const consumableShopItems = shopItems.filter((item) => item.type === 'CONSUMABLE' || (item.shopSection || '') === 'CONSUMABLES')
  const cosmeticShopItems = shopItems.filter((item) => item.type === 'COSMETIC' || (item.shopSection || '') === 'COSMETICS')

  return (
    <div className="game">
      <div className="topbar">
        <span>Coins: {dashboard.wallet.coins}</span>
        {dashboard.privileged && (
          <details className="dev-dropdown">
            <summary>Developer</summary>
            <div className="dev-dropdown-body">
              <button type="button" onClick={devGrantCoins}>+1000 coins</button>
              <button type="button" onClick={devRefillStats}>Refill hunger / happiness / energy</button>
              <button type="button" onClick={() => setDevStatsOpen(true)}>Set stats (%)…</button>
              <p><small>Unlocked via your account flag or APP_PRIVILEGED_EMAILS.</small></p>
            </div>
          </details>
        )}
        {!dashboard.privileged && (
          <Link className="dev-hint" to="/app/settings#developer-tools">How to enable Developer</Link>
        )}
        {[
          { label: 'Hunger', value: Math.round(dashboard.pet.hunger), cls: 'bar-hunger' },
          { label: 'Happiness', value: Math.round(dashboard.pet.happiness), cls: 'bar-happiness' },
          { label: 'Energy', value: Math.round(dashboard.pet.energy), cls: 'bar-energy' },
        ].map((s) => (
          <div key={s.label} className="stat">
            <small>{s.label}: {s.value}</small>
            <div className="bar"><div className={`fill ${s.cls}`} style={{ width: `${s.value}%` }} /></div>
          </div>
        ))}
      </div>
      <div className="pet-stage card">
        <h3>{speciesCode === 'cat' ? 'Cat' : 'Dog'} — {MOOD_LABELS[activeMood]}</h3>
        {moodImagePath ? (
          <div
            className="pet-stage-layers"
            style={backgroundLayerUrl ? { backgroundImage: `url(${backgroundLayerUrl})` } : undefined}
          >
            <img src={moodImagePath} alt={`${speciesCode} ${activeMood}`} className="pet-stage-image" />
            {foregroundLayerUrl ? (
              <img src={foregroundLayerUrl} alt="" className="pet-stage-fg" aria-hidden />
            ) : null}
          </div>
        ) : (
          <p className="muted">No image found for current mood/species.</p>
        )}
      </div>
      <div className="nav">{nav.map(([path, label]) => <Link key={path} to={path} className={locationPath === path ? 'active' : ''}>{label}</Link>)}</div>
      <Routes>
        <Route path="shop" element={<div className="card pane">
          <h3>Consumables</h3>
          {consumableShopItems.map((item) => (
            <div key={item.code} className="row shop-row">
              <div><b>{item.name}</b> ({item.priceCoins})<br />{item.description}</div>
              <div className="shop-row-actions">
                <button type="button" onClick={() => buy(item.code)}>Buy</button>
              </div>
            </div>
          ))}
          <h3 style={{ marginTop: '1.25rem' }}>Cosmetics</h3>
          <p className="muted">Backgrounds, foreground overlays, and future alternate pet looks. One-time purchase; equip in Customize.</p>
          {cosmeticShopItems.length === 0 ? <p className="muted">No cosmetics in catalog.</p> : cosmeticShopItems.map((item) => (
            <div key={item.code} className="row shop-row">
              <div><b>{item.name}</b> ({item.priceCoins})<br />{item.description}</div>
              <div className="shop-row-actions">
                <button type="button" onClick={() => buy(item.code)}>Buy</button>
              </div>
            </div>
          ))}
        </div>} />
        <Route path="inventory" element={<div className="card pane">{inv.length === 0 ? 'No consumables.' : inv.map((i) => <details key={i.itemCode} className="inv-item"><summary>{itemNameMap[i.itemCode] || i.itemCode} x{i.quantity}</summary><p>{shopItems.find((s) => s.code === i.itemCode)?.description || 'No description'}</p><button onClick={() => useItem(i.itemCode)}>Use</button></details>)}</div>} />
        <Route path="customize" element={<div className="card pane">
          <h3>Pet customization</h3>
          <p className="muted">One species on stage at a time. Pick default or owned mood art; set scene background and foreground (starters and purchased items only).</p>
          <div className="subnav">
            <button type="button" className={speciesCode === 'dog' ? 'tab active' : 'tab'} onClick={() => void setSpecies('dog')}>Dog</button>
            <button type="button" className={speciesCode === 'cat' ? 'tab active' : 'tab'} onClick={() => void setSpecies('cat')}>Cat</button>
          </div>
          <h4 style={{ marginTop: '12px' }}>Scene</h4>
          <label className="dev-stat-label">
            Background
            <select
              value={equippedBg || 'none'}
              onChange={(e) => void equipVisualLayers(e.target.value, equippedFg || 'none')}
            >
              <option value="none">Default (gradient)</option>
              {visualCatalog.filter((a) => a.assetType === 'BACKGROUND' && (a.starter || ownedVisuals.has(a.code))).map((a) => (
                <option key={a.code} value={a.code}>{a.label}</option>
              ))}
            </select>
          </label>
          <label className="dev-stat-label">
            Foreground overlay
            <select
              value={equippedFg || 'none'}
              onChange={(e) => void equipVisualLayers(equippedBg || 'none', e.target.value)}
            >
              <option value="none">None</option>
              {visualCatalog.filter((a) => a.assetType === 'FOREGROUND' && (a.starter || ownedVisuals.has(a.code))).map((a) => (
                <option key={a.code} value={a.code}>{a.label}</option>
              ))}
            </select>
          </label>
          <h4 style={{ marginTop: '16px' }}>Mood images ({speciesCode})</h4>
          {MOOD_SLOT_ORDER.map((mood) => {
            const options = moodAssetsForSpecies.filter((a) => a.moodCode === mood && (a.starter || ownedVisuals.has(a.code)))
            const selected = moodSlots[mood] || 'none'
            return (
              <label key={mood} className="dev-stat-label">
                {MOOD_LABELS[mood]} image
                <select value={selected} onChange={(e) => void setMoodAsset(mood, e.target.value)}>
                  <option value="none">Default for this mood</option>
                  {options.map((o) => <option key={o.code} value={o.code}>{o.label}</option>)}
                </select>
              </label>
            )
          })}
        </div>} />
        <Route path="minigames" element={<div className="card pane">
          {minigames.map((g) => <details key={g.code} className="inv-item">
            <summary>{g.name}</summary>
            <p>{g.description}</p>
            <p><strong>Energy cost:</strong> {energyCostFor(g.code)}</p>
            {!rewardPreview && (
              <p className="muted">
                Live payout preview unavailable. Use a current backend (see README “Verify local stack”): from repo root run{' '}
                <code>start-all.ps1</code> or restart <code>backend</code> with <code>mvn spring-boot:run</code> after <code>git pull</code>.
              </p>
            )}
            {rewardCaptionFor(g).map((line, i) => (
              <p key={i}>{i === 0 ? <><strong>Payout preview:</strong> {line}</> : line}</p>
            ))}
            {g.code === 'higher_lower' && <button onClick={startGame}>Start</button>}
            {g.code === 'puzzle_swap' && <div className="subnav"><select value={String(puzzleSize)} onChange={(e) => setPuzzleSize(Number(e.target.value))}>{[3, 5, 7, 9, 11].map((n) => <option key={n} value={n}>{n}x{n}</option>)}</select><button onClick={startPuzzle}>Start</button></div>}
            {g.code === 'connect4_ai' && <div className="subnav"><select value={connectDifficulty} onChange={(e) => setConnectDifficulty(e.target.value as 'easy' | 'medium' | 'hard')}><option value="easy">Easy (random)</option><option value="medium">Medium (minimax d1)</option><option value="hard">Hard (minimax d3)</option></select><button onClick={startConnect4}>Start</button></div>}
            {g.code === 'minesweep_ai' && <div className="subnav"><select value={mineDifficulty} onChange={(e) => setMineDifficulty(e.target.value as MineDifficulty)}><option value="easy">Beginner 9×9 (10 mines)</option><option value="medium">Intermediate 16×16 (40)</option><option value="hard">Expert 16×30 (99)</option></select><button type="button" onClick={() => void startMineSweep()}>Start</button></div>}
            {g.code === 'checkers_ai' && <div className="subnav"><select value={checkersDifficulty} onChange={(e) => setCheckersDifficulty(e.target.value as CheckersDifficulty)}><option value="easy">Easy (random)</option><option value="medium">Medium (minimax depth 1)</option><option value="hard">Hard (minimax depth 3)</option></select><button type="button" onClick={() => void startCheckers()}>Start</button></div>}
          </details>)}
          {minigameOpen && (
            <div
              className="modal-backdrop"
              role="presentation"
              onMouseDown={(e) => {
                if (e.target === e.currentTarget) onMinigameBackdropMouseDown()
              }}
            >
            <div className="modal modal-minigame" onMouseDown={(e) => e.stopPropagation()}>
            <div className="modal-minigame-scroll-all">
              {minigameEndSummary && (
                <div className="minigame-result-sheet">
                  <h3>{minigameEndSummary.title}</h3>
                  <p><strong>Coins earned:</strong> {minigameEndSummary.coinsEarned}</p>
                  <p><strong>Happiness adjustment:</strong> {minigameEndSummary.happinessDeltaPercent > 0 ? '+' : ''}{minigameEndSummary.happinessDeltaPercent}%</p>
                  <p><strong>Pet state now:</strong> Hunger {minigameEndSummary.hunger}%, Happiness {minigameEndSummary.happiness}%, Energy {minigameEndSummary.energy}%</p>
                  {minigameEndSummary.hlSnap && (
                    <div className="snapshot-block">
                      <h4>Last round</h4>
                      <p>Previous card: {minigameEndSummary.hlSnap.previous} → Next: {minigameEndSummary.hlSnap.next} (streak credited: {minigameEndSummary.hlSnap.streak})</p>
                    </div>
                  )}
                  {minigameEndSummary.connect4Board && (
                    <div className="snapshot-block">
                      <h4>Final board</h4>
                      <div className="connect-grid readonly">
                        {minigameEndSummary.connect4Board.map((row, r) => row.map((cell, c) => (
                          <div key={`${r}-${c}`} className={`c4 ${cell === 1 ? 'p1' : cell === 2 ? 'p2' : ''}`} />
                        )))}
                      </div>
                    </div>
                  )}
                  {minigameEndSummary.puzzleSnap && (
                    <div className="snapshot-block">
                      <h4>Final picture</h4>
                      <div className="puzzle-snapshot-wrap">
                      <div
                        className="puzzle-grid puzzle-snapshot"
                        style={{ gridTemplateColumns: `repeat(${minigameEndSummary.puzzleSnap.size}, 1fr)` }}
                      >
                        {minigameEndSummary.puzzleSnap.tiles.map((tile, idx) => {
                          const sz = minigameEndSummary.puzzleSnap!.size
                          const denom = Math.max(1, sz - 1)
                          const x = tile % sz
                          const y = Math.floor(tile / sz)
                          return (
                            <div
                              key={idx}
                              className="tile"
                              style={{
                                backgroundImage: `url(${minigameEndSummary.puzzleSnap!.imageUrl})`,
                                backgroundSize: `${sz * 100}% ${sz * 100}%`,
                                backgroundPosition: `${(x / denom) * 100}% ${(y / denom) * 100}%`,
                              }}
                            />
                          )
                        })}
                      </div>
                      </div>
                    </div>
                  )}
                  {minigameEndSummary.detailLines?.map((l) => <p key={l} className="muted">{l}</p>)}
                  <div className="result-actions">
                    <button type="button" onClick={() => playAgainMinigame()}>Play again</button>
                    <button type="button" className="danger" onClick={() => closeMinigameFully()}>Close</button>
                  </div>
                </div>
              )}
              {!minigameEndSummary && activeMinigame === 'higher_lower' && <>
                <h3>Higher / Lower</h3>
                <p>Current number: {currentNumber} | Streak: {streak}</p>
                <div>
                  <button type="button" onClick={() => guess('HIGHER')}>Higher</button>
                  <button type="button" onClick={() => guess('LOWER')}>Lower</button>
                </div>
              </>}
              {!minigameEndSummary && activeMinigame === 'puzzle_swap' && (
                <div className="puzzle-modal-block">
                  <h3>Puzzle Swap ({puzzleSize}x{puzzleSize})</h3>
                  <p>Moves: {puzzleMoves}</p>
                  <div className="puzzle-play-wrap" style={{ ['--puzzle-n' as string]: String(puzzleSize) }}>
                    <div className="puzzle-grid puzzle-grid-modal" style={{ gridTemplateColumns: `repeat(${puzzleSize}, 1fr)` }}>
                      {puzzleBoard.map((tile, idx) => {
                        const x = tile % puzzleSize
                        const y = Math.floor(tile / puzzleSize)
                        const denom = Math.max(1, puzzleSize - 1)
                        return (
                          <button
                            key={idx}
                            type="button"
                            className={`tile ${puzzleFirst === idx ? 'selected' : ''}`}
                            onClick={() => puzzleSwap(idx)}
                            style={{
                              backgroundImage: `url(${puzzleImage})`,
                              backgroundSize: `${puzzleSize * 100}% ${puzzleSize * 100}%`,
                              backgroundPosition: `${(x / denom) * 100}% ${(y / denom) * 100}%`,
                            }}
                          />
                        )
                      })}
                    </div>
                  </div>
                </div>
              )}
              {!minigameEndSummary && activeMinigame === 'minesweep_ai' && (() => {
                const dim = MINESWEEP_DIMS[mineDifficulty]
                const cols = mineField[0]?.length || dim.cols
                return (
                  <div className="mines-modal-block">
                    <h3>Minesweeper</h3>
                    <p className="muted">Left click reveal · Right click flag · First reveal places mines (safe cell)</p>
                    <div
                      className="mine-grid"
                      style={{
                        ['--mine-cell' as string]: cols >= 24 ? '20px' : cols >= 16 ? '22px' : '24px',
                        gridTemplateColumns: `repeat(${cols}, var(--mine-cell, 22px))`,
                      }}
                    >
                      {mineField.flatMap((row, r) =>
                        row.map((v, c) => {
                          let label = ''
                          if (v === -1) label = ''
                          else if (v === -2) label = '⚑'
                          else if (v === 9) label = '💥'
                          else if (v === 10) label = '💣'
                          else if (v > 0) label = String(v)
                          else label = ''
                          const revealed = (v >= 0 && v <= 8) || v === 9 || v === 10
                          const numCls =
                            v >= 1 && v <= 8 ? ` mine-num mine-num-${v}` : v === 9 ? ' mine-boom' : v === 10 ? ' mine-dormant' : ''
                          return (
                            <button
                              key={`${r}-${c}`}
                              type="button"
                              className={`mine-cell revealed-${revealed ? 'open' : 'closed'}${numCls}`}
                              onClick={() => void mineCellClick(r, c)}
                              onContextMenu={(e) => mineCellFlag(e, r, c)}
                            >
                              {label}
                            </button>
                          )
                        }),
                      )}
                    </div>
                  </div>
                )
              })()}
              {!minigameEndSummary && activeMinigame === 'checkers_ai' && (
                <div className="checkers-modal-block">
                  <h3>Checkers</h3>
                  <p className="muted">
                    American checkers on dark squares only. Men move diagonally forward (including captures). When a man
                    reaches the far home row it promotes to a <strong>king</strong> (◉ / ◎): kings move and capture
                    diagonally in <strong>all four directions</strong> (not “flying kings” — one step at a time, same as
                    men). If any capture is possible you must jump; multi-jumps use the same piece until finished.
                  </p>
                  <p>
                    {checkersState.turn === 'human'
                      ? (checkersState.jumpContinuation
                          ? 'Continue jumping with the highlighted piece until no more captures from it.'
                          : 'Your move — if any capture is possible you must jump (pick any legal capturing piece/path).')
                      : 'AI thinking…'}
                  </p>
                  <CheckersBoardView
                    state={checkersState}
                    pick={checkersPick}
                    disabled={Boolean(minigameEndSummary)}
                    onCell={tryHumanCheckers}
                  />
                </div>
              )}
              {!minigameEndSummary && activeMinigame === 'connect4_ai' && <>
                <h3>Connect 4</h3>
                <p>{connectStatus}</p>
                <div className="connect-grid">
                  {connectBoard.map((row, r) => row.map((cell, c) => (
                    <button key={`${r}-${c}`} type="button" className={`c4 ${cell === 1 ? 'p1' : cell === 2 ? 'p2' : ''}`} onClick={() => playConnect(c)} />
                  )))}
                </div>
              </>}
              {!minigameEndSummary && <button type="button" className="danger" onClick={quitGame}>Close</button>}
            </div>
            </div>
            </div>
          )}
        </div>} />
        <Route path="settings" element={<div className="card">
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
        </div>} />
      </Routes>
      {devStatsOpen && (
        <div className="modal-backdrop" onClick={() => setDevStatsOpen(false)} role="presentation">
          <div className="modal card dev-stats-panel" onClick={(e) => e.stopPropagation()}>
            <h3>Set pet stats (%)</h3>
            <label className="dev-stat-label">Hunger <input value={devH} onChange={(e) => setDevH(e.target.value)} /></label>
            <label className="dev-stat-label">Happiness <input value={devHa} onChange={(e) => setDevHa(e.target.value)} /></label>
            <label className="dev-stat-label">Energy <input value={devE} onChange={(e) => setDevE(e.target.value)} /></label>
            <div className="result-actions">
              <button type="button" onClick={devApplyStats}>Apply</button>
              <button type="button" className="danger" onClick={() => setDevStatsOpen(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
