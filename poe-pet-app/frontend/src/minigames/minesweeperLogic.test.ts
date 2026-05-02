import { describe, expect, it } from 'vitest'
import {
  countHiddenSafe,
  createEmptyField,
  exposeAllMinesAfterLoss,
  MINESWEEP_DIMS,
  placeMines,
  revealCell,
} from './minesweeperLogic'

describe('placeMines', () => {
  it('never places a mine on the safe first cell', () => {
    for (let i = 0; i < 40; i++) {
      const mines = placeMines(9, 9, 10, 4, 4)
      expect(mines[4]![4]).toBe(false)
      expect(mines.flat().filter(Boolean).length).toBe(10)
    }
  })
})

describe('revealCell', () => {
  it('reveals zeros with flood fill', () => {
    const mines = [
      [false, false, false],
      [false, false, false],
      [false, false, true],
    ]
    const field = createEmptyField(3, 3)
    const { field: f2, hitMine } = revealCell(field, mines, 0, 0)
    expect(hitMine).toBe(false)
    expect(f2[0]![0]).toBe(0)
    expect(countHiddenSafe(f2, mines)).toBe(0)
  })
})

describe('countHiddenSafe', () => {
  it('counts unrevealed non-mine cells', () => {
    const mines = createEmptyField(2, 2).map(() => [false, false])
    const field = createEmptyField(2, 2)
    expect(countHiddenSafe(field, mines)).toBe(4)
  })
})

describe('MINESWEEP_DIMS', () => {
  it('has sane mine counts vs area', () => {
    for (const k of ['easy', 'medium', 'hard'] as const) {
      const { rows, cols, mines } = MINESWEEP_DIMS[k]
      expect(rows * cols - 1).toBeGreaterThanOrEqual(mines)
    }
  })
})

describe('exposeAllMinesAfterLoss', () => {
  it('marks detonation as 9, other mines as 10, and clears wrong flags to clue counts', () => {
    const mines = [
      [true, false],
      [false, true],
    ]
    const field = [
      [-1, -1],
      [-2, -1],
    ]
    const out = exposeAllMinesAfterLoss(field, mines, 0, 0)
    expect(out[0]![0]).toBe(9)
    expect(out[1]![1]).toBe(10)
    expect(out[1]![0]).toBe(2)
  })
})
