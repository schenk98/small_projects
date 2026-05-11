export const CZ_SYNONYMS: Record<string, string[]> = {
  odpad: ["popelnice", "svoz", "tříděný", "trideny", "sběr", "sberny", "kontejner"],
  popelnice: ["odpad", "svoz", "známka", "znamka"],
  komunalni: ["odpad", "svoz"],
  komunální: ["odpad", "svoz"],
  svoz: ["odpad", "komunalni", "komunální"],
  trash: ["odpad", "waste", "popelnice"],
  waste: ["odpad", "komunální", "komunalni", "svoz"],
  bin: ["popelnice", "kontejner", "odpad"],
  disposal: ["odpad", "svoz", "likvidace"],
  platby: ["poplatky", "poplatek", "úhrada", "uhrada"],
  poplatky: ["platby", "poplatek", "odpad"],
  úřední: ["úřad", "uredni", "úřední deska", "uredni deska"],
  úřad: ["úřední", "uredni", "podání", "podatelna"],
  rozhlas: ["hlášení", "hlaseni", "oznámení", "oznameni"],
  kino: ["program", "film"],
};

export function expandQueryWithSynonyms(query: string): string[] {
  const tokens = query
    .toLowerCase()
    .split(/\s+/)
    .map((t) => t.trim())
    .filter(Boolean);

  const expanded = new Set<string>(tokens);
  // Make synonym expansion robust:
  // - expand both when token matches a key OR appears in any synonym list (reverse map)
  // - handle prefixes (odpad -> odpadoveho)
  const forward = CZ_SYNONYMS;
  const reverse = new Map<string, Set<string>>();
  for (const [k, syns] of Object.entries(forward)) {
    for (const s of syns) {
      const set = reverse.get(s) ?? new Set<string>();
      set.add(k);
      reverse.set(s, set);
    }
  }

  for (const t of tokens) {
    for (const [k, syns] of Object.entries(forward)) {
      if (t === k || t.startsWith(k) || k.startsWith(t)) syns.forEach((s) => expanded.add(s));
    }
    const rev = reverse.get(t);
    if (rev) rev.forEach((k) => expanded.add(k));
  }
  return [...expanded];
}

