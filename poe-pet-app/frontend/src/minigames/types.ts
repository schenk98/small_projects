export type MinigameEndSummary = {
  title: string
  coinsEarned: number
  happinessDeltaPercent: number
  hunger: number
  happiness: number
  energy: number
  detailLines?: string[]
  hlSnap?: { previous: number; next: number; streak: number }
  connect4Board?: number[][]
  puzzleSnap?: { size: number; tiles: number[]; imageUrl: string }
}
