"use client";

import { useEffect, useId, useMemo, useState, useSyncExternalStore } from "react";

import type { TranslationAvailability } from "@/lib/contentTranslations";

type GoogleTranslateOptions = {
  pageLanguage: string;
  includedLanguages: string;
  autoDisplay: boolean;
};

type GoogleTranslateElementCtor = new (options: GoogleTranslateOptions, elementId: string) => unknown;

type GoogleTranslateAPI = {
  translate: {
    TranslateElement: GoogleTranslateElementCtor;
  };
};

declare global {
  interface Window {
    google?: GoogleTranslateAPI;
    googleTranslateElementInit?: () => void;
  }
}

function ensureScriptLoaded(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${src}"]`) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Failed to load script")), { once: true });
      if ((existing as HTMLScriptElement & { dataset: { loaded?: string } }).dataset.loaded === "1") resolve();
      return;
    }

    const s = document.createElement("script");
    s.src = src;
    s.async = true;
    s.addEventListener("load", () => {
      (s as HTMLScriptElement & { dataset: { loaded?: string } }).dataset.loaded = "1";
      resolve();
    });
    s.addEventListener("error", () => reject(new Error("Failed to load script")));
    document.head.appendChild(s);
  });
}

function setCookie(name: string, value: string, maxAgeSeconds: number) {
  document.cookie = `${name}=${value}; path=/; max-age=${maxAgeSeconds}; SameSite=Lax`;
}

function clearCookie(name: string) {
  document.cookie = `${name}=; path=/; max-age=0`;
}

function getCookie(name: string): string | null {
  const parts = document.cookie.split(";").map((p) => p.trim());
  const hit = parts.find((p) => p.startsWith(`${name}=`));
  if (!hit) return null;
  return decodeURIComponent(hit.slice(name.length + 1));
}

export function TranslateWidget({
  compact,
  translationAvailability,
}: {
  compact?: boolean;
  translationAvailability?: TranslationAvailability;
} = {}) {
  const [ready, setReady] = useState(false);
  const current = useSyncExternalStore(
    () => () => {},
    () => {
      const c = getCookie("stankov_lang");
      return c === "en" || c === "de" || c === "uk" || c === "cs" ? c : "cs";
    },
    () => "cs",
  );
  const rid = useId();
  const id = useMemo(
    () => `google_translate_element_${rid.replace(/[^a-zA-Z0-9_-]/g, "")}`,
    [rid],
  );

  const avail = translationAvailability ?? { en: false, de: false, uk: false };

  function hasStaticFor(target: "en" | "de" | "uk"): boolean {
    if (target === "en") return avail.en;
    if (target === "de") return avail.de;
    return avail.uk;
  }

  function setLang(target: "cs" | "en" | "de" | "uk") {
    setCookie("stankov_lang", target, 60 * 60 * 24 * 365);

    if (target === "cs") {
      clearCookie("googtrans");
      document.documentElement.lang = "cs";
      window.location.reload();
      return;
    }

    if (hasStaticFor(target)) {
      clearCookie("googtrans");
    } else {
      const v = `/cs/${target}`;
      // Google Translate expects the raw value (not URL-encoded) in the cookie.
      setCookie("googtrans", v, 60 * 60 * 24 * 365);
    }

    document.documentElement.lang = target;
    window.location.reload();
  }

  useEffect(() => {
    const init = () => {
      try {
        if (!window.google?.translate?.TranslateElement) return;
        const host = document.getElementById(id);
        if (!host) return;
        if (host.getAttribute("data-initialized") === "1") return;

        host.setAttribute("data-initialized", "1");
        new window.google.translate.TranslateElement(
          {
            pageLanguage: "cs",
            includedLanguages: "cs,en,de,uk",
            autoDisplay: false,
          },
          id,
        );
        setReady(true);
      } catch {
        // ignore
      }
    };

    window.googleTranslateElementInit = init;
    const src = "https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit";
    ensureScriptLoaded(src)
      .then(() => init())
      .catch(() => {
        // ignore
      });
  }, [id]);

  const btnClass = compact ? "header-pill-btn" : "btn";
  const pad = compact ? "0.25rem 0.4rem" : "0.35rem 0.55rem";

  const flag = current === "cs" ? "🇨🇿" : current === "en" ? "🇬🇧" : current === "de" ? "🇩🇪" : "🇺🇦";

  if (compact) {
    return (
      <details className="lang-compact">
        <summary className="header-icon-btn" aria-label="Jazyk" title="Jazyk">
          {flag}
        </summary>
        <div className="lang-panel card" style={{ padding: "0.6rem 0.75rem" }}>
          <div style={{ display: "grid", gap: "0.4rem" }}>
            <button type="button" className="lang-item" onClick={() => setLang("cs")}>
              🇨🇿 Čeština
            </button>
            <button type="button" className="lang-item" onClick={() => setLang("en")}>
              🇬🇧 English
            </button>
            <button type="button" className="lang-item" onClick={() => setLang("de")}>
              🇩🇪 Deutsch
            </button>
            <button type="button" className="lang-item" onClick={() => setLang("uk")}>
              🇺🇦 Українська
            </button>
          </div>
        </div>
        {/* Must exist for Google Translate to initialize (even in compact mode). */}
        <div id={id} style={{ display: "none" }} />
      </details>
    );
  }

  return (
    <div
      className={compact ? "header-pill" : "card"}
      style={{
        padding: compact ? "0.25rem 0.4rem" : "0.5rem 0.75rem",
        display: "flex",
        gap: compact ? "0.4rem" : "0.75rem",
        alignItems: "center",
      }}
    >
      {compact ? null : <span style={{ fontWeight: 800, color: "var(--muted)" }}>Jazyk</span>}
      <div style={{ display: "flex", gap: "0.35rem", flexWrap: "wrap" }}>
        <button type="button" className={btnClass} style={{ padding: pad }} onClick={() => setLang("cs")}>
          CZ
        </button>
        <button type="button" className={btnClass} style={{ padding: pad }} onClick={() => setLang("en")}>
          EN
        </button>
        <button type="button" className={btnClass} style={{ padding: pad }} onClick={() => setLang("de")}>
          DE
        </button>
        <button type="button" className={btnClass} style={{ padding: pad }} onClick={() => setLang("uk")}>
          UK
        </button>
      </div>
      <div id={id} style={{ minHeight: 28, display: compact ? "none" : "block" }} />
      {!ready && !compact ? <span style={{ fontSize: "0.85rem", color: "var(--muted)" }}>Načítám…</span> : null}
    </div>
  );
}
