/**
 * Small JSON fetch helper for the Spring API.
 *
 * Design goals:
 * - keep auth/header wiring in one place
 * - normalize error handling (non-2xx => throw Error)
 * - tolerate non-JSON error bodies (common with proxies / old endpoints)
 */
export function createJsonApiClient(apiBaseUrl: string, authHeaders: Record<string, string>) {
  /**
   * Parsed JSON body.
   *
   * Note: backend responses often come from flexible maps; callers narrow fields as needed.
   */
  function getErrorMessage(body: unknown): string | undefined {
    if (!body || typeof body !== 'object') return undefined
    const o = body as Record<string, unknown>
    return typeof o.error === 'string' ? o.error : undefined
  }

  return async function apiJson(path: string, init?: RequestInit): Promise<unknown> {
    const mergedHeaders: Record<string, string> = {
      ...authHeaders,
      ...(init?.headers as Record<string, string> | undefined),
    }
    const res = await fetch(`${apiBaseUrl}${path}`, { ...init, headers: mergedHeaders })
    const raw = await res.text()
    let data: unknown
    try {
      data = raw ? (JSON.parse(raw) as unknown) : {}
    } catch {
      // Keep the raw response visible instead of a generic JSON parse error.
      data = { error: raw || 'Non-JSON response' }
    }
    if (!res.ok) {
      // Encode the route into the message so users can report issues quickly.
      const message = getErrorMessage(data) || `Request failed (${res.status})`
      throw new Error(`${message} (${path})`)
    }
    return data
  }
}
