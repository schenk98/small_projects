export type ConnectDifficulty = 'easy' | 'medium' | 'hard'

export type ConnectCell = 0 | 1 | 2
export type ConnectWinner = 0 | 1 | 2 | 3 // 3 = draw

/**
 * Pure Connect-4 helpers used by the UI and by tests.
 *
 * Intentional: no React, no API calls. This keeps game logic testable and reusable.
 */
export function createEmptyConnectBoard(): ConnectCell[][] {
  return Array.from({ length: 6 }, () => Array<ConnectCell>(7).fill(0))
}

export function dropInColumn(board: ConnectCell[][], col: number, player: 1 | 2): ConnectCell[][] | null {
  const next = board.map((r) => [...r]) as ConnectCell[][]
  for (let row = 5; row >= 0; row--) {
    if (next[row][col] === 0) {
      next[row][col] = player
      return next
    }
  }
  return null
}

export function checkWinner(board: ConnectCell[][]): ConnectWinner {
  const dirs: [number, number][] = [[1, 0], [0, 1], [1, 1], [1, -1]]
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
        if (ok) return board[r][c] as 1 | 2
      }
    }
  }
  return board.flat().every((v) => v !== 0) ? 3 : 0
}

function scoreWindow(windowCells: ConnectCell[]) {
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

function evalBoard(board: ConnectCell[][]) {
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

function minimax(
  board: ConnectCell[][],
  depth: number,
  alpha: number,
  beta: number,
  maximizing: boolean,
): { score: number; col: number } {
  // Alpha-beta pruned minimax on a small fixed board.
  // Depth is intentionally shallow (1 or 3) to keep the UI responsive.
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

export function pickAiMove(board: ConnectCell[][], difficulty: ConnectDifficulty): number {
  const valid = Array.from({ length: 7 }, (_, c) => c).filter((c) => board[0][c] === 0)
  if (valid.length === 0) return -1
  if (difficulty === 'easy') return valid[Math.floor(Math.random() * valid.length)]
  const depth = difficulty === 'hard' ? 3 : 1
  return minimax(board, depth, -Infinity, Infinity, true).col
}

