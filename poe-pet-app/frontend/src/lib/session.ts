/** Detect auth failure messages from API client / fetch wrappers. */
export function isSessionExpiredMessage(message: string): boolean {
  const m = message.toLowerCase()
  return (
    m.includes('401')
    || m.includes('unauthorized')
    || m.includes('invalid token')
    || m.includes('missing token')
    || m.includes('forbidden')
  )
}
