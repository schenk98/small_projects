# Poe Pet App - Source of Truth

## 0) Art & content policy (agreed)

1. **Pet art pipeline (personal project)**  
   - AI-generated raster art is acceptable.  
   - Use **one seed image per species** (or per character line) and **reference that image** when generating other moods so fur/markings stay consistent.  
   - Target **true alpha** where possible so a **background layer** can sit underneath the pet on the stage.

2. **Puzzle minigame photos**  
   - Puzzle swap needs **detailed** source images (photo or high-detail render) so tiles stay interesting.  
   - Acceptable sources: **free stock** (e.g. Unsplash/Pexels) **or** AI-generated placeholders. URLs live in `frontend/src/App.tsx` (`startPuzzle`).

3. **Naming: “playing dead”**  
   - The hunger-critical mood is stored as `playing_dead` (not `dead`). UI copy: **Playing dead** (laying down, cartoon X eyes, non-violent).

---

## 1) Product Baseline

The app is a virtual-pet loop with:
- authentication
- pet stats simulation (`hunger`, `happiness`, `energy`)
- minigames for coins
- consumables shop + **cosmetics** shop (backgrounds / foregrounds; alternate mood art hooks)
- inventory usage
- settings/developer tools

## 2) Center Pet Visual System (Current Implementation)

### 2.1 Starter species
- `dog`
- `cat`

Users can switch species in `Customize`.

### 2.2 Mood image model

The center of the page shows one transparent-background image of the current species and current mood.

Default mood slots:
- `happy`
- `sad`
- `hungry`
- `tired`
- `playing_dead` (UI: *Playing dead* — e.g. laying down with X eyes; shown when `hunger <= 0`)

For each starter species, there is one default starter image per mood (10 total mood PNGs). Additional purchasable `PET_MOOD` rows can be added later for extra expressions per species.

### 2.3 Mood thresholds (agreed profile)

Rules are deterministic and evaluated in this order:
1. `playing_dead` when `hunger <= 0` (hard rule)
2. `hungry` when `hunger < 20`
3. `tired` when `energy < 20`
4. `sad` when `happiness < 30`
5. otherwise `happy`

Priority if multiple stats are low:
- `hungry` > `tired` > `sad` > `happy` (with `playing_dead` always first)

### 2.4 Slot behavior

Each mood is a slot. Slot value can be:
- `none` (use default mood image for current species)
- explicit `assetCode` override

UI lets the user assign **owned or starter** mood art per slot in `Customize`, and equip **one background** + **one foreground** for the stage (only starter or purchased assets).

## 3) Database Model (Current)

### 3.1 `pets`
Fields:
- `userId`
- `hunger`
- `happiness`
- `energy`
- `lastSimulationAt`
- `activeEffects[]`
- `speciesCode` (`dog` | `cat`)
- `moodAssetCodes` (map: mood -> assetCode)
- `ownedVisualAssetCodes` (string[] — unlocked cosmetic / alternate mood `pet_visual_assets.code` values)
- `equippedBackgroundAssetCode` (nullable)
- `equippedForegroundAssetCode` (nullable)

### 3.2 `pet_visual_assets`
Collection for visual catalog entries.

Fields:
- `code` (unique asset code)
- `assetType`: `PET_MOOD` | `BACKGROUND` | `FOREGROUND`
- `speciesCode`: `dog` | `cat` for mood rows; `all` for scene layers
- `moodCode`: for `PET_MOOD`: `happy` / `sad` / `hungry` / `tired` / `playing_dead`; otherwise `""` for scene assets
- `label`
- `imagePath` (frontend public path; starter mood assets are **PNG** raster files)
- `starter` (bool)
- `active` (bool)

### 3.3 `shop_items`
- `CONSUMABLE` — food/boosts; increments `inventory_items` on purchase
- `COSMETIC` — one-time purchase; `effects` contains `{ kind: "GRANT_VISUAL", visualAssetCode: "<pet_visual_assets.code>" }`; grants into `pets.ownedVisualAssetCodes` (no consumable inventory row)

## 4) API (Current)

### Existing game APIs
- dashboard, shop, inventory, minigames endpoints remain active

