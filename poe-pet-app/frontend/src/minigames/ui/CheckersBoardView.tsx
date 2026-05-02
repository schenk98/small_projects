import type { CheckersState } from '../checkersAi'

type Props = {
  state: CheckersState
  pick: { r: number; c: number } | null
  disabled: boolean
  onCell: (r: number, c: number) => void
}

export function CheckersBoardView({ state, pick, disabled, onCell }: Props) {
  const { board, jumpContinuation, turn } = state
  return (
    <div className="checkers-board">
      {board.map((row, r) => (
        <div key={r} className="checkers-row">
          {row.map((cell, c) => {
            const dark = (r + c) % 2 === 1
            const cont = jumpContinuation && jumpContinuation.r === r && jumpContinuation.c === c
            const sel = (pick?.r === r && pick?.c === c) || cont
            let mark = ''
            if (cell === 1) mark = '●'
            else if (cell === 2) mark = '◉'
            else if (cell === 3) mark = '○'
            else if (cell === 4) mark = '◎'
            return (
              <button
                key={`${r}-${c}`}
                type="button"
                className={`checkers-cell ${dark ? 'dark' : 'light'}${sel ? ' picked' : ''}`}
                disabled={!dark || disabled || turn !== 'human'}
                onClick={() => onCell(r, c)}
              >
                {mark}
              </button>
            )
          })}
        </div>
      ))}
    </div>
  )
}
