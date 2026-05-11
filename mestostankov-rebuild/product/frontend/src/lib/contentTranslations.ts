import raw from "@/data/content-translations.json";

export type HotLang = "en" | "de" | "uk";

export type TranslationAvailability = {
  en: boolean;
  de: boolean;
  uk: boolean;
};

type Entry = Partial<Record<HotLang, string>>;

type Store = Record<string, Entry>;

function loadStore(): Store {
  return raw as Store;
}

const store = loadStore();

export function getTranslationAvailability(key: string): TranslationAvailability {
  const e = store[key];
  return {
    en: Boolean(e?.en?.trim()),
    de: Boolean(e?.de?.trim()),
    uk: Boolean(e?.uk?.trim()),
  };
}

/** Sanitized HTML fragment for main content (trusted, maintained by you). */
export function getContentTranslation(key: string, lang: HotLang): string | null {
  const t = store[key]?.[lang];
  const s = t?.trim();
  return s ? s : null;
}

export function parseStankovLang(value: string | undefined): "cs" | HotLang {
  if (value === "en" || value === "de" || value === "uk") return value;
  return "cs";
}

/** Keys for curated hub routes (optional entries in content-translations.json). */
export const HUB_TRANSLATION_KEYS = {
  oStankove: "hub:o-stankove",
  coSeDeje: "hub:co-se-deje",
  coPotrebujete: "hub:co-potrebujete",
  urad: "hub:urad-a-samosprava",
} as const;

/** When no scraped page id (e.g. search UI). */
export const CURATED_SEARCH_KEY = "curated:hledat" as const;

/** Fallback homepage key if root URL is missing from the dataset. */
export const CURATED_HOME_KEY = "curated:home" as const;
