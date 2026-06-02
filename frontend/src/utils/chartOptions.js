export function baseGrid() {
  return {
    left: 44,
    right: 22,
    top: 42,
    bottom: 40,
    containLabel: true,
  }
}

export function formatVideoTime(value) {
  const minutes = Math.floor(value / 60)
  const seconds = String(value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}
