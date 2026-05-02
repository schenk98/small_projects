/**
 * American-style checkers: 8×8, dark squares only. Men move/capture diagonally **forward**; **kings**
 * (after promotion on the far row) move/capture in **all four** diagonals, **one step** per move — not
 * “flying kings”. **Mandatory capture** when any jump exists; **any legal capture path** may be chosen.
 * **Multi-jump** continues with the same piece until no further captures from that square.
 */

export const EMPTY = 0
export const HUMAN_MAN = 1
export const HUMAN_KING = 2
export const AI_MAN = 3
export const AI_KING = 4

export type CheckersSide = 'human' | 'ai'
export type CheckersDifficulty = 'easy' | 'medium' | 'hard'

export type CheckersMove = { fr: number; fc: number; tr: number; tc: number; capture?: [number, number] }

export type CheckersState = {
  board: number[][]
  turn: CheckersSide
  /** Mid multi-jump: only this square may move again (must capture). */
  jumpContinuation: { r: number; c: number } | null
}

const D4 = [
  [-1, -1],
  [-1, 1],
  [1, -1],
  [1, 1],
] as const

export function isDark(r: number, c: number): boolean {
  return (r + c) % 2 === 1
}

export function createInitialCheckersState(): CheckersState {
  const board = Array.from({ length: 8 }, () => Array(8).fill(EMPTY))
  for (let r = 0; r < 3; r++) {
    for (let c = 0; c < 8; c++) {
      if (isDark(r, c)) board[r]![c] = AI_MAN
    }
  }
  for (let r = 5; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      if (isDark(r, c)) board[r]![c] = HUMAN_MAN
    }
  }
  return { board, turn: 'human', jumpContinuation: null }
}

export function createInitialCheckersBoard(): number[][] {
  return createInitialCheckersState().board
}

export function isHumanPiece(p: number): boolean {
  return p === HUMAN_MAN || p === HUMAN_KING
}

export function isAiPiece(p: number): boolean {
  return p === AI_MAN || p === AI_KING
}

function humanStepDirs(piece: number): readonly (readonly [number, number])[] {
  if (piece === HUMAN_KING) return D4
  return [
    [-1, -1],
    [-1, 1],
  ]
}

function aiStepDirs(piece: number): readonly (readonly [number, number])[] {
  if (piece === AI_KING) return D4
  return [
    [1, -1],
    [1, 1],
  ]
}

function pushSimple(board: number[][], r: number, c: number, piece: number, humanSide: boolean, out: CheckersMove[]) {
  const dirs = humanSide ? humanStepDirs(piece) : aiStepDirs(piece)
  for (const [dr, dc] of dirs) {
    const tr = r + dr
    const tc = c + dc
    if (tr < 0 || tr >= 8 || tc < 0 || tc >= 8) continue
    if (!isDark(tr, tc)) continue
    if (board[tr]![tc] !== EMPTY) continue
    out.push({ fr: r, fc: c, tr, tc })
  }
}

function pushCaptures(board: number[][], r: number, c: number, piece: number, humanSide: boolean, out: CheckersMove[]) {
  const dirs = humanSide ? humanStepDirs(piece) : aiStepDirs(piece)
  for (const [dr, dc] of dirs) {
    const mr = r + dr
    const mc = c + dc
    const tr = r + 2 * dr
    const tc = c + 2 * dc
    if (tr < 0 || tr >= 8 || tc < 0 || tc >= 8) continue
    if (!isDark(tr, tc)) continue
    const mid = board[mr]![mc]!
    const foe = humanSide ? isAiPiece(mid) : isHumanPiece(mid)
    if (!foe) continue
    if (board[tr]![tc] !== EMPTY) continue
    out.push({ fr: r, fc: c, tr, tc, capture: [mr, mc] })
  }
}

function listCapturesFrom(board: number[][], r: number, c: number, humanSide: boolean): CheckersMove[] {
  const piece = board[r]![c]!
  if (piece === EMPTY) return []
  if (humanSide && !isHumanPiece(piece)) return []
  if (!humanSide && !isAiPiece(piece)) return []
  const out: CheckersMove[] = []
  pushCaptures(board, r, c, piece, humanSide, out)
  return out
}

function hasAnyCaptureForSide(board: number[][], humanSide: boolean): boolean {
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const piece = board[r]![c]!
      if (humanSide && !isHumanPiece(piece)) continue
      if (!humanSide && !isAiPiece(piece)) continue
      if (listCapturesFrom(board, r, c, humanSide).length > 0) return true
    }
  }
  return false
}

export function applyMove(board: number[][], m: CheckersMove): number[][] {
  const next = board.map((row) => [...row])
  const piece = next[m.fr]![m.fc]!
  next[m.fr]![m.fc] = EMPTY
  let promoted = piece
  if (piece === HUMAN_MAN && m.tr === 0) promoted = HUMAN_KING
  if (piece === AI_MAN && m.tr === 7) promoted = AI_KING
  next[m.tr]![m.tc] = promoted
  if (m.capture) {
    const [cr, cc] = m.capture
    next[cr]![cc] = EMPTY
  }
  return next
}

function opposite(turn: CheckersSide): CheckersSide {
  return turn === 'human' ? 'ai' : 'human'
}

