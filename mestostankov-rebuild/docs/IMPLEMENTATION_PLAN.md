## Step-by-step implementation plan

This plan is designed so we can stop at any step and resume deterministically.

### Milestone A — “Read-only mirror” (URL-preserving, search-first shell)

#### A1. Freeze the scrape as an importable dataset (DONE)

- Input: `support/scraper/out/20260508-202940/`
- Output: `normalized/pages.jsonl`, `normalized/assets.jsonl`, `index/url_map.jsonl`

#### A2. Build the product frontend skeleton (Next.js App Router)

- Create `product/frontend/` Next.js app
- Add global layout aligned to vision:
  - large, accessible typography (base ~18px)
  - high contrast palette
  - “search-first” header with prominent search box
  - footer with accessibility/legal links

Stop point: app boots locally and shows a placeholder homepage.

#### A3. Implement a local content store backed by the scrape dataset

- Add a small library that:
  - loads `pages.jsonl` + `url_map.jsonl` into memory (dev) or into a lightweight local cache file
  - provides `getPageByUrl(fullUrl)` and `searchPages(query)` (simple substring in phase A)
  - normalizes URLs the same way the scraper did (drop fragments, sort query params, remove utm)

Stop point: `GET /api/debug/page?url=...` (or server component helper) returns a page record.

#### A4. URL-preserving router (“mirror mode”)

- Create a catch-all route that maps incoming requests to the scraped canonical URL:
  - match `pathname + search` to `url_map`
  - render a “Page” view using extracted main content
  - fallback: show a helpful 404 + site search

Stop point: visiting `/mesto/soucasnost/` renders the scraped content for that URL.

#### A5. Improve content extraction (main content vs. navigation noise)

- Add a second-pass extractor:
  - prefer `<main>` / content container candidates
  - strip repeated chrome (menu, footer widgets)
  - keep headings + body paragraphs + lists + downloads

Stop point: pages look readable and not polluted by repeated menu text.

#### A6. Content-type refinement + “key views”

Based on content (not old menu location), promote into dedicated views:

- News list + item
- Events list + item
- Official board list + item (with “active/expired”)
- Document/download pages
- Contacts

Stop point: top navigation and quick links lead to these key views with consistent UI.

#### A7. Accessibility extras (keep/restore)

- **TTS (“předčítání”)**: add a simple read-aloud control for page content (Web Speech API as baseline; later dedicated TTS).
- **Multi-language**:
  - baseline: language links that open Google Translate for the current page, plus an obvious “Zpět na češtinu” action
  - later: proper i18n routing + curated translations for critical pages

### Milestone B — “Structured data + real search”

#### B1. Define a stable data model

- Tables/collections (roadmap-aligned):
  - `pages`, `news`, `events`, `official_board`, `documents`, `assets`
- Decide DB: Supabase (Postgres) vs MongoDB

#### B2. Build an importer from the scrape dataset

- Parse `pages.jsonl` into structured records
- Extract dates, titles, attachments
- De-duplicate assets

#### B3. Add Meilisearch

- Create index + sync job
- Implement global search UI (instant results)
- Add Czech synonyms set

### Milestone C — “Publishing workflow”

- Keep staff UX “acceptable” by default:
  - Option 1: minimal admin UI (upload docs, set expiry)
  - Option 2: use Supabase dashboard
  - Option 3: later full CMS replacement

