export function normalizeText(s: string): string {
  return (s ?? "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}+/gu, "")
    .replace(/[^a-z0-9\s/._-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

