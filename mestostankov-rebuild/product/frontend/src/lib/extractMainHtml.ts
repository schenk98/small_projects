import * as cheerio from "cheerio";
import sanitizeHtml from "sanitize-html";

const SCRAPE_ORIGIN = process.env.SCRAPE_ORIGIN ?? "https://www.mestostankov.cz";

function rewriteInternalLinksToLocal(html: string): string {
  const $ = cheerio.load(html);

  $("a[href]").each((_, el) => {
    const href = $(el).attr("href");
    if (!href) return;
    try {
      const u = new URL(href, SCRAPE_ORIGIN);
      if (u.origin === SCRAPE_ORIGIN) {
        $(el).attr("href", `${u.pathname}${u.search}${u.hash}`);
      }
    } catch {
      // ignore
    }
  });

  // Assets must stay absolute to render in our app (otherwise they try localhost paths).
  for (const [selector, attr] of [
    ["img[src]", "src"],
    ["source[src]", "src"],
    ["video[src]", "src"],
    ["audio[src]", "src"],
  ] as const) {
    $(selector).each((_, el) => {
      const v = $(el).attr(attr);
      if (!v) return;
      try {
        const u = new URL(v, SCRAPE_ORIGIN);
        if (u.origin === SCRAPE_ORIGIN) {
          $(el).attr(attr, u.toString());
        }
      } catch {
        // ignore
      }
    });
  }

  return $.root().html() ?? html;
}

export function extractMainHtml(rawHtml: string): { title?: string; html: string } {
  const $ = cheerio.load(rawHtml);

  const title = $("title").first().text().trim() || undefined;

  // Prefer content containers observed on the current site
  const candidates = [
    ".editor_content.readable",
    ".gcm-main .multipage",
    ".gcm-main",
    ".gcm-content",
    ".container_bg .gcm-main-container",
    "main",
    "article",
    "#content",
  ];

  let $root: ReturnType<typeof $> | null = null;
  for (const sel of candidates) {
    const found = $(sel).first();
    if (!found.length) continue;

    // Some pages include an empty `.editor_content.readable` and real content in `.module_content`.
    const textLen = found.text().replace(/\s+/g, " ").trim().length;
    if (textLen < 40) continue;

    $root = found;
    break;
  }

  if (!$root) {
    // Last resort: fall back to broader containers even if they are short.
    const fallbackSel = [".module_content", ".gcm-main", ".gcm-content", "body"];
    for (const sel of fallbackSel) {
      const found = $(sel).first();
      if (found.length) {
        $root = found;
        break;
      }
    }
    if (!$root) $root = $.root();
  }

  // Remove obvious chrome/noise
  $root.find("script, style, noscript").remove();
  $root.find("header, footer, nav").remove();
  $root
    .find(
      ".gcm-links, .gcm-info__wrap, .breadcrumb, .breadcrumbs, .gcm-breadcrumb, .path, .cookie, .cookies, .gcm-footer",
    )
    .remove();
  $root.find("form").remove();

  // Remove side widgets that repeat across pages
  $root
    .find(
      ".gcm-right, .gcm-left, .tpl_mini_module, .mini_module, .module_mini, .calendar_mini, .gcm-aside",
    )
    .remove();

  // A lot of pages wrap each widget in `.editor .content`
  const html = $root.html() ?? "";
  const rewritten = rewriteInternalLinksToLocal(html);

  const cleaned = sanitizeHtml(rewritten, {
    allowedTags: sanitizeHtml.defaults.allowedTags.concat([
      "img",
      "h1",
      "h2",
      "h3",
      "h4",
      "figure",
      "figcaption",
      "table",
      "thead",
      "tbody",
      "tr",
      "th",
      "td",
    ]),
    allowedAttributes: {
      a: ["href", "title", "target", "rel"],
      img: ["src", "alt", "title", "width", "height", "loading"],
      // Strip classes/styles from legacy HTML to prevent accidental collisions with our UI classes (e.g. `.btn`),
      // and to avoid "everything looks bold/button-like" artifacts.
      "*": ["id"],
    },
    // Keep relative URLs; do not allow javascript: URLs
    allowedSchemes: ["http", "https", "mailto", "tel"],
    allowProtocolRelative: false,
  });

  return { title, html: cleaned };
}

