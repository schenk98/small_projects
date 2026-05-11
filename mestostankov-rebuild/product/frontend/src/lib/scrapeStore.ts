import fs from "node:fs";
import path from "node:path";

import { normalizeUrlForLookup } from "@/lib/url";
import { extractMainHtml } from "@/lib/extractMainHtml";
import { normalizeText } from "@/lib/normalizeText";

export type ScrapedPage = {
  id: string;
  url: string;
  canonical_url?: string | null;
  lang?: string | null;
  title?: string | null;
  fetched_at_iso: string;
  fetch_kind: "http" | "browser";
  http_status?: number | null;
  content_type: string;
  content_subtype?: string | null;
  headings: string[];
  text: string;
  excerpt?: string | null;
  discovered_links: string[];
  discovered_assets: string[];
  raw_html_path: string;
  raw_text_path: string;
  raw_meta_path: string;
  _search_blob?: string;
};

type UrlMapRow = { url: string; page_id: string; final_url?: string | null };

type Store = {
  byId: Map<string, ScrapedPage>;
  byUrl: Map<string, string>; // normalized full URL -> page_id
};

let cached: Store | null = null;

function getDatasetRoot(): string {
  const env = process.env.SCRAPE_DATASET_ROOT;
  if (env) return env;

  // Default to the current successful run. We can make this configurable later.
  return path.join(
    process.cwd(),
    "..",
    "..",
    "support",
    "scraper",
    "out",
    "20260508-202940",
  );
}

function resolveDatasetPath(datasetRoot: string, originalPath: string): string {
  // Scraper currently writes absolute paths into `pages.jsonl` which are machine-specific
  // (e.g. Windows `C:\...`). In production we mount the dataset at `datasetRoot`, so we
  // rewrite those paths to point into the mounted folder.
  //
  // If the original path already exists (e.g. local dev), keep it as-is.
  try {
    if (originalPath && fs.existsSync(originalPath)) return originalPath;
  } catch {
    // ignore fs errors and attempt rewrite below
  }

  const norm = (originalPath ?? "").replace(/\\/g, "/");
  const htmlBase = path.basename(norm);

  if (norm.includes("/raw/pages/")) {
    return path.join(datasetRoot, "raw", "pages", htmlBase);
  }
  if (norm.includes("/raw/text/")) {
    return path.join(datasetRoot, "raw", "text", htmlBase);
  }
  if (norm.includes("/raw/meta/")) {
    return path.join(datasetRoot, "raw", "meta", htmlBase);
  }

  // Fallback: if the path contains a dataset-relative segment like `/raw/...`,
  // rebuild from that segment.
  const rawIdx = norm.lastIndexOf("/raw/");
  if (rawIdx !== -1) return path.join(datasetRoot, norm.slice(rawIdx + 1));

  return path.join(datasetRoot, htmlBase);
}

function readJsonLinesFile(filePath: string): unknown[] {
  const raw = fs.readFileSync(filePath, "utf-8");
  const lines = raw.split(/\r?\n/).filter(Boolean);
  return lines.map((l) => JSON.parse(l));
}

export function getScrapeStore(): Store {
  if (cached) return cached;

  const datasetRoot = getDatasetRoot();
  const pagesPath = path.join(datasetRoot, "normalized", "pages.jsonl");
  const urlMapPath = path.join(datasetRoot, "index", "url_map.jsonl");

  const pages = (readJsonLinesFile(pagesPath) as ScrapedPage[]).map((p) => ({
    ...p,
    raw_html_path: resolveDatasetPath(datasetRoot, p.raw_html_path),
    raw_text_path: resolveDatasetPath(datasetRoot, p.raw_text_path),
    raw_meta_path: resolveDatasetPath(datasetRoot, p.raw_meta_path),
  }));
  const urlMap = readJsonLinesFile(urlMapPath) as UrlMapRow[];

  const byId = new Map<string, ScrapedPage>();
  for (const p of pages) byId.set(p.id, p);

  const byUrl = new Map<string, string>();
  for (const row of urlMap) {
    const norm = normalizeUrlForLookup(row.url);
    byUrl.set(norm, row.page_id);
    if (row.final_url) {
      byUrl.set(normalizeUrlForLookup(row.final_url), row.page_id);
    }
  }

  cached = { byId, byUrl };
  return cached;
}

export function lookupPageByFullUrl(fullUrl: string): ScrapedPage | null {
  const store = getScrapeStore();
  const norm = normalizeUrlForLookup(fullUrl);
  const id = store.byUrl.get(norm);
  if (id) return store.byId.get(id) ?? null;

  // Trailing-slash robustness: some pages vary in the dataset.
  // Try toggling trailing slash for non-file paths.
  try {
    const u = new URL(fullUrl);
    const lastSeg = u.pathname.split("/").filter(Boolean).at(-1) ?? "";
    if (!lastSeg.includes(".")) {
      const altPath = u.pathname.endsWith("/") ? u.pathname.slice(0, -1) : `${u.pathname}/`;
      const alt = new URL(`${altPath}${u.search}${u.hash}`, u.origin).toString();
      const altId = store.byUrl.get(normalizeUrlForLookup(alt));
      if (altId) return store.byId.get(altId) ?? null;
    }
  } catch {
    // ignore
  }

  return null;
}

export function loadPageMainHtml(page: ScrapedPage): { title?: string; html: string } {
  // raw_html_path is absolute, produced by the scraper.
  const rawHtml = fs.readFileSync(page.raw_html_path, "utf-8");
  return extractMainHtml(rawHtml);
}

export function getPageSearchBlob(page: ScrapedPage): string {
  if (page._search_blob) return page._search_blob;
  const rawHtml = fs.readFileSync(page.raw_html_path, "utf-8");
  const main = extractMainHtml(rawHtml);
  // Convert main HTML to text for search to avoid indexing nav/chatbot boilerplate.
  const textOnly = main.html.replace(/<[^>]+>/g, " ");
  const blob = `${page.title ?? ""}\n${page.url ?? ""}\n${textOnly}`;
  page._search_blob = normalizeText(blob);
  return page._search_blob;
}

export function getPageMainText(page: ScrapedPage): string {
  const rawHtml = fs.readFileSync(page.raw_html_path, "utf-8");
  const main = extractMainHtml(rawHtml);
  return main.html
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function getPageSearchSnippet(page: ScrapedPage, maxLen = 220): string | null {
  const txt = getPageMainText(page);
  if (!txt) return null;

  // Defensive removals if any boilerplate leaks into extracted content.
  const cleaned = txt
    .replace(/JavaScript je vypnut[^\.\!]*[\.\!]/gi, "")
    .replace(/Pro navigaci přejděte na[^\.\!]*[\.\!]/gi, "")
    .replace(/\s+/g, " ")
    .trim();

  if (!cleaned) return null;
  return cleaned.length > maxLen ? `${cleaned.slice(0, maxLen - 1)}…` : cleaned;
}

export function loadRawHtml(page: ScrapedPage): string {
  return fs.readFileSync(page.raw_html_path, "utf-8");
}

