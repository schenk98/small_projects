import Image from "next/image";
import Link from "next/link";
import { ThemeToggle } from "@/components/ThemeToggle";
import { TtsButton } from "@/components/TtsButton";
import { TranslateWidget } from "@/components/TranslateWidget";
import { PrintPdfButton } from "@/components/PrintPdfButton";
import type { TranslationAvailability } from "@/lib/contentTranslations";

type SiteHeaderProps = {
  query?: string;
  /** Reserved for analytics / translate public URL parity */
  currentUrl?: string;
  localHref?: string;
  translationAvailability?: TranslationAvailability;
};

export function SiteHeader({ query, translationAvailability }: SiteHeaderProps) {
  return (
    <header className="site-header">
      <div className="container" style={{ paddingTop: "0.85rem", paddingBottom: "1rem" }}>
        <div>
          <div className="header-row">
            <Link href="/" className="brand">
              <Image
                src="https://www.mestostankov.cz/skins/stankov_lego2/images/crest.png"
                alt="Erb města Staňkov"
                width={44}
                height={44}
                style={{ width: 44, height: 44 }}
              />
              <div>
                <div className="brand-title">Město Staňkov</div>
                <div className="brand-subtitle">Oficiální stránky (modernizace)</div>
              </div>
            </Link>

            <nav className="nav-links" aria-label="Hlavní navigace">
              <Link href="/o-stankove">O Staňkově</Link>
              <Link href="/co-se-deje">Co se děje</Link>
              <Link href="/co-potrebujete">Co potřebujete</Link>
              <Link href="/urad-a-samosprava">Úřad</Link>
            </nav>

            <details className="nav-mobile">
              <summary className="header-pill-btn" style={{ padding: "0.35rem 0.6rem" }}>
                Menu
              </summary>
              <div className="nav-mobile-panel card" style={{ padding: "0.75rem" }}>
                <div style={{ display: "grid", gap: "0.5rem" }}>
                  <Link href="/o-stankove">O Staňkově</Link>
                  <Link href="/co-se-deje">Co se děje</Link>
                  <Link href="/co-potrebujete">Co potřebujete</Link>
                  <Link href="/urad-a-samosprava">Úřad</Link>
                </div>
              </div>
            </details>

            <div className="header-actions">
              <TranslateWidget compact translationAvailability={translationAvailability} />
              <PrintPdfButton compact />
              <ThemeToggle />
            </div>
          </div>

          <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", marginTop: "0.65rem" }}>
            <TtsButton />
          </div>

          <form action="/hledat" className="search-row">
            <input
              name="q"
              defaultValue={query}
              placeholder="Hledat (např. odpad, popelnice, úřední deska, kino…)…"
              className="search-input"
            />
            <button
              type="submit"
              className="btn"
            >
              Hledat
            </button>
          </form>
        </div>
      </div>
    </header>
  );
}

