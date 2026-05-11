import Link from "next/link";

import { SiteHeader } from "@/components/SiteHeader";
import { CURATED_SEARCH_KEY, getTranslationAvailability } from "@/lib/contentTranslations";
import { searchPages } from "@/lib/search";
import { getPageSearchSnippet } from "@/lib/scrapeStore";

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const qRaw = sp.q;
  const q = Array.isArray(qRaw) ? qRaw[0] ?? "" : qRaw ?? "";

  const hits = q ? searchPages(q, 30) : [];
  const currentUrl = new URL(`/hledat?q=${encodeURIComponent(q)}`, "https://www.mestostankov.cz").toString();
  const translationAvailability = getTranslationAvailability(CURATED_SEARCH_KEY);

  return (
    <div className="min-h-full">
      <SiteHeader
        query={q}
        currentUrl={currentUrl}
        localHref={`/hledat?q=${encodeURIComponent(q)}`}
        translationAvailability={translationAvailability}
      />

      <main className="container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 800, margin: 0 }}>Vyhledávání</h1>
        <div style={{ marginTop: "0.5rem", color: "var(--muted)" }}>
          Dotaz: <span style={{ fontWeight: 800, color: "var(--foreground)" }}>{q || "—"}</span>
        </div>

        <div style={{ marginTop: "1.25rem", display: "grid", gap: "0.75rem" }}>
          {q && hits.length === 0 ? (
            <div className="card" style={{ padding: "1rem" }}>
              Nic jsme nenašli. Zkuste jiné slovo (např. „odpad“ nebo „popelnice“).
            </div>
          ) : null}

          {hits.map((h) => (
            <div key={h.page.id} className="card" style={{ padding: "1rem" }}>
              <div style={{ fontSize: "0.8rem", color: "var(--muted)" }}>{h.page.content_type}</div>
              <Link
                href={new URL(h.page.url).pathname + new URL(h.page.url).search}
                className="mt-1 block"
                style={{ fontWeight: 800, textDecoration: "none", color: "var(--link)" }}
              >
                {h.page.title || h.page.url}
              </Link>
              {(() => {
                const snippet = getPageSearchSnippet(h.page);
                return snippet ? (
                  <div style={{ marginTop: "0.5rem", color: "var(--foreground)" }}>{snippet}</div>
                ) : null;
              })()}
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

