import { cookies } from "next/headers";

import { SiteHeader } from "@/components/SiteHeader";
import { LinkList } from "@/components/Hubs";
import {
  getContentTranslation,
  getTranslationAvailability,
  HUB_TRANSLATION_KEYS,
  parseStankovLang,
} from "@/lib/contentTranslations";

const HUB_KEY = HUB_TRANSLATION_KEYS.oStankove;

export default async function AboutHubPage() {
  const cookieStore = await cookies();
  const lang = parseStankovLang(cookieStore.get("stankov_lang")?.value);
  const translationAvailability = getTranslationAvailability(HUB_KEY);
  const override = lang !== "cs" ? getContentTranslation(HUB_KEY, lang) : null;

  return (
    <div className="min-h-full">
      <SiteHeader
        localHref="/o-stankove"
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
            <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>O Staňkově</h1>
            <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
              Rychlý rozcestník k informacím o městě a organizacích.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginTop: "1.25rem" }}>
              <LinkList
                title="O Staňkově"
                links={[
                  { label: "Současnost", href: "/mesto/soucasnost/" },
                  { label: "Historie", href: "/mesto/historie/" },
                  { label: "Části Staňkova", href: "/mesto/soucasnost/casti-stankova/" },
                  { label: "Interaktivní mapa", href: "/mesto/interaktivni-mapa/" },
                  { label: "Publikace", href: "/mesto/historie/publikace-o-stankove/" },
                ]}
              />
              <LinkList
                title="Organizace ve městě"
                links={[
                  { label: "Školství", href: "/mesto/soucasnost/prispevkove-organizace/" },
                  { label: "Zdravotnictví", href: "/mesto/zdravotnictvi/" },
                  { label: "Sport", href: "/mesto/sport/" },
                  { label: "Kultura", href: "/mesto/kultura/" },
                  { label: "Spolky", href: "/mesto/soucasnost/organizace-spolky-sdruzeni/" },
                  { label: "Podnikání", href: "/mesto/soucasnost/podnikani/" },
                ]}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
