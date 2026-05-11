"use client";

// Deprecated: replaced by in-place `TranslateWidget` in `SiteHeader`.
export function LanguageMenu({
  sourceUrl,
  localHref,
}: {
  sourceUrl: string;
  localHref: string;
}) {
  const translateBase = "https://translate.google.com/translate";
  const makeTranslateUrl = (lang: string) => {
    const u = new URL(translateBase);
    u.searchParams.set("sl", "cs");
    u.searchParams.set("tl", lang);
    // Translate the public original page (works even during local dev).
    u.searchParams.set("u", sourceUrl);
    return u.toString();
  };

  return (
    <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
      <a
        className="btn"
        style={{ padding: "0.5rem 0.75rem", textDecoration: "none" }}
        href={localHref}
        title="Zpět na češtinu"
      >
        CZ
      </a>
      <a className="btn" style={{ padding: "0.5rem 0.75rem", textDecoration: "none" }} href={makeTranslateUrl("en")} target="_blank" rel="noreferrer">
        EN
      </a>
      <a className="btn" style={{ padding: "0.5rem 0.75rem", textDecoration: "none" }} href={makeTranslateUrl("de")} target="_blank" rel="noreferrer">
        DE
      </a>
    </div>
  );
}

