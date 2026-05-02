/**
 * Small JSON fetch helper for the Spring API: one place for base URL, auth headers, and error shape.
 */
export function createJsonApiClient(apiBaseUrl: string, authHeaders: Record<string, string>) {
  /** Parsed JSON body; callers narrow fields as needed (Spring returns loosely typed maps). */
  return async function apiJson(path: string, init?: RequestInit): Promise<any> {
    const mergedHeaders: Record<string, string> = {
      ...authHeaders,
      ...(init?.headers as Record<string, string> | undefined),
    }
    const res = await fetch(`${apiBaseUrl}${path}`, { ...init, headers: mergedHeaders })
    const raw = await res.text()
    let data: Record<string, unknown> = {}
    try {
      data = raw ? (JSON.parse(raw) as Record<string, unknown>) : {}
    } catch {
      data = { error: raw || 'Non-JSON response' }
    }
    if (!res.ok) {
      const message = (data.error as string) || `Request failed (${res.status})`
      throw new Error(`${message} (${path})`)
    }
    return data as any
  }
}
