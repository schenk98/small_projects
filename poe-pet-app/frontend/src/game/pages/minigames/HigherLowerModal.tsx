export function HigherLowerModal({
  currentNumber,
  streak,
  onGuess,
}: {
  currentNumber: number | null
  streak: number
  onGuess: (dir: 'HIGHER' | 'LOWER') => void
}) {
  return (
    <>
      <h3>Higher / Lower</h3>
      <p>Current number: {currentNumber} | Streak: {streak}</p>
      <div>
        <button type="button" onClick={() => onGuess('HIGHER')}>Higher</button>
        <button type="button" onClick={() => onGuess('LOWER')}>Lower</button>
      </div>
    </>
  )
}

