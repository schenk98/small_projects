export function PuzzleSwapModal({
  puzzleSize,
  puzzleMoves,
  puzzleBoard,
  puzzleFirst,
  puzzleImage,
  onSwap,
}: {
  puzzleSize: number
  puzzleMoves: number
  puzzleBoard: number[]
  puzzleFirst: number | null
  puzzleImage: string
  onSwap: (idx: number) => void
}) {
  return (
    <div className="puzzle-modal-block">
      <h3>Puzzle Swap ({puzzleSize}x{puzzleSize})</h3>
      <p>Moves: {puzzleMoves}</p>
      <div className="puzzle-play-wrap" style={{ ['--puzzle-n' as string]: String(puzzleSize) }}>
        <div className="puzzle-grid puzzle-grid-modal" style={{ gridTemplateColumns: `repeat(${puzzleSize}, 1fr)` }}>
          {puzzleBoard.map((tile, idx) => {
            const x = tile % puzzleSize
            const y = Math.floor(tile / puzzleSize)
            const denom = Math.max(1, puzzleSize - 1)
            return (
              <button
                key={idx}
                type="button"
                className={`tile ${puzzleFirst === idx ? 'selected' : ''}`}
                onClick={() => onSwap(idx)}
                style={{
                  backgroundImage: `url(${puzzleImage})`,
                  backgroundSize: `${puzzleSize * 100}% ${puzzleSize * 100}%`,
                  backgroundPosition: `${(x / denom) * 100}% ${(y / denom) * 100}%`,
                }}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

