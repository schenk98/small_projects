import type { CheckersState } from '../../../minigames/checkersAi'
import { CheckersBoardView } from '../../../minigames/ui/CheckersBoardView'

export function CheckersModal({
  state,
  pick,
  disabled,
  onCell,
}: {
  state: CheckersState
  pick: { r: number; c: number } | null
  disabled: boolean
  onCell: (r: number, c: number) => void
}) {
  return (
    <div className="checkers-modal-block">
      <h3>Checkers</h3>
      <p className="muted">
        American checkers on dark squares only. Men move diagonally forward (including captures). When a man reaches the
        far home row it promotes to a <strong>king</strong> (◉ / ◎): kings move and capture diagonally in{' '}
        <strong>all four directions</strong> (not “flying kings” — one step at a time, same as men). If any capture is
        possible you must jump; multi-jumps use the same piece until finished.
      </p>
      <p>
        {state.turn === 'human'
          ? (state.jumpContinuation
            ? 'Continue jumping with the highlighted piece until no more captures from it.'
            : 'Your move — if any capture is possible you must jump (pick any legal capturing piece/path).')
          : 'AI thinking…'}
      </p>
      <CheckersBoardView
        state={state}
        pick={pick}
        disabled={disabled}
        onCell={onCell}
      />
    </div>
  )
}

