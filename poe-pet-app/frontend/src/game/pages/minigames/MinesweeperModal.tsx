import type { MouseEvent as ReactMouseEvent } from 'react'
import { MINESWEEP_DIMS, type MineDifficulty } from '../../../minigames/minesweeperLogic'

export function MinesweeperModal({
  mineDifficulty,
  mineField,
  onCellClick,
  onCellFlag,
}: {
  mineDifficulty: MineDifficulty
  mineField: number[][]
  onCellClick: (r: number, c: number) => void
  onCellFlag: (e: ReactMouseEvent, r: number, c: number) => void
}) {
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
            const label =
              v === -2 ? '⚑'
                : v === 9 ? '💥'
                  : v === 10 ? '💣'
                    : v > 0 ? String(v)
                      : ''
            const revealed = (v >= 0 && v <= 8) || v === 9 || v === 10
            const numCls =
              v >= 1 && v <= 8 ? ` mine-num mine-num-${v}` : v === 9 ? ' mine-boom' : v === 10 ? ' mine-dormant' : ''
            return (
              <button
                key={`${r}-${c}`}
                type="button"
                className={`mine-cell revealed-${revealed ? 'open' : 'closed'}${numCls}`}
                onClick={() => onCellClick(r, c)}
                onContextMenu={(e) => onCellFlag(e, r, c)}
              >
                {label}
              </button>
            )
          }),
        )}
      </div>
    </div>
  )
}

