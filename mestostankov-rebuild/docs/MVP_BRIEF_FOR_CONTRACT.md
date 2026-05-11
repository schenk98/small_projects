## MVP brief (for procurement / contract negotiations)

### What this MVP proves

- **Feasibility**: a modern, accessible shell can be layered over the legacy content while **preserving existing URLs**.
- **Risk reduction**: the town can modernize iteratively without “big bang” migration.
- **Accessibility baseline**:
  - large readable typography
  - high contrast themes (light/dark)
  - text-to-speech (server TTS)
  - translation (cookie-driven, plus Google fallback)
- **Search-first UX**: users can find content without digging through deep menus.

### What is implemented (current MVP scope)

- **URL-preserving mirror** for the scraped legacy site (Next.js catch-all route)
- **Curated hub pages** aligned with the vision:
  - `/o-stankove`
  - `/co-se-deje`
  - `/co-potrebujete`
  - `/urad-a-samosprava`
- **Paginated key lists**:
  - Aktuality
  - Kalendář akcí
  - Úřední deska (with filters)
- **Search** with Czech normalization + synonyms
- **TTS gateway** (Piper) reachable via `/api/tts`
- **Translation**:
  - “hot” languages ready (EN/DE/UK) via `content-translations.json` (instant when populated)
  - Google Translate fallback for coverage

### What remains for a “final” system (what you can ask vendors to price)

#### Structured data + backend

- Database schema: `pages`, `news`, `events`, `official_board`, `documents`, `assets`
- Import pipeline from scraper output
- Attachment/document metadata extraction
- Official board “active vs expired” with dates

#### Search

- Meilisearch (or equivalent) with Czech tuning + synonyms
- Faster, ranked results + analytics

#### Publishing workflow

- Simple admin UI for clerks (upload PDF, set expiry, category, title)
- Roles, audit logs, approvals

#### SEO + redirects

- Redirect map for reorganized content while keeping old URLs valid
- Sitemap, canonical URLs, metadata, robots, analytics

#### Operations

- Deployment (AWS/Vercel), monitoring, backups, disaster recovery
- “One container” or multi-service architecture with clear responsibilities

### How to use this MVP in negotiations

- Ask the vendor to:
  - **reuse** the MVP URL-preserving approach (reduces SEO risk)
  - deliver Phase 2–5 items **incrementally**, with acceptance criteria per milestone
  - keep **accessibility (WCAG)** as a hard requirement
  - demonstrate search quality on Czech queries (typos, diacritics, synonyms)
  - propose the content governance model (who edits what, approvals, archiving)

### Suggested acceptance criteria (examples)

- Lighthouse Accessibility ≥ 95 on key pages
- All legacy URLs continue to resolve (200/301) with no mass 404s
- Official board entries searchable, filterable, with correct validity
- Admin can publish a new notice + PDF in under 2 minutes