/** Apply one hop and update turn / continuation per rules. */
export function advanceCheckersState(state: CheckersState, m: CheckersMove): CheckersState {
  const humanSide = state.turn === 'human'
  const nb = applyMove(state.board, m)
  if (m.capture) {
    const more = listCapturesFrom(nb, m.tr, m.tc, humanSide)
    if (more.length > 0) {
      return { board: nb, turn: state.turn, jumpContinuation: { r: m.tr, c: m.tc } }
    }
  }
  return { board: nb, turn: opposite(state.turn), jumpContinuation: null }
}

export function generateMoves(state: CheckersState): CheckersMove[] {
  const { board, turn, jumpContinuation } = state
  const humanSide = turn === 'human'

  if (jumpContinuation) {
    const { r, c } = jumpContinuation
    return listCapturesFrom(board, r, c, humanSide)
  }

  if (hasAnyCaptureForSide(board, humanSide)) {
    const out: CheckersMove[] = []
    for (let r = 0; r < 8; r++) {
      for (let c = 0; c < 8; c++) {
        const piece = board[r]![c]!
        if (humanSide && !isHumanPiece(piece)) continue
        if (!humanSide && !isAiPiece(piece)) continue
        pushCaptures(board, r, c, piece, humanSide, out)
      }
    }
    return out
  }

  const simple: CheckersMove[] = []
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const piece = board[r]![c]!
      if (humanSide && !isHumanPiece(piece)) continue
      if (!humanSide && !isAiPiece(piece)) continue
      pushSimple(board, r, c, piece, humanSide, simple)
    }
  }
  return simple
}

/** If jump continuation is stuck (no legal hops), end the turn (should not happen with correct advance). */
export function repairJumpContinuation(state: CheckersState): CheckersState {
  if (!state.jumpContinuation) return state
  const { r, c } = state.jumpContinuation
  const humanSide = state.turn === 'human'
  if (listCapturesFrom(state.board, r, c, humanSide).length > 0) return state
  return { board: state.board, turn: opposite(state.turn), jumpContinuation: null }
}

export function pieceCount(board: number[][], pred: (v: number) => boolean): number {
  let n = 0
  for (const row of board) for (const v of row) if (pred(v)) n++
  return n
}

export function evaluateBoard(board: number[][]): number {
  let s = 0
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const p = board[r]![c]!
      if (p === AI_KING) s += 52
      else if (p === AI_MAN) s += 32 + (7 - r)
      else if (p === HUMAN_KING) s -= 52
      else if (p === HUMAN_MAN) s -= 32 + r
    }
  }
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const p = board[r]![c]!
      for (const [dr, dc] of D4) {
        const rr = r + dr
        const cc = c + dc
        if (rr < 0 || rr >= 8 || cc < 0 || cc >= 8) continue
        const o = board[rr]![cc]!
        if ((p === AI_MAN || p === AI_KING) && isHumanPiece(o)) s -= 2
        if ((p === HUMAN_MAN || p === HUMAN_KING) && isAiPiece(o)) s += 2
      }
    }
  }
  return s
}

function minimax(state: CheckersState, depth: number, alpha: number, beta: number): number {
  const moves = generateMoves(state)
  if (moves.length === 0) {
    return state.turn === 'ai' ? -100000 : 100000
  }
  if (depth <= 0) return evaluateBoard(state.board)

  const maxing = state.turn === 'ai'
  if (maxing) {
    let v = -Infinity
    for (const m of moves) {
      const next = advanceCheckersState(state, m)
      const sc = minimax(next, depth - 1, alpha, beta)
      v = Math.max(v, sc)
      alpha = Math.max(alpha, v)
      if (beta <= alpha) break
    }
    return v
  }
  let v = Infinity
  for (const m of moves) {
    const next = advanceCheckersState(state, m)
    const sc = minimax(next, depth - 1, alpha, beta)
    v = Math.min(v, sc)
    beta = Math.min(beta, v)
    if (beta <= alpha) break
  }
  return v
}

export function pickAiMove(state: CheckersState, difficulty: CheckersDifficulty): CheckersMove | null {
  const moves = generateMoves(state)
  if (moves.length === 0) return null
  if (difficulty === 'easy') return moves[Math.floor(Math.random() * moves.length)]!

  const depthBudget = difficulty === 'medium' ? 3 : 6
  let bestScore = -Infinity
  const tied: CheckersMove[] = []
  for (const m of moves) {
    const next = advanceCheckersState(state, m)
    const sc = minimax(next, depthBudget - 1, -Infinity, Infinity)
    if (sc > bestScore) {
      bestScore = sc
      tied.length = 0
      tied.push(m)
    } else if (sc === bestScore) {
      tied.push(m)
    }
  }
  return tied[Math.floor(Math.random() * tied.length)]!
}

export function winnerFromMoves(state: CheckersState): 'human' | 'ai' | null {
  const fixed = repairJumpContinuation(state)
  const moves = generateMoves(fixed)
  if (moves.length > 0) return null
  const hp = pieceCount(fixed.board, isHumanPiece)
  const ap = pieceCount(fixed.board, isAiPiece)
  if (hp === 0) return 'ai'
  if (ap === 0) return 'human'
  return fixed.turn === 'human' ? 'ai' : 'human'
}
