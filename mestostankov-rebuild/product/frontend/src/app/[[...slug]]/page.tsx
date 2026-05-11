import { notFound } from "next/navigation";

import Link from "next/link";
import { cookies } from "next/headers";

import {
  CURATED_HOME_KEY,
  getContentTranslation,
  getTranslationAvailability,
  parseStankovLang,
} from "@/lib/contentTranslations";
import { loadPageMainHtml, loadRawHtml, lookupPageByFullUrl } from "@/lib/scrapeStore";
import { SiteHeader } from "@/components/SiteHeader";
import { HubGrid } from "@/components/Hubs";
import { extractHomepageHighlights } from "@/lib/homepageHighlights";

function withTrailingSlashForLegacy(pathname: string): string {
  if (pathname === "/") return "/";
  if (pathname.endsWith("/")) return pathname;
  // Keep URLs that look like file paths without forcing trailing slash
  const lastSeg = pathname.split("/").filter(Boolean).at(-1) ?? "";
  if (lastSeg.includes(".")) return pathname;
  return `${pathname}/`;
}

function buildRequestedScrapeUrl(pathname: string, searchParams: Record<string, string | string[] | undefined>): string {
  const scrapeOrigin = process.env.SCRAPE_ORIGIN ?? "https://www.mestostankov.cz";
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(searchParams)) {
    if (typeof v === "undefined") continue;
    if (Array.isArray(v)) v.forEach((vv) => sp.append(k, vv));
    else sp.set(k, v);
  }
  const search = sp.toString();
  const finalPath = withTrailingSlashForLegacy(pathname);
  return new URL(`${finalPath}${search ? `?${search}` : ""}`, scrapeOrigin).toString();
}

