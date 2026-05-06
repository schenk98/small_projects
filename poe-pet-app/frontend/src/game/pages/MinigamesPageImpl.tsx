import { useState } from 'react'
import type { MouseEvent as ReactMouseEvent } from 'react'
import type { Dashboard, RewardPreview } from '../../lib/dashboard'
import type { MinigameInfo } from '../../lib/gameTypes'
import type { MinigameEndSummary } from '../../minigames/types'
import type { MineDifficulty } from '../../minigames/minesweeperLogic'
import type { CheckersDifficulty } from '../../minigames/checkersAi'
import { CheckersModal } from './minigames/CheckersModal'
import { Connect4Modal } from './minigames/Connect4Modal'
import { HigherLowerModal } from './minigames/HigherLowerModal'
import { MinigameModalShell } from './minigames/MinigameModalShell'
import { MinigameResultSheet } from './minigames/MinigameResultSheet'
import { MinigamesList } from './minigames/MinigamesList'
import { MinesweeperModal } from './minigames/MinesweeperModal'
import { PuzzleSwapModal } from './minigames/PuzzleSwapModal'
import type { ActiveMinigameCode } from './minigames/types'
import { useCheckers } from './minigames/useCheckers'
import { useConnect4 } from './minigames/useConnect4'
import { useEnergyGate } from './minigames/useEnergyGate'
import { useHigherLower } from './minigames/useHigherLower'
import { useModalDismiss } from './minigames/useModalDismiss'
import { useMinesweeper } from './minigames/useMinesweeper'
import { usePuzzleSwap } from './minigames/usePuzzleSwap'
import { useRewardCaptions } from './minigames/useRewardCaptions'

