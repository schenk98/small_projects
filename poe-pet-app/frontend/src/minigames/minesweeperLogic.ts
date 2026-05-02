/**
 * Cell values:
 * - `-1` unrevealed safe, `-2` flagged (safe or mine unknown to player)
 * - `0`–`8` revealed clue count
 * - `9` exploded mine (loss)
 * - `10` revealed unhit mine after loss (classic “show all mines”)
 */

export type MineDifficulty = 'easy' | 'medium' | 'hard'

/** Classic Windows Minesweeper: beginner / intermediate / expert */
export const MINESWEEP_DIMS: Record<MineDifficulty, { rows: number; cols: number; mines: number }> = {
  easy: { rows: 9, cols: 9, mines: 10 },
  medium: { rows: 16, cols: 16, mines: 40 },
  hard: { rows: 16, cols: 30, mines: 99 },
}

export function createEmptyField(rows: number, cols: number): number[][] {
  return Array.from({ length: rows }, () => Array(cols).fill(-1))
}

/** Place `mineCount` mines on dark cells, never on (safeR, safeC). */
export function placeMines(rows: number, cols: number, mineCount: number, safeR: number, safeC: number): boolean[][] {
  const mines = Array.from({ length: rows }, () => Array(cols).fill(false))
  const candidates: [number, number][] = []
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (r === safeR && c === safeC) continue
      candidates.push([r, c])
    }
  }
  for (let i = candidates.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[candidates[i], candidates[j]] = [candidates[j], candidates[i]]
  }
  const n = Math.min(mineCount, candidates.length)
  for (let i = 0; i < n; i++) {
    const [r, c] = candidates[i]!
    mines[r]![c] = true
  }
  return mines
}

export function neighborMineCount(mines: boolean[][], r: number, c: number): number {
  let n = 0
  for (let dr = -1; dr <= 1; dr++) {
    for (let dc = -1; dc <= 1; dc++) {
      if (dr === 0 && dc === 0) continue
      const rr = r + dr
      const cc = c + dc
      if (rr >= 0 && rr < mines.length && cc >= 0 && cc < mines[0]!.length && mines[rr]![cc]) n++
    }
  }
  return n
}

export function revealCell(
  field: number[][],
  mines: boolean[][],
  r: number,
  c: number,
): { field: number[][]; hitMine: boolean } {
  const rows = field.length
  const cols = field[0]!.length
  const next = field.map((row) => [...row])
  if (next[r]![c] === -2) return { field: next, hitMine: false }
  if (next[r]![c] >= 0 && next[r]![c] <= 8) return { field: next, hitMine: false }
  if (mines[r]![c]) {
    next[r]![c] = 9
    return { field: next, hitMine: true }
  }
  const stack: [number, number][] = [[r, c]]
  const seen = new Set<string>()
  while (stack.length) {
    const [cr, cc] = stack.pop()!
    const key = `${cr},${cc}`
    if (seen.has(key)) continue
    seen.add(key)
    if (cr < 0 || cr >= rows || cc < 0 || cc >= cols) continue
    if (mines[cr]![cc]) continue
    const cnt = neighborMineCount(mines, cr, cc)
    next[cr]![cc] = cnt
    if (cnt === 0) {
      for (let dr = -1; dr <= 1; dr++) {
        for (let dc = -1; dc <= 1; dc++) {
          if (dr === 0 && dc === 0) continue
          stack.push([cr + dr, cc + dc])
        }
      }
    }
  }
  return { field: next, hitMine: false }
}

export function countHiddenSafe(field: number[][], mines: boolean[][]): number {
  let n = 0
  for (let r = 0; r < field.length; r++) {
    for (let c = 0; c < field[0]!.length; c++) {
      if (!mines[r]![c] && field[r]![c]! < 0) n++
    }
  }
  return n
}

/** Win when every safe cell shows a clue (0–8), not hidden or flagged. */
export function isWinningBoard(field: number[][], mines: boolean[][]): boolean {
  for (let r = 0; r < field.length; r++) {
    for (let c = 0; c < field[0]!.length; c++) {
      if (mines[r]![c]) continue
      const v = field[r]![c]!
      if (v < 0 || v > 8) return false
    }
  }
  return true
}

export function toggleFlag(field: number[][], r: number, c: number): number[][] {
  const v = field[r]![c]!
  if (v >= 0 && v <= 8) return field
  const next = field.map((row) => [...row])
  next[r]![c] = v === -2 ? -1 : -2
  return next
}

/**
 * After stepping on a mine: keep detonation at `9`, reveal every other mine as `10`,
 * and replace incorrect flags on safe cells with their clue counts (so the board matches classic Minesweeper).
 */
export function exposeAllMinesAfterLoss(
  field: number[][],
  mines: boolean[][],
  detR: number,
  detC: number,
): number[][] {
  const rows = field.length
  const cols = field[0]!.length
  const out = field.map((row) => [...row])
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (mines[r]![c]) {
        if (r === detR && c === detC) {
          out[r]![c] = 9
        } else if (out[r]![c]! < 0 || out[r]![c]! === -2) {
          out[r]![c] = 10
        }
      } else if (out[r]![c] === -2) {
        out[r]![c] = neighborMineCount(mines, r, c)
      }
    }
  }
  return out
}