export default async function MirrorPage({
  params,
  searchParams,
}: {
  params: Promise<{ slug?: string[] }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const resolvedParams = await params;
  const resolvedSearchParams = await searchParams;

  const slug = resolvedParams.slug ?? [];
  const pathname = `/${slug.join("/")}`;
  const fullUrl = buildRequestedScrapeUrl(pathname, resolvedSearchParams);
  const localHref = `${pathname}${Object.keys(resolvedSearchParams).length ? `?${new URLSearchParams(
    Object.entries(resolvedSearchParams).flatMap(([k, v]) =>
      typeof v === "undefined"
        ? []
        : Array.isArray(v)
          ? v.map((vv) => [k, vv] as [string, string])
          : ([[k, v]] as [string, string][]),
    ),
  ).toString()}` : ""}`;
  // Curated homepage (vision-inspired). Everything else stays mirrored.
  if (pathname === "/") {
    const cookieStore = await cookies();
    const stankovLang = parseStankovLang(cookieStore.get("stankov_lang")?.value);

    const root = lookupPageByFullUrl("https://www.mestostankov.cz/");
    const raw = root ? loadRawHtml(root) : "";
    const hl = raw ? extractHomepageHighlights(raw) : { news: [], events: [] };

    const homeTransKey = root?.id ?? CURATED_HOME_KEY;
    const translationAvailability = getTranslationAvailability(homeTransKey);
    const homeOverride =
      stankovLang !== "cs" ? getContentTranslation(homeTransKey, stankovLang) : null;

    return (
      <div className="min-h-full">
        <SiteHeader
          currentUrl="https://www.mestostankov.cz/"
          localHref="/"
          translationAvailability={translationAvailability}
        />

        <main className="container" style={{ paddingTop: "1.5rem", paddingBottom: "2rem" }}>
          {homeOverride ? (
            <article className="card" style={{ padding: "1.5rem" }}>
              <div className="content-prose" dangerouslySetInnerHTML={{ __html: homeOverride }} />
            </article>
          ) : (
            <>
          <div style={{ marginTop: "1.25rem" }}>
            <HubGrid
              items={[
                { title: "O Staňkově", href: "/o-stankove", description: "Historie, současnost, mapa, organizace" },
                { title: "Co se děje", href: "/co-se-deje", description: "Aktuality, akce, rozhlas, fotky" },
                { title: "Co potřebujete", href: "/co-potrebujete", description: "Poplatky, odpady, formuláře, situace" },
                { title: "Úřad", href: "/urad-a-samosprava", description: "Úřední deska, hodiny, ePodatelna" },
              ]}
            />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginTop: "1.5rem" }}>
            <section className="card" style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: "1rem" }}>
                <h2 style={{ margin: 0, fontSize: "1.25rem", fontWeight: 900 }}>Aktuality</h2>
                <Link href="/informace-pro-obcany/aktuality/" style={{ fontSize: "0.9rem" }}>
                  Zobrazit vše
                </Link>
              </div>
              <div style={{ display: "grid", gap: "0.6rem", marginTop: "0.75rem" }}>
                {hl.news.slice(0, 5).map((n) => (
                  <Link key={n.href} href={n.href} style={{ textDecoration: "none", color: "inherit" }}>
                    <div className="card card-hover" style={{ padding: "0.75rem" }}>
                      <div style={{ fontWeight: 900 }}>{n.title}</div>
                      {n.dateText ? <div style={{ color: "var(--muted)", fontSize: "0.9rem" }}>{n.dateText}</div> : null}
                      {n.description ? <div style={{ color: "var(--muted)", fontSize: "0.95rem", marginTop: "0.25rem" }}>{n.description}</div> : null}
                    </div>
                  </Link>
                ))}
                {hl.news.length === 0 ? <div style={{ color: "var(--muted)" }}>Načítám…</div> : null}
              </div>
            </section>

            <section className="card" style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: "1rem" }}>
                <h2 style={{ margin: 0, fontSize: "1.25rem", fontWeight: 900 }}>Kalendář akcí</h2>
                <Link href="/informace-pro-obcany/kalendar-akci/" style={{ fontSize: "0.9rem" }}>
                  Zobrazit vše
                </Link>
              </div>
              <div style={{ display: "grid", gap: "0.6rem", marginTop: "0.75rem" }}>
                {hl.events.slice(0, 5).map((e) => (
                  <Link key={e.href} href={e.href} style={{ textDecoration: "none", color: "inherit" }}>
                    <div className="card card-hover" style={{ padding: "0.75rem" }}>
                      <div style={{ fontWeight: 900 }}>{e.title}</div>
                      {e.dateText ? <div style={{ color: "var(--muted)", fontSize: "0.9rem" }}>{e.dateText}</div> : null}
                      {e.description ? <div style={{ color: "var(--muted)", fontSize: "0.95rem", marginTop: "0.25rem" }}>{e.description}</div> : null}
                    </div>
                  </Link>
                ))}
                {hl.events.length === 0 ? <div style={{ color: "var(--muted)" }}>Načítám…</div> : null}
              </div>
            </section>
          </div>
            </>
          )}
        </main>
      </div>
    );
  }

  const page = lookupPageByFullUrl(fullUrl);

  if (!page) notFound();

  const cookieStore = await cookies();
  const stankovLang = parseStankovLang(cookieStore.get("stankov_lang")?.value);
  const main = loadPageMainHtml(page);
  const translated =
    stankovLang !== "cs" ? getContentTranslation(page.id, stankovLang) : null;
  const mainHtml = translated ?? main.html;
  const translationAvailability = getTranslationAvailability(page.id);

  return (
    <div className="min-h-full">
      <SiteHeader
        currentUrl={fullUrl}
        localHref={localHref}
        translationAvailability={translationAvailability}
      />

      <main className="container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
        <article className="card" style={{ padding: "1.5rem" }}>
          <div className="content-prose">
            <div dangerouslySetInnerHTML={{ __html: mainHtml }} />
          </div>
        </article>
      </main>
    </div>
  );
}

