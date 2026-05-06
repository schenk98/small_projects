import { useEffect, useRef } from 'react'
import type { MinigameEndSummary } from '../../../minigames/types'
import type { ActiveMinigameCode } from './types'

export function useModalDismiss({
  open,
  active,
  endSummary,
  closeFully,
  quit,
}: {
  open: boolean
  active: ActiveMinigameCode | null
  endSummary: MinigameEndSummary | null
  closeFully: () => Promise<void> | void
  quit: () => Promise<void> | void
}) {
  /** Keep latest handlers without re-binding listeners every render. */
  const ref = useRef({
    closeFully,
    quit,
    endSummary,
    active,
  })
  useEffect(() => {
    ref.current = { closeFully, quit, endSummary, active }
  }, [closeFully, quit, endSummary, active])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return
      e.preventDefault()
      const d = ref.current
      if (d.endSummary) {
        void d.closeFully()
        return
      }
      const msg = d.active === 'higher_lower'
        ? 'Quit Higher/Lower and claim coins for your current streak?'
        : 'Leave this minigame? Half the energy cost will be refunded; no coins or happiness change.'
      if (window.confirm(msg)) void d.quit()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  const onBackdrop = () => {
    const d = ref.current
    if (d.endSummary) {
      void d.closeFully()
      return
    }
    const msg = d.active === 'higher_lower'
      ? 'Quit Higher/Lower and claim coins for your current streak?'
      : 'Leave this minigame? Half the energy cost will be refunded; no coins or happiness change.'
    if (window.confirm(msg)) void d.quit()
  }

  return { onBackdrop }
}

