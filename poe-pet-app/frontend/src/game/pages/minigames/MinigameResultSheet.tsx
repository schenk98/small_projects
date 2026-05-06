import type { MinigameEndSummary } from '../../../minigames/types'

export function MinigameResultSheet({
  summary,
  onPlayAgain,
  onClose,
}: {
  summary: MinigameEndSummary
  onPlayAgain: () => void
  onClose: () => void
}) {
  return (
    <div className="minigame-result-sheet">
      <h3>{summary.title}</h3>
      <p><strong>Coins earned:</strong> {summary.coinsEarned}</p>
      <p><strong>Happiness adjustment:</strong> {summary.happinessDeltaPercent > 0 ? '+' : ''}{summary.happinessDeltaPercent}%</p>
      <p><strong>Pet state now:</strong> Hunger {summary.hunger}%, Happiness {summary.happiness}%, Energy {summary.energy}%</p>
      {summary.hlSnap && (
        <div className="snapshot-block">
          <h4>Last round</h4>
          <p>Previous card: {summary.hlSnap.previous} → Next: {summary.hlSnap.next} (streak credited: {summary.hlSnap.streak})</p>
        </div>
      )}
      {summary.connect4Board && (
        <div className="snapshot-block">
          <h4>Final board</h4>
          <div className="connect-grid readonly">
            {summary.connect4Board.map((row, r) => row.map((cell, c) => (
              <div key={`${r}-${c}`} className={`c4 ${cell === 1 ? 'p1' : cell === 2 ? 'p2' : ''}`} />
            )))}
          </div>
        </div>
      )}
      {summary.puzzleSnap && (
        <div className="snapshot-block">
          <h4>Final picture</h4>
          <div className="puzzle-snapshot-wrap">
            <div
              className="puzzle-grid puzzle-snapshot"
              style={{ gridTemplateColumns: `repeat(${summary.puzzleSnap.size}, 1fr)` }}
            >
              {summary.puzzleSnap.tiles.map((tile, idx) => {
                const sz = summary.puzzleSnap!.size
                const denom = Math.max(1, sz - 1)
                const x = tile % sz
                const y = Math.floor(tile / sz)
                return (
                  <div
                    key={idx}
                    className="tile"
                    style={{
                      backgroundImage: `url(${summary.puzzleSnap!.imageUrl})`,
                      backgroundSize: `${sz * 100}% ${sz * 100}%`,
                      backgroundPosition: `${(x / denom) * 100}% ${(y / denom) * 100}%`,
                    }}
                  />
                )
              })}
            </div>
          </div>
        </div>
      )}
      {summary.detailLines?.map((l) => <p key={l} className="muted">{l}</p>)}
      <div className="result-actions">
        <button type="button" onClick={onPlayAgain}>Play again</button>
        <button type="button" className="danger" onClick={onClose}>Close</button>
      </div>
    </div>
  )
}