### New pet visual APIs
- `GET /api/pet-visuals/catalog` — all active rows (`PET_MOOD`, `BACKGROUND`, `FOREGROUND`), sorted
- `POST /api/pet-visuals/species` with `{ speciesCode }`
- `POST /api/pet-visuals/mood-assets` with `{ moodAssetCodes }` — non-starter mood assets require ownership
- `POST /api/pet-visuals/equip-layers` with `{ backgroundAssetCode, foregroundAssetCode }` — use `"none"` to clear a slot

### Error response + status convention (Current)

Error responses are normalized to JSON:
- `{ "error": "<message>" }`

HTTP status codes:
- `400` — validation / rule failure (e.g. not enough coins/energy)
- `401` — missing/invalid access token (enforced by `AuthInterceptor`)
- `403` — forbidden (e.g. non-privileged user calling `/api/dev/*`)
- `404` — unknown catalog items/minigames by code
- `409` — invalid state for the action (e.g. higher/lower guess without active session)

## 5) Frontend UX (Current)

### 5.1 Puzzle swap image sources
- Implemented in `App.tsx` inside `startPuzzle`: **curated Unsplash URLs** (free stock) by default. Swap for other free-stock or self-hosted URLs, or AI-generated assets under `public/`, as needed.

### 5.2 Frontend structure (readability rule)

We keep React code modular and easy to learn:
- `frontend/src/App.tsx`: auth + routing shell
- `frontend/src/game/GameApp.tsx`: game shell (topbar + pet stage + tabs) and page routing
- `frontend/src/game/pages/*`: one file per tab/page (`ShopPage`, `CustomizePage`, etc.)
- `frontend/src/game/pages/minigames/*`: minigames are split by area (modal shell, result sheet, and per-minigame hooks/components)

Rule: avoid “god components”; prefer files/functions under ~250 lines unless truly unavoidable.

### 5.2 Center pet (layered stage)
- Background: `equippedBackgroundAssetCode` → image from catalog (`BACKGROUND`), or default CSS gradient when unset.
- Pet: current species + derived mood PNG (`PET_MOOD`), alpha-friendly.
- Foreground: optional `equippedForegroundAssetCode` (`FOREGROUND`), drawn above the pet.

`Customize` tab:
- species (`dog` / `cat`)
- scene: background + foreground selects (starter or owned only)
- mood slots: `happy` / `sad` / `hungry` / `tired` / `playing_dead` — options limited to **starter + owned** assets for that mood/species

**Pet mood PNGs:**  
- `frontend/public/pet-assets/dog/*.png`  
- `frontend/public/pet-assets/cat/*.png`  

**Scene cosmetics (SVG or raster paths in catalog):**  
- Backgrounds: `frontend/public/cosmetic-staging/backgrounds/*.svg`  
- Foregrounds: `frontend/public/cosmetic-staging/foregrounds/*.svg`

## 6) Seed & Cleanup

### Seed
- `mongodb/init/01-seed.js` seeds:
  - consumables
  - minigames
  - `pet_visual_assets` (starter moods + backgrounds + foregrounds)
  - `shop_items` cosmetics

**Existing MongoDB:** re-run `mongodb/init/01-seed.js` for a clean catalog, or run `mongodb/scripts/migrate-playing-dead-and-cosmetics.js` then merge in new shop/visual rows manually.

### Cleanup script
- `mongodb/scripts/cleanup-legacy-pet-cosmetics.js` is kept for old-db cleanup and should continue removing deprecated cosmetic remnants if present.

## 7) Future Plan (Documented, Not Yet Implemented)

### 7.1 More purchasable content
- Additional `PET_MOOD` rows per species (extra expressions) and shop `COSMETIC` rows pointing at them.
- Optional **preview thumbnails** in the shop and richer equip UX.

### 7.2 More species / “other pets”
- Additional `speciesCode` values and matching `PET_MOOD` starter sets (shop unlock or progression).
- Color/variant lines per species (e.g. white cat, black cat, orange cat) and equivalent sets for other species.

### 7.3 More mood states
- Add additional emotional states and event-driven expressions.
- Keep threshold-based default fallback for all required core states.

### 7.4 AI Pet (planned) — conversational personality + locally-run model on AWS

Goal: turn the pet into a character that can “talk” in a consistent voice, driven by an AI model that runs under the owner's credentials.

#### 7.4.1 UX spec (frontend)
- Add a **chat panel** (prompt textbox + send button + answer area styled like a conversation).
- AI responses must be written **in the pet’s voice** (personality, tone, quirks).
- The prompt/response UI is part of the main game experience (exact placement TBD):
  - option A: new tab (e.g. “Chat”)
  - option B: panel under the center pet stage
