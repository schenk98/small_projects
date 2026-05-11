import Link from "next/link";

import { getTranslationAvailability } from "@/lib/contentTranslations";
import { SiteHeader } from "@/components/SiteHeader";
import { Pagination } from "@/components/Pagination";
import { loadRawHtml, lookupPageByFullUrl } from "@/lib/scrapeStore";
import { extractAktualityItems } from "@/lib/listExtractors";

const BASE = "/informace-pro-obcany/aktuality/";
const SOURCE_URL = "https://www.mestostankov.cz/informace-pro-obcany/aktuality/";

export default async function AktualityListPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const sp = await searchParams;
  const pageRaw = sp.page;
  const page = Math.max(1, Number(Array.isArray(pageRaw) ? pageRaw[0] : pageRaw) || 1);

  const scraped =
    lookupPageByFullUrl(SOURCE_URL) ??
    lookupPageByFullUrl(SOURCE_URL.replace(/\/$/, "")) ??
    null;
  const raw = scraped ? loadRawHtml(scraped) : "";
  const items = raw ? extractAktualityItems(raw) : [];
  const translationAvailability = getTranslationAvailability(scraped?.id ?? "");

  const pageSize = 12;
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const cur = Math.min(page, totalPages);
  const slice = items.slice((cur - 1) * pageSize, cur * pageSize);

  return (
    <div className="min-h-full">
      <SiteHeader
        currentUrl={SOURCE_URL}
        localHref={BASE}
        translationAvailability={translationAvailability}
      />

      <main className="container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
        <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>Aktuality</h1>
        <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
          Přehled novinek z webu města.
        </p>

        <div style={{ display: "grid", gap: "0.75rem", marginTop: "1.25rem" }}>
          {slice.map((it) => (
            <Link
              key={it.href}
              href={it.href}
              className="card card-hover"
              style={{ padding: "1rem", textDecoration: "none", color: "inherit" }}
            >
              <div style={{ display: "flex", gap: "0.75rem", alignItems: "flex-start" }}>
                {it.imageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={it.imageUrl}
                    alt=""
                    width={72}
                    height={72}
                    style={{ width: 72, height: 72, objectFit: "cover", borderRadius: 8 }}
                  />
                ) : null}
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 900, fontSize: "1.1rem" }}>{it.title}</div>
                  {it.dateText ? <div style={{ color: "var(--muted)", marginTop: "0.25rem" }}>{it.dateText}</div> : null}
                  {it.description ? (
                    <div style={{ color: "var(--muted)", marginTop: "0.4rem" }}>{it.description}</div>
                  ) : null}
                </div>
              </div>
            </Link>
          ))}

          {!items.length ? (
            <div className="card" style={{ padding: "1rem", color: "var(--muted)" }}>
              Data zatím nejsou k dispozici (znovu načtěte stránku).
            </div>
          ) : null}
        </div>

        <Pagination baseHref={BASE} page={cur} totalPages={totalPages} />
      </main>
    </div>
  );
}

