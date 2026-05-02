export function warnNotEnoughEnergy(need: number, have: number, gameName: string): void {
  window.alert(`${gameName}: not enough energy. You need at least ${need}, currently ${Math.floor(have)}.`)
}
