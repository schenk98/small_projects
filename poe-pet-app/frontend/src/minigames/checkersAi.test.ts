import { describe, expect, it } from 'vitest'
import {
  applyMove,
  advanceCheckersState,
  createInitialCheckersBoard,
  createInitialCheckersState,
  generateMoves,
  isHumanPiece,
  pieceCount,
  winnerFromMoves,
} from './checkersAi'

const freshHuman = () => createInitialCheckersState()

describe('checkersAi', () => {
  it('starts with 12 pieces per side on dark squares', () => {
    const b = createInitialCheckersBoard()
    expect(pieceCount(b, isHumanPiece)).toBe(12)
    expect(pieceCount(b, (p) => p === 3 || p === 4)).toBe(12)
  })

  it('human has legal moves from start', () => {
    const s = freshHuman()
    const moves = generateMoves(s)
    expect(moves.length).toBeGreaterThan(0)
  })

  it('applyMove moves piece and clears from square', () => {
    const s = freshHuman()
    const m = generateMoves(s)[0]!
    const nb = applyMove(s.board, m)
    expect(nb[m.fr]![m.fc]).toBe(0)
    expect(nb[m.tr]![m.tc]).toBeGreaterThan(0)
  })

  it('does not declare winner at start', () => {
    expect(winnerFromMoves(freshHuman())).toBeNull()
  })

  it('sets jump continuation after capture with further jumps', () => {
    const board = Array.from({ length: 8 }, () => Array(8).fill(0))
    board[5]![2] = 1
    board[4]![3] = 3
    board[2]![3] = 3
    const m = { fr: 5, fc: 2, tr: 3, tc: 4, capture: [4, 3] as [number, number] }
    const next = advanceCheckersState({ board, turn: 'human', jumpContinuation: null }, m)
    expect(next.jumpContinuation).toEqual({ r: 3, c: 4 })
    expect(next.turn).toBe('human')
  })
})
