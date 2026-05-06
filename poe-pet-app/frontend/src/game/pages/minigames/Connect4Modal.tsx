import type { ConnectCell } from '../../../minigames/connect4Ai'

export function Connect4Modal({
  status,
  board,
  onPlayColumn,
}: {
  status: string
  board: ConnectCell[][]
  onPlayColumn: (col: number) => void
}) {
  return (
    <>
      <h3>Connect 4</h3>
      <p>{status}</p>
      <div className="connect-grid">
        {board.map((row, r) => row.map((cell, c) => (
          <button
            key={`${r}-${c}`}
            type="button"
            className={`c4 ${cell === 1 ? 'p1' : cell === 2 ? 'p2' : ''}`}
            onClick={() => onPlayColumn(c)}
          />
        )))}
      </div>
    </>
  )
}

