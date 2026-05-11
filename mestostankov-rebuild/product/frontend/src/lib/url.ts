export function normalizeUrlForLookup(url: string): string {
  const u = new URL(url);
  u.hash = "";

  // Drop common tracking params
  const toDelete: string[] = [];
  u.searchParams.forEach((_, k) => {
    const lk = k.toLowerCase();
    if (
      lk === "utm_source" ||
      lk === "utm_medium" ||
      lk === "utm_campaign" ||
      lk === "utm_term" ||
      lk === "utm_content"
    ) {
      toDelete.push(k);
    }
  });
  for (const k of toDelete) u.searchParams.delete(k);

  // Sort query params (URLSearchParams preserves insertion order)
  const sorted = [...u.searchParams.entries()].sort(([a], [b]) =>
    a.localeCompare(b),
  );
  u.search = sorted.length ? `?${new URLSearchParams(sorted).toString()}` : "";

  // Normalize default ports
  if ((u.protocol === "http:" && u.port === "80") || (u.protocol === "https:" && u.port === "443")) {
    u.port = "";
  }

  return u.toString();
}

