/** Pause before AI updates the board (Connect 4 + Checkers). */
export const AI_MOVE_DELAY_MS = 600

export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}
