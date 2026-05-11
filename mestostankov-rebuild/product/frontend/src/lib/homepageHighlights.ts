import * as cheerio from "cheerio";

const SCRAPE_ORIGIN = process.env.SCRAPE_ORIGIN ?? "https://www.mestostankov.cz";

export type HighlightItem = {
  title: string;
  href: string; // local href
  dateText?: string;
  description?: string;
  imageUrl?: string;
};

function toLocalHref(href: string): string {
  const u = new URL(href, SCRAPE_ORIGIN);
  if (u.origin === SCRAPE_ORIGIN) return `${u.pathname}${u.search}${u.hash}`;
  return href;
}

export function extractHomepageHighlights(rawHtml: string): {
  news: HighlightItem[];
  events: HighlightItem[];
} {
  const $ = cheerio.load(rawHtml);

  const news: HighlightItem[] = [];
  const events: HighlightItem[] = [];

  // Homepage: widget_174 contains "Aktuality" feed items.
  $("#widget_174 .feed_item").each((_, el) => {
    const a = $(el).find("a").first();
    const href = a.attr("href");
    const title = $(el).find(".item_name").first().text().trim() || a.attr("title") || "";
    const dateText = $(el).find(".item_date").first().text().replace(/\s+/g, " ").trim() || undefined;
    const description = $(el).find(".item_description").first().text().trim() || undefined;
    const img = $(el).find("img").first().attr("src");
    if (!href || !title) return;
    news.push({
      title,
      href: toLocalHref(href),
      dateText,
      description,
      imageUrl: img ? new URL(img, SCRAPE_ORIGIN).toString() : undefined,
    });
  });

  // Homepage: widget_140 contains "Kalendář akcí" items (class evts_akce)
  $("#widget_140 .feed_item").each((_, el) => {
    const a = $(el).find("a").first();
    const href = a.attr("href");
    const title = $(el).find(".item_name").first().text().trim() || a.attr("title") || "";
    const dateText = $(el).find(".item_date").first().text().replace(/\s+/g, " ").trim() || undefined;
    const description = $(el).find(".item_description").first().text().trim() || undefined;
    const img = $(el).find("img").first().attr("src");
    if (!href || !title) return;
    events.push({
      title,
      href: toLocalHref(href),
      dateText,
      description,
      imageUrl: img ? new URL(img, SCRAPE_ORIGIN).toString() : undefined,
    });
  });

  return { news: news.slice(0, 6), events: events.slice(0, 6) };
}

