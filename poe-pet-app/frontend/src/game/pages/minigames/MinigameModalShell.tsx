import type { ReactNode } from 'react'

export function MinigameModalShell({
  open,
  onBackdrop,
  children,
}: {
  open: boolean
  onBackdrop: () => void
  children: ReactNode
}) {
  if (!open) return null
  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onBackdrop()
      }}
    >
      <div className="modal modal-minigame" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-minigame-scroll-all">
          {children}
        </div>
      </div>
    </div>
  )
}

