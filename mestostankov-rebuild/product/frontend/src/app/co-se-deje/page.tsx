import { cookies } from "next/headers";

import { SiteHeader } from "@/components/SiteHeader";
import { LinkList } from "@/components/Hubs";
import {
  getContentTranslation,
  getTranslationAvailability,
  HUB_TRANSLATION_KEYS,
  parseStankovLang,
} from "@/lib/contentTranslations";

const HUB_KEY = HUB_TRANSLATION_KEYS.coSeDeje;

export default async function WhatsOnHubPage() {
  const cookieStore = await cookies();
  const lang = parseStankovLang(cookieStore.get("stankov_lang")?.value);
  const translationAvailability = getTranslationAvailability(HUB_KEY);
  const override = lang !== "cs" ? getContentTranslation(HUB_KEY, lang) : null;

  return (
    <div className="min-h-full">
      <SiteHeader
        localHref="/co-se-deje"
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
            <h1 style={{ fontSize: "1.75rem", fontWeight: 900, margin: 0 }}>Co se děje</h1>
            <p style={{ color: "var(--muted)", marginTop: "0.5rem" }}>
              Aktuality, akce, rozhlas, fotky a další.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginTop: "1.25rem" }}>
              <LinkList
                title="Co se děje ve Staňkově"
                links={[
                  { label: "Kalendář akcí", href: "/informace-pro-obcany/kalendar-akci/" },
                  { label: "Aktuality", href: "/informace-pro-obcany/aktuality/" },
                  { label: "Staňkovsko (časopis)", href: "/mesto/casopis-stankovsko/" },
                  { label: "Hlášení místního rozhlasu", href: "/informace-pro-obcany/hlaseni-mistniho-rozhlasu-/" },
                  { label: "Fotogalerie", href: "/fotogalerie-1/" },
                  {
                    label: "Stav Radbuzy (CHMI)",
                    href: "https://hydro.chmi.cz/hppsoldv/hpps_prfdyn.php?seq=307072",
                    external: true,
                  },
                ]}
              />
              <LinkList
                title="Rychlé odkazy"
                links={[
                  { label: "Odběr zpráv (SMS/E-mail)", href: "/informace-pro-obcany/sms-a-emaily/" },
                  { label: "Mobilní aplikace", href: "/kontakty/mobilni-aplikace/" },
                  { label: "Senior web", href: "https://www.mestostankov.cz/seniori/", external: true },
                ]}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
