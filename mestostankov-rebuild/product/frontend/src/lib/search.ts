import Fuse from "fuse.js";

import { getPageSearchBlob, getScrapeStore, type ScrapedPage } from "@/lib/scrapeStore";
import { normalizeText } from "@/lib/normalizeText";
import { expandQueryWithSynonyms } from "@/lib/synonyms";

export type SearchHit = {
  page: ScrapedPage;
  score: number | null;
};

let fuse: Fuse<ScrapedPage> | null = null;

function getFuse(): Fuse<ScrapedPage> {
  if (fuse) return fuse;
  const store = getScrapeStore();
  const pages = [...store.byId.values()].map((p) => {
    getPageSearchBlob(p);
    return p;
  });

  fuse = new Fuse(pages, {
    includeScore: true,
    threshold: 0.42,
    ignoreLocation: true,
    minMatchCharLength: 2,
    keys: [
      { name: "title", weight: 0.6 },
      // normalized blob improves matching for diacritics & phrasing
      { name: "_search_blob", weight: 0.55 },
      { name: "text", weight: 0.25 },
      { name: "url", weight: 0.2 },
    ],
  });

  return fuse;
}

export function searchPages(query: string, limit = 25): SearchHit[] {
  const q = query.trim();
  if (!q) return [];

  const expanded = expandQueryWithSynonyms(normalizeText(q));
  const fuseQuery = expanded.join(" ");

  const results = getFuse().search(fuseQuery, { limit });
  if (results.length) return results.map((r) => ({ page: r.item, score: r.score ?? null }));

  // Fallback: token prefix match over normalized blob words
  const store = getScrapeStore();
  const tokens = expanded.filter((t) => t.length >= 3);
  const hits: SearchHit[] = [];
  for (const p of store.byId.values()) {
    const blob = getPageSearchBlob(p);
    const words = blob.split(" ");
    const ok = tokens.some((t) => words.some((w) => w.startsWith(t)));
    if (ok) hits.push({ page: p, score: null });
    if (hits.length >= limit) break;
  }
  return hits;
}