export function MinigamesPageImpl({
  apiJson,
  dashboard,
  refresh,
  minigames,
  rewardPreview,
  setMessage,
}: {
  apiJson: (path: string, init?: RequestInit) => Promise<unknown>
  dashboard: Dashboard
  refresh: () => Promise<Dashboard | undefined>
  minigames: MinigameInfo[]
  rewardPreview?: RewardPreview
  setMessage: (m: string) => void
}) {
  /**
   * This page is intentionally "thin orchestration":
   * - it owns modal state + which minigame is active
   * - each minigame’s rules/UI live in `pages/minigames/*`
   */
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState<ActiveMinigameCode | null>(null)
  const [endSummary, setEndSummary] = useState<MinigameEndSummary | null>(null)

  const { energyCostFor, assertEnergy } = useEnergyGate({ rewardPreview, minigames })
  const { rewardCaptionFor } = useRewardCaptions(rewardPreview)

  const openModal = () => setOpen(true)
  const locked = Boolean(endSummary)

  const hl = useHigherLower({
    apiJson, dashboard, refresh, setMessage, energyCostFor, assertEnergy,
    openModal,
    setActive: () => setActive('higher_lower'),
    setEndSummary,
  })
  const puzzle = usePuzzleSwap({
    apiJson, dashboard, refresh, setMessage, energyCostFor, assertEnergy,
    openModal,
    setActive: () => setActive('puzzle_swap'),
    setEndSummary,
  })
  const c4 = useConnect4({
    apiJson, dashboard, refresh, setMessage, energyCostFor, assertEnergy,
    openModal,
    setActive: () => setActive('connect4_ai'),
    setEndSummary,
  })
  const ms = useMinesweeper({
    apiJson, dashboard, refresh, setMessage, energyCostFor, assertEnergy,
    openModal,
    setActive: () => setActive('minesweep_ai'),
    setEndSummary,
  })
  const ck = useCheckers({
    apiJson, dashboard, refresh, setMessage, energyCostFor, assertEnergy,
    openModal,
    setActive: () => setActive('checkers_ai'),
    setEndSummary,
  })

  /**
   * Close modal and reset all minigame-local UI state.
   * We also refresh the dashboard so coins/energy/happiness are up-to-date.
   */
  const closeFully = async () => {
    setEndSummary(null)
    setOpen(false)
    setActive(null)
    hl.reset(); puzzle.reset(); c4.reset(); ms.reset(); ck.reset()
    await refresh()
  }

  /**
   * Quit an in-progress minigame session.
   * Higher/Lower has a special "quit = claim current streak payout" behavior.
   */
  const quit = async () => {
    if (locked) return
    if (active === 'higher_lower') return void hl.quit(true)
    if (active === 'puzzle_swap') await puzzle.abandon(true)
    else if (active === 'connect4_ai') await c4.abandon(true)
    else if (active === 'minesweep_ai') await ms.abandon(true)
    else if (active === 'checkers_ai') await ck.abandon(true)
    setOpen(false); setActive(null); setEndSummary(null)
    await refresh()
  }

  const { onBackdrop } = useModalDismiss({ open, active, endSummary, closeFully, quit })

  /** Convenience: start the same minigame again after a result sheet. */
  const playAgain = async () => {
    setEndSummary(null)
    if (active === 'higher_lower') await hl.start()
    else if (active === 'puzzle_swap') await puzzle.start()
    else if (active === 'connect4_ai') await c4.start()
    else if (active === 'minesweep_ai') await ms.start()
    else if (active === 'checkers_ai') await ck.start()
  }

  const onStart = async (code: ActiveMinigameCode) => {
    if (code === 'higher_lower') await hl.start()
    else if (code === 'puzzle_swap') await puzzle.start()
    else if (code === 'connect4_ai') await c4.start()
    else if (code === 'minesweep_ai') await ms.start()
    else if (code === 'checkers_ai') await ck.start()
  }

  return (
    <div className="card pane">
      <MinigamesList
        minigames={minigames}
        rewardPreview={rewardPreview}
        energyCostFor={energyCostFor}
        rewardCaptionFor={rewardCaptionFor}
        puzzleSize={puzzle.puzzleSize}
        onChangePuzzleSize={(n) => puzzle.setPuzzleSize(n)}
        connectDifficulty={c4.difficulty}
        onChangeConnectDifficulty={(d) => c4.setDifficulty(d)}
        mineDifficultyLabel="Difficulty:"
        mineDifficultySelect={(
          <select value={ms.difficulty} onChange={(e) => ms.setDifficulty(e.target.value as MineDifficulty)}>
            <option value="easy">Beginner 9×9 (10 mines)</option>
            <option value="medium">Intermediate 16×16 (40)</option>
            <option value="hard">Expert 16×30 (99)</option>
          </select>
        )}
        checkersDifficultyLabel="Difficulty:"
        checkersDifficultySelect={(
          <select value={ck.difficulty} onChange={(e) => ck.setDifficulty(e.target.value as CheckersDifficulty)}>
            <option value="easy">Easy (random)</option>
            <option value="medium">Medium (minimax depth 1)</option>
            <option value="hard">Hard (minimax depth 3)</option>
          </select>
        )}
        onStart={(code) => void onStart(code)}
      />

      <MinigameModalShell open={open} onBackdrop={onBackdrop}>
        {endSummary ? (
          <MinigameResultSheet summary={endSummary} onPlayAgain={() => void playAgain()} onClose={() => void closeFully()} />
        ) : active === 'higher_lower' ? (
          <HigherLowerModal currentNumber={hl.currentNumber} streak={hl.streak} onGuess={(d) => void hl.guess(d)} />
        ) : active === 'puzzle_swap' ? (
          <PuzzleSwapModal
            puzzleSize={puzzle.puzzleSize}
            puzzleMoves={puzzle.puzzleMoves}
            puzzleBoard={puzzle.puzzleBoard}
            puzzleFirst={puzzle.puzzleFirst}
            puzzleImage={puzzle.puzzleImage}
            onSwap={(idx) => void puzzle.swap(idx)}
          />
        ) : active === 'connect4_ai' ? (
          <Connect4Modal status={c4.status} board={c4.board} onPlayColumn={(col) => void c4.play(col, true)} />
        ) : active === 'minesweep_ai' ? (
          <MinesweeperModal
            mineDifficulty={ms.difficulty}
            mineField={ms.field}
            onCellClick={(r, c) => void ms.click(r, c, true, locked)}
            onCellFlag={(e: ReactMouseEvent, r, c) => {
              e.preventDefault()
              ms.flag(r, c, true, locked)
            }}
          />
        ) : active === 'checkers_ai' ? (
          <CheckersModal state={ck.state} pick={ck.pick} disabled={locked} onCell={(r, c) => ck.click(r, c, true, locked)} />
        ) : null}

        {!endSummary && open && (
          <button type="button" className="danger" onClick={() => void quit()}>Close</button>
        )}
      </MinigameModalShell>
    </div>
  )
}

