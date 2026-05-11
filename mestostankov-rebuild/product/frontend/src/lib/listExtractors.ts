import * as cheerio from "cheerio";

const SCRAPE_ORIGIN = process.env.SCRAPE_ORIGIN ?? "https://www.mestostankov.cz";

export type ListItem = {
  title: string;
  href: string; // local href
  dateText?: string;
  description?: string;
  imageUrl?: string;
};

export type OfficialBoardItem = {
  category: string;
  title: string;
  href: string; // local href
  posted?: string;
  removed?: string;
};

function toLocalHref(href: string): string {
  const u = new URL(href, SCRAPE_ORIGIN);
  if (u.origin === SCRAPE_ORIGIN) return `${u.pathname}${u.search}${u.hash}`;
  return href;
}

function absAsset(url: string | undefined): string | undefined {
  if (!url) return undefined;
  try {
    return new URL(url, SCRAPE_ORIGIN).toString();
  } catch {
    return undefined;
  }
}

export function extractAktualityItems(rawHtml: string): ListItem[] {
  const $ = cheerio.load(rawHtml);
  const items: ListItem[] = [];

  // Same markup as homepage widget, but list page also uses feed_item blocks.
  $(".feed_item").each((_, el) => {
    const a = $(el).find("a").first();
    const href = a.attr("href");
    const title = $(el).find(".item_name").first().text().trim() || a.attr("title") || "";
    const dateText = $(el).find(".item_date").first().text().replace(/\s+/g, " ").trim() || undefined;
    const description = $(el).find(".item_description").first().text().trim() || undefined;
    const img = $(el).find("img").first().attr("src");
    if (!href || !title) return;
    items.push({
      title,
      href: toLocalHref(href),
      dateText,
      description,
      imageUrl: absAsset(img),
    });
  });

  // Fallback: if nothing matched, use links under main content
  if (!items.length) {
    $("a[href]").each((_, el) => {
      const href = $(el).attr("href") || "";
      if (!href.includes("/informace-pro-obcany/aktuality/")) return;
      const title = $(el).text().trim();
      if (!title) return;
      items.push({ title, href: toLocalHref(href) });
    });
  }

  // De-dup by href
  const seen = new Set<string>();
  return items.filter((i) => (seen.has(i.href) ? false : (seen.add(i.href), true)));
}

export function extractKalendarItems(rawHtml: string): ListItem[] {
  const $ = cheerio.load(rawHtml);
  const items: ListItem[] = [];

  $(".feed_item").each((_, el) => {
    const a = $(el).find("a").first();
    const href = a.attr("href");
    const title = $(el).find(".item_name").first().text().trim() || a.attr("title") || "";
    const dateText = $(el).find(".item_date").first().text().replace(/\s+/g, " ").trim() || undefined;
    const description = $(el).find(".item_description").first().text().trim() || undefined;
    const img = $(el).find("img").first().attr("src");
    if (!href || !title) return;
    items.push({
      title,
      href: toLocalHref(href),
      dateText,
      description,
      imageUrl: absAsset(img),
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => (seen.has(i.href) ? false : (seen.add(i.href), true)));
}

export function extractOfficialBoardItems(rawHtml: string): OfficialBoardItem[] {
  const $ = cheerio.load(rawHtml);
  const items: OfficialBoardItem[] = [];

  $(".official-desk-list .category.card").each((_, catEl) => {
    const category =
      $(catEl).find(".category-name, .accordion-heading").first().text().replace(/\s+/g, " ").trim() ||
      "Kategorie";

    $(catEl)
      .find(".item")
      .each((_, itemEl) => {
        const a = $(itemEl).find("a.item-href, a").first();
        const href = a.attr("href");
        const title = a.text().trim();
        const posted = $(itemEl).find(".item-date-from").first().text().replace(/\s+/g, " ").trim() || undefined;
        const removed = $(itemEl).find(".item-date-to").first().text().replace(/\s+/g, " ").trim() || undefined;
        if (!href || !title) return;
        items.push({
          category,
          title,
          href: toLocalHref(href),
          posted,
          removed,
        });
      });
  });

  return items;
}

