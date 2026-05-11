import { cookies } from "next/headers";

import { SiteHeader } from "@/components/SiteHeader";
import { LinkList } from "@/components/Hubs";
import {
  getContentTranslation,
  getTranslationAvailability,
  HUB_TRANSLATION_KEYS,
  parseStankovLang,
} from "@/lib/contentTranslations";

const HUB_KEY = HUB_TRANSLATION_KEYS.urad;

export default async function OfficeHubPage() {
  const cookieStore = await cookies();
  const lang = parseStankovLang(cookieStore.get("stankov_lang")?.value);
  const translationAvailability = getTranslationAvailability(HUB_KEY);
  const override = lang !== "cs" ? getContentTranslation(HUB_KEY, lang) : null;

  return (
    <div className="min-h-full">
      <SiteHeader
        localHref="/urad-a-samosprava"
        currentUrl="https://www.mestostankov.cz/"
        translationAvailability={translationAvailability}
      />

      <main className="container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
        {override ? (
          <article className="card" style={{ padding: "1.5rem" }}>
            <div className="content-prose" dangerouslySetInnerHTML={{ __html: override }} />
          </article>
        ) : (
          <>
            <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>Úřad a samospráva</h1>
            <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
              Úřední hodiny, úřední deska, ePodatelna a informace o samosprávě.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginTop: "1.25rem" }}>
              <LinkList
                title="Úřad"
                links={[
                  { label: "Úřední hodiny", href: "/urad-2/uredni-hodiny/" },
                  { label: "Úřední deska", href: "/urad-2/uredni-deska/" },
                  { label: "ePodatelna", href: "/urad-2/epodatelna/" },
                  { label: "Struktura úřadu", href: "/urad-2/struktura-mestskeho-uradu/" },
                  { label: "Odbory MěÚ", href: "/urad-2/odbory-meu-stankov/" },
                ]}
              />
              <LinkList
                title="Samospráva"
                links={[
                  { label: "Zastupitelstvo", href: "/urad-2/zastupitelstvo-mesta/" },
                  { label: "Usnesení ZM", href: "/urad-2/zastupitelstvo-mesta/usneseni-zm/2026-1" },
                  { label: "Výběrová řízení", href: "/urad-2/vyberova-rizeni/" },
                  { label: "Dotace", href: "/urad-2/dotace/" },
                  { label: "Povinné informace", href: "/urad-2/povinne-zverejnovane-informace" },
                ]}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
