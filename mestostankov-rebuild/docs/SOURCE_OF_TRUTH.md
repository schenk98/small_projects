## Source of truth — Město Staňkov web rebuild

This document is the canonical reference for requirements, decisions, constraints, and Q&A captured during development.

### Mission

Recreate and modernize the public website at `https://www.mestostankov.cz/` with a professional, accessible, fast experience while **preserving existing URLs** as much as possible (SEO-friendly).

### Non-goals (for initial phase)

- Replacing the town staff publishing experience immediately (current solution is acceptable; we’ll take inspiration from it).

### Key decisions (so far)

- **Project folder**: `mestostankov-rebuild/`
- **URL strategy**: preserve existing URLs/routing as much as possible
- **Product stack**: Next.js (React)
- **Admin/CMS**: keep staff experience comparable to current; don’t disrupt publishing workflow early

### Current site observations (seed)

From the homepage/navigation snapshot:

- **Languages present**: CZ primary, and links for EN/DE/FR/ES/RU/UK (site-wide language switcher)
- **Primary sections**:
  - Město (Současnost, Historie, Sport, Kultura, …)
  - Informace pro občany (Aktuality, Kalendář akcí, …)
  - Úřad (Úřední hodiny, Úřední deska, ePodatelna, …)
  - Fotogalerie
  - Kontakty
- **Homepage modules** (examples): Aktuality, Kalendář akcí, Úřední hodiny, “Přihlášení k odběru zpráv”, Rezervační systém, senior mode, etc.

### Open questions (to be answered and then recorded here)

- Exact target hosting/runtime (self-hosted VPS? managed? municipality infra?)
- Whether we need to support the “senior simplified” module as a dedicated view or via accessibility controls
- Which parts must be editable vs. can be mirrored/imported regularly

### Q&A log

#### 2026-05-08

- **Q**: What should the new project folder be named in this repo?
  - **A**: `mestostankov-rebuild`
- **Q**: How close should we match the current site structure/URLs?
  - **A**: Keep existing URLs/routing as much as possible (SEO-friendly)
- **Q**: Preferred stack for the final product?
  - **A**: Next.js
- **Q**: Do you want a CMS/editor experience for town staff?
  - **A**: Take inspiration from current solution — staff experience is already acceptable as it is now
- **Q**: Can you move the roadmap file and the HTML vision folder into the repo so I can read them?
  - **A**: Yes, will move

### Scraping dataset (support/scraper)

#### Successful scrape run

- **Run folder**: `mestostankov-rebuild/support/scraper/out/20260508-202940/`
- **Config** (`run.json`): max_pages=2500, concurrency=8, delay_ms=200, download_assets=true, browser_fallback=false
- **Outputs**:
  - `normalized/pages.jsonl` (~12.7MB): 1 JSON object per crawled HTML page
  - `normalized/assets.jsonl` (~7.3MB): referenced assets + downloaded documents index
  - `index/url_map.jsonl` (~248KB): url → page_id (stable)
  - `raw/pages/*.html`, `raw/text/*.txt`, `raw/meta/*.json`: per-page raw artifacts

#### Coverage summary (pages.jsonl content_type counts)

- `homepage`: 1
- `static_page`: 631
- `official_board_list`: 185
- `events_list`: 32
- `gallery_list`: 16
- `news_list`: 10
- `contact`: 7

#### Known limitations (to address)

- **Content extraction** is currently “full page text” (includes navigation, repeated footer widgets). We’ll add a second pass to extract the primary article body more cleanly.
- **Item-level typing** (e.g., `news_item`, `events_item`, `official_board_item`) is under-classified in the current run; these pages exist but are mostly counted as `static_page`. We’ll refine classification once we start importing into the product.

### MVP cleanup notes (2026-05-09)

- **Search quality**: switched search indexing to use extracted *main content* HTML (not full-page `text`) to avoid junk matches (header/menu/chatbot boilerplate).
- **Legacy HTML sanitization**: stripped `class`/`style` attributes from mirrored HTML to avoid collisions with our UI classes (e.g. `.btn` causing “button-like” artifacts in content).
- **URL robustness**: added trailing-slash fallback in URL lookup so `/path` and `/path/` both resolve when the dataset differs.
- **Homepage copy**: removed “Najděte informace do 3 kliknutí…” line per MVP simplification request.

