import type { MinigameEndSummary } from '../../../minigames/types'

export type ActiveMinigameCode =
  | 'higher_lower'
  | 'puzzle_swap'
  | 'connect4_ai'
  | 'minesweep_ai'
  | 'checkers_ai'

export type MinigameModalState = {
  open: boolean
  active: ActiveMinigameCode | null
  endSummary: MinigameEndSummary | null
}

