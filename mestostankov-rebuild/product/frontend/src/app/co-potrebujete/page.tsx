import { cookies } from "next/headers";

import { SiteHeader } from "@/components/SiteHeader";
import { LinkList } from "@/components/Hubs";
import {
  getContentTranslation,
  getTranslationAvailability,
  HUB_TRANSLATION_KEYS,
  parseStankovLang,
} from "@/lib/contentTranslations";

const HUB_KEY = HUB_TRANSLATION_KEYS.coPotrebujete;

export default async function NeedsHubPage() {
  const cookieStore = await cookies();
  const lang = parseStankovLang(cookieStore.get("stankov_lang")?.value);
  const translationAvailability = getTranslationAvailability(HUB_KEY);
  const override = lang !== "cs" ? getContentTranslation(HUB_KEY, lang) : null;

  return (
    <div className="min-h-full">
      <SiteHeader
        localHref="/co-potrebujete"
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
            <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>Co potřebujete vyřídit</h1>
            <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
              Nejčastější situace a služby pro občany.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginTop: "1.25rem" }}>
              <LinkList
                title="Rychlý rozcestník"
                links={[
                  { label: "Životní situace", href: "/urad-2/zivotni-situace/" },
                  {
                    label: "Poplatky a platby",
                    href: "https://platby.plzensky-kraj.cz/obce/domazlice/stankov",
                    external: true,
                  },
                  { label: "Odpady", href: "/mesto/odpady/" },
                  { label: "Rezervační systém", href: "/mesto/rezervacni-system/" },
                  { label: "Formuláře / dokumenty", href: "/urad-2/epodatelna/" },
                  { label: "Doklady / CzechPOINT", href: "/urad-2/epodatelna/" },
                ]}
              />

              <LinkList
                title="Další"
                links={[
                  { label: "CHVAK", href: "/mesto/soucasnost/chvak/" },
                  { label: "Podnikání", href: "/mesto/soucasnost/podnikani/" },
                  { label: "Kontakty", href: "/kontakty/" },
                ]}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
