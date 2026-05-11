import Link from "next/link";

import { getTranslationAvailability } from "@/lib/contentTranslations";
import { SiteHeader } from "@/components/SiteHeader";
import { Pagination } from "@/components/Pagination";
import { loadRawHtml, lookupPageByFullUrl } from "@/lib/scrapeStore";
import { extractOfficialBoardItems } from "@/lib/listExtractors";
import { normalizeText } from "@/lib/normalizeText";

const BASE = "/urad-2/uredni-deska/";
const SOURCE_URL = "https://www.mestostankov.cz/urad-2/uredni-deska/";

export default async function OfficialBoardPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const pageRaw = sp.page;
  const page = Math.max(1, Number(Array.isArray(pageRaw) ? pageRaw[0] : pageRaw) || 1);
  const qRaw = sp.q;
  const q = normalizeText(Array.isArray(qRaw) ? qRaw[0] ?? "" : qRaw ?? "");
  const categoryRaw = sp.cat;
  const cat = normalizeText(Array.isArray(categoryRaw) ? categoryRaw[0] ?? "" : categoryRaw ?? "");

  const scraped = lookupPageByFullUrl(SOURCE_URL);
  const raw = scraped ? loadRawHtml(scraped) : "";
  const all = raw ? extractOfficialBoardItems(raw) : [];
  const translationAvailability = getTranslationAvailability(scraped?.id ?? "");

  const categories = Array.from(new Set(all.map((x) => x.category))).sort((a, b) => a.localeCompare(b));

  const filtered = all.filter((it) => {
    const blob = normalizeText(`${it.category}\n${it.title}\n${it.posted ?? ""}\n${it.removed ?? ""}`);
    if (q && !blob.includes(q)) return false;
    if (cat && !normalizeText(it.category).includes(cat)) return false;
    return true;
  });

  const pageSize = 20;
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const cur = Math.min(page, totalPages);
  const slice = filtered.slice((cur - 1) * pageSize, cur * pageSize);

  const baseHrefWithFilters = (() => {
    const usp = new URLSearchParams();
    if (q) usp.set("q", q);
    if (cat) usp.set("cat", cat);
    const s = usp.toString();
    return s ? `${BASE}?${s}` : BASE;
  })();

  return (
    <div className="min-h-full">
      <SiteHeader
        currentUrl={SOURCE_URL}
        localHref={BASE}
        translationAvailability={translationAvailability}
      />

      <main className="container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
        <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>Úřední deska</h1>
        <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
          Vyhledávání a filtrování záznamů (moderní zobrazení nad legacy daty).
        </p>

        <form
          method="get"
          className="card"
          style={{ padding: "1rem", marginTop: "1rem", display: "grid", gap: "0.75rem" }}
        >
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
            <div>
              <label style={{ display: "block", fontWeight: 800, marginBottom: "0.25rem" }} htmlFor="q">
                Hledat
              </label>
              <input id="q" name="q" defaultValue={q} className="search-input" placeholder="např. vyhláška, daň, hrob…" />
            </div>
            <div>
              <label style={{ display: "block", fontWeight: 800, marginBottom: "0.25rem" }} htmlFor="cat">
                Kategorie
              </label>
              <select id="cat" name="cat" defaultValue={cat} className="search-input">
                <option value="">Vše</option>
                {categories.map((c) => (
                  <option key={c} value={normalizeText(c)}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
            <button className="btn" type="submit">
              Filtrovat
            </button>
            <Link className="card" style={{ padding: "0.75rem 1rem", textDecoration: "none", color: "inherit" }} href={BASE}>
              Reset
            </Link>
          </div>
        </form>

        <div style={{ display: "grid", gap: "0.75rem", marginTop: "1rem" }}>
          {slice.map((it) => (
            <Link
              key={`${it.category}:${it.href}`}
              href={it.href}
              className="card"
              style={{ padding: "1rem", textDecoration: "none", color: "inherit" }}
            >
              <div style={{ fontSize: "0.85rem", color: "var(--muted)", fontWeight: 800 }}>{it.category}</div>
              <div style={{ fontWeight: 900, fontSize: "1.05rem", marginTop: "0.25rem" }}>{it.title}</div>
              <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", marginTop: "0.35rem", color: "var(--muted)" }}>
                {it.posted ? <span>{it.posted}</span> : null}
                {it.removed ? <span>{it.removed}</span> : null}
              </div>
            </Link>
          ))}

          {!filtered.length ? (
            <div className="card" style={{ padding: "1rem", color: "var(--muted)" }}>
              Žádné záznamy neodpovídají filtru.
            </div>
          ) : null}
        </div>

        <Pagination baseHref={baseHrefWithFilters} page={cur} totalPages={totalPages} />
      </main>
    </div>
  );
}