- Session behavior (TBD):
  - short rolling context (last N turns)
  - optional “reset conversation” button

#### 7.4.2 AI integration contract (between pet app and AI service)

We will run the model as a **separate standalone service** (no frontend) and call it from this pet app.

Minimum API contract (shape, not implementation):
- `POST /v1/chat` with:
  - `userId` (or a stable pseudonymous id)
  - `petStateSnapshot` (species, derived mood, stats %, active effects summary, equipped cosmetics summary)
  - `conversation` (last N turns)
  - `userPrompt`
  - `personaVersion` (so we can evolve the prompt without breaking old behavior)
- Response:
  - `assistantText`
  - optional `usage` (tokens, latency), for debugging and cost tracking
  - optional `safetyFlags` / `refusalReason` (future)

Auth between services (TBD):
- simplest: shared secret / API key in header
- better: mTLS or signed JWT between services
- hard rule: AI service is private (not open internet), only callable by our backend / infra

#### 7.4.3 Hosting spec (planned AWS architecture)

We will run *two* apps on AWS:
- **pet app stack** (frontend + Java backend + Mongo or managed DB)
- **AI service** (Python backend, model weights on disk, private API)

Connectivity:
- pet backend calls AI service over private network (VPC / security group allowlist)
- frontend never calls AI service directly (keeps credentials private)

Observability goals (TBD):
- log per-request latency, model name, prompt size, response size
- capture failure modes cleanly (timeouts, overload, model errors)

#### 7.4.4 Roadmap (detailed steps)

Phase A — Product/spec alignment (docs-first)
- Define pet “persona” (name, traits, style rules, do/don’t list).
- Decide conversation memory policy (last N turns vs summarized memory).
- Decide where chat lives in the UI (tab vs panel).

Phase B — Choose AI model + runtime (research spike)
- Decide the model family and size target (small, offline, CPU/GPU needs).
- Decide inference runtime:
  - `llama.cpp`-style (GGUF) vs `transformers` (HF) vs vendor runtime
- Decide quantization level / hardware:
  - CPU-only vs GPU (cost/latency trade-off)
- Decide “no internet access” constraints:
  - confirm model is self-contained (no tools, no browsing)

Phase C — Build the AI service (standalone Python backend)
- Create a minimal HTTP API that can:
  - load model weights once at boot
  - accept chat request payload
  - return assistant text
- Add authentication (API key / JWT) and rate limiting.
- Add timeouts and “graceful failure” responses.

Phase D — Connect pet app to AI service
- Backend: add a server-side endpoint that:
  - gathers the pet snapshot + optional conversation context
  - calls the AI service
  - returns normalized `{ reply: string }` to the frontend
- Frontend: chat UI wires to backend endpoint, shows loading/error states.

Phase E — Quality + safety + cost control
- Add tests:
  - contract tests for AI endpoint shape
  - basic “persona adherence” regression checks (snapshot tests / golden responses; best-effort)
- Add guardrails:
  - max prompt length
  - max response length
  - content filters/refusals (lightweight, since it’s a personal project)
- Add caching / dedupe if needed (optional).

#### 7.4.5 Open questions (must be answered before implementation)

Model choice:
- What model do we actually pick? (initial speculation: “MiniMax” vs “Gemma light”, but we need current scouting)
- Which license constraints apply? Can we run it on AWS with our intended usage?
- What quality bar do we want (short cute replies vs longer roleplay)?

Hardware + costs:
- CPU-only acceptable? If not, which GPU instance class and expected hourly cost?
- Target latency: what is “good enough” for chat (e.g. < 2s vs < 10s)?

Security:
- How do we authenticate pet backend → AI service?
- Should user prompts be stored? If yes, where and for how long?

Data + privacy:
- Do we send full pet state, or a minimal summary?
- Do we persist chat history in Mongo, or keep it ephemeral in the frontend only?

Reliability:
- What happens when AI service is down? (fallback message in pet voice)
- Rate limiting: per user, per IP, global?

Dev workflow:
- Local dev: do we run a tiny model locally, or stub the AI service?

## 8) Development Rule

When changing behavior:
1. Update this file first.
2. Update seeds/schema/API docs.
3. Implement code.
4. Verify frontend build and backend compile.
