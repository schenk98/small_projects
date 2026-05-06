import type { RewardPreview } from '../../../lib/dashboard'
import type { MinigameInfo } from '../../../lib/gameTypes'
import type { ActiveMinigameCode } from './types'

export function MinigamesList({
  minigames,
  rewardPreview,
  energyCostFor,
  rewardCaptionFor,
  puzzleSize,
  onChangePuzzleSize,
  connectDifficulty,
  onChangeConnectDifficulty,
  mineDifficultyLabel,
  mineDifficultySelect,
  checkersDifficultyLabel,
  checkersDifficultySelect,
  onStart,
}: {
  minigames: MinigameInfo[]
  rewardPreview?: RewardPreview
  energyCostFor: (code: string) => number
  rewardCaptionFor: (g: MinigameInfo) => string[]
  puzzleSize: number
  onChangePuzzleSize: (n: number) => void
  connectDifficulty: 'easy' | 'medium' | 'hard'
  onChangeConnectDifficulty: (d: 'easy' | 'medium' | 'hard') => void
  mineDifficultyLabel: string
  mineDifficultySelect: React.ReactNode
  checkersDifficultyLabel: string
  checkersDifficultySelect: React.ReactNode
  onStart: (code: ActiveMinigameCode) => void
}) {
  return (
    <>
      {minigames.map((g) => (
        <details key={g.code} className="inv-item">
          <summary>{g.name}</summary>
          <p>{g.description}</p>
          <p><strong>Energy cost:</strong> {energyCostFor(g.code)}</p>
          {!rewardPreview && (
            <p className="muted">
              Live payout preview unavailable. Use a current backend (see README “Verify local stack”): from repo root run{' '}
              <code>start-all.ps1</code> or restart <code>backend</code> with <code>mvn spring-boot:run</code> after <code>git pull</code>.
            </p>
          )}
          {rewardCaptionFor(g).map((line, i) => (
            <p key={i}>{i === 0 ? <><strong>Payout preview:</strong> {line}</> : line}</p>
          ))}
          {g.code === 'higher_lower' && <button type="button" onClick={() => onStart('higher_lower')}>Start</button>}
          {g.code === 'puzzle_swap' && (
            <div className="subnav">
              <select value={String(puzzleSize)} onChange={(e) => onChangePuzzleSize(Number(e.target.value))}>
                {[3, 5, 7, 9, 11].map((n) => <option key={n} value={n}>{n}x{n}</option>)}
              </select>
              <button type="button" onClick={() => onStart('puzzle_swap')}>Start</button>
            </div>
          )}
          {g.code === 'connect4_ai' && (
            <div className="subnav">
              <select value={connectDifficulty} onChange={(e) => onChangeConnectDifficulty(e.target.value as 'easy' | 'medium' | 'hard')}>
                <option value="easy">Easy (random)</option>
                <option value="medium">Medium (minimax d1)</option>
                <option value="hard">Hard (minimax d3)</option>
              </select>
              <button type="button" onClick={() => onStart('connect4_ai')}>Start</button>
            </div>
          )}
          {g.code === 'minesweep_ai' && (
            <div className="subnav">
              <span className="muted">{mineDifficultyLabel}</span>
              {mineDifficultySelect}
              <button type="button" onClick={() => onStart('minesweep_ai')}>Start</button>
            </div>
          )}
          {g.code === 'checkers_ai' && (
            <div className="subnav">
              <span className="muted">{checkersDifficultyLabel}</span>
              {checkersDifficultySelect}
              <button type="button" onClick={() => onStart('checkers_ai')}>Start</button>
            </div>
          )}
        </details>
      ))}
    </>
  )
}

