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
- `thinking` (transient UI-only mood used while the AI pet chat request is in flight; based on the happy art as the visual reference point)

For each starter species, there is one default starter image per gameplay mood plus a transient `thinking` starter visual for AI chat. Additional purchasable `PET_MOOD` rows can be added later for extra expressions per species.

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
- `speciesCode` (`dog` | `cat` | `penguin` | `fox` | `hamster` | `tiger` | `lion` | `horse` | `parrot` | `unicorn` | `midnight_cat` | `panda` | `goldfish` | `lizard`)
- `ownedSpeciesCodes` (selectable species unlocked for this user; new users start with `dog` and `cat`)
- `moodAssetCodes` (map: mood -> assetCode)
- `ownedVisualAssetCodes` (string[] — unlocked cosmetic / alternate mood `pet_visual_assets.code` values)
- `equippedBackgroundAssetCode` (nullable)
- `equippedForegroundAssetCode` (nullable)

### 3.2 `pet_visual_assets`
Collection for visual catalog entries.

Fields:
- `code` (unique asset code)
- `assetType`: `PET_MOOD` | `BACKGROUND` | `FOREGROUND`
- `speciesCode`: `dog` | `cat` | `penguin` | `fox` | `hamster` | `tiger` | `lion` | `horse` | `parrot` | `unicorn` | `midnight_cat` | `panda` | `goldfish` | `lizard` for mood rows; `all` for scene layers
- `moodCode`: for `PET_MOOD`: `happy` / `sad` / `hungry` / `tired` / `thinking` / `playing_dead`; otherwise `""` for scene assets
- `label`
- `imagePath` (frontend public path; starter mood assets are **PNG** raster files, including the transient `thinking` visuals generated from the happy art style)
- `starter` (bool)
- `active` (bool)

### 3.3 `shop_items`
- `CONSUMABLE` — food/boosts; increments `inventory_items` on purchase
- `COSMETIC` — one-time purchase; `effects` contains `{ kind: "GRANT_VISUAL", visualAssetCode: "<pet_visual_assets.code>" }`; grants into `pets.ownedVisualAssetCodes` (no consumable inventory row)
- `SPECIES` — one-time pet unlock; `effects` contains `{ kind: "GRANT_SPECIES", speciesCode: "<species>" }`; grants into `pets.ownedSpeciesCodes`

### 3.4 Email delivery (current)

Two SMTP paths:

1. **Auth mail** (verification link on register, password reset): the **main backend** uses Spring `JavaMailSender` (`sendMail` in `AppService`) to the configured SMTP host. In local dev this is typically **MailHog** (`spring.mail.*` in `application.yml`).
2. **Gameplay notification mail** (low-hunger reminder, daily AI summary): the main backend calls **`NotificationSoapClient`** → **`notification-soap-service`** (SOAP) → that service sends mail with its own `JavaMailSender` to the same style of SMTP/MailHog endpoint.

E2E check: `tests/e2e/api-smoke.ps1` asserts the verification message reaches MailHog (`8025`); Playwright `frontend/e2e/smoke.spec.ts` repeats the verify path and opens Shop + Chat; optional `frontend/e2e/mail-soap.spec.ts` can assert SOAP-sent low-hunger mail when run with env vars (see `documentation/testing/TEST_STRATEGY.md`).

## 4) API (Current)

### Existing game APIs
- dashboard, shop, inventory, minigames endpoints remain active

### New pet visual APIs
- `GET /api/pet-visuals/catalog` — all active rows (`PET_MOOD`, `BACKGROUND`, `FOREGROUND`), sorted
- `POST /api/pet-visuals/species` with `{ speciesCode }`
- `POST /api/pet-visuals/mood-assets` with `{ moodAssetCodes }` — non-starter mood assets require ownership
- `POST /api/pet-visuals/equip-layers` with `{ backgroundAssetCode, foregroundAssetCode }` — use `"none"` to clear a slot

- `GET /api/ai/info` — gateway configured flag, optional `gatewayHealth` (when enabled), and public guardrail sizes (max message / turns / assistant chars)

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
- `frontend/src/game/pages/*`: one file per tab/page (`ShopPage`, `CustomizePage`, `ChatPage`, etc.)
- `frontend/src/game/pages/minigames/*`: minigames are split by area (modal shell, result sheet, and per-minigame hooks/components)

Rule: avoid “god components”; prefer files/functions under ~250 lines unless truly unavoidable.

### 5.3 Center pet (layered stage)
- Background: `equippedBackgroundAssetCode` → image from catalog (`BACKGROUND`), or default CSS gradient when unset.
- Pet: current species + derived mood PNG (`PET_MOOD`), alpha-friendly.
- Foreground: optional `equippedForegroundAssetCode` (`FOREGROUND`), drawn above the pet.

`Customize` tab:
- species: **starters** are `dog` and `cat`; additional species are unlocked via **Shop → Pets** (`SPECIES` items) into `ownedSpeciesCodes`. Tabs for locked species are disabled until purchased.
- scene: background + foreground selects (starter or owned only)
- mood slots: `happy` / `sad` / `hungry` / `tired` / `playing_dead` — options limited to **starter + owned** assets for that mood/species

**Pet mood PNGs:**  
- `frontend/public/pet-assets/<species-folder>/*.png` (e.g. `dog/`, `cat/`, `penguin/`, `goldfish/`, `lizard/`, `midnight-cat/` for `midnight_cat`, …)  

**Scene cosmetics (SVG or raster paths in catalog):**  
- Backgrounds: `frontend/public/cosmetic-staging/backgrounds/*.svg`  
- Foregrounds: `frontend/public/cosmetic-staging/foregrounds/*.svg`

## 6) Seed & Cleanup

### Seed
- `mongodb/init/01-seed.js` seeds:
  - consumables
  - minigames
  - `pet_visual_assets` (starter moods + backgrounds + foregrounds)
  - `shop_items`: `COSMETIC` scene unlocks, **`SPECIES` pet unlocks** (1000 coins standard, 3000 for legendary pets)

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

**Current MVP (local):** a **Chat** tab talks to `POST /api/ai/chat` (backend → optional Local SLM Gateway). **Settings** loads `GET /api/ai/info` (gateway configured flag, health/model hint when reachable, server guardrail sizes). Persona fine-tuning uses `app.aiPersonaAddendum` / `APP_AI_PERSONA_ADDENDUM` appended to the stats prefix. User message length, conversation turns, and assistant reply length are capped server-side.

Goal: turn the pet into a character that can “talk” in a consistent voice, driven by an AI model that runs under the owner's credentials.

Hard constraint: **cost is the #1 priority**. “Stupid and slow but cheap” is preferred over “fast and expensive”.

#### 7.4.1 UX spec (frontend)
- **Implemented:** a **Chat** tab (prompt, send, rolling thread, clear). The center stage still shows the pet; a “thinking” mood applies while a reply is loading.
- AI responses must be written **in the pet’s voice** (personality, tone, quirks).
- Session behavior:
  - short rolling context (last ~6 turns) on client and server-side turn cap
  - **Clear thread** button on Chat tab resets local history

#### 7.4.2 AI integration contract (between pet app and AI service)

We will run the model as a **separate standalone service** (no frontend) and call it from this pet app.

Minimum API contract (shape, not implementation):
- `POST /v1/chat` with:
  - `userId` (or a stable pseudonymous id)
  - `contextPrefix` (fixed caller-provided “system/prefix” string; includes pet name/species/stats)
  - `conversation` (last N turns)
  - `message` (the user message)
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

Current working assumptions (from answers/research):
- **AWS region**: `eu-central-1` (Frankfurt) preferred.
- **Concurrency**: up to ~3 active users at a time; ~20 registered accounts total.
- **Hardware**: CPU-only for now.
- **Runtime**: prefer **Ollama** as the local inference engine, but model choice comes first.
- **Chat history**: keep rolling context (e.g. last ~5 messages) + persona context; store in-memory initially (exact location TBD).
- **Fallback when AI is down**: return pet-noise strings like `_barks cheerfully_`, `_Woof?_`, `_Makes curious noise_`, etc.

Caller-owned prefix format (baseline):
- `contextPrefix` should look like:
  - `You are cute pet named <name> of type <dog> that can magically speak. You are <happy>/<happy_max> happy, <hunger>/<hunger_max> fed and you have <energy>/<energy_max> energy. Respond on following message impersonating this character in language of that message: <message>`

Cost-first hosting notes (research summary; numbers are directional, not guaranteed):
- CPU-friendly models are typically **≤ 4B params** with **4-bit quantization** (GGUF).
- Example instance families discussed for cheap CPU hosting:
  - `t4g.small` (2 vCPU / 2 GB): lowest cost; likely only supports very small models
  - `t4g.medium` (2 vCPU / 4 GB): still cheap; more realistic for small chat models
- Common cost-saving techniques:
  - **Spot** instances (cheaper, but can restart)
  - scheduling downtime (turn off when unused)
  - strict prompt/response limits (reduces CPU time)

#### 7.4.4 Roadmap (detailed steps)

Phase A — Product/spec alignment (docs-first)
- Define pet “persona” (name, traits, style rules, do/don’t list).
- Decide conversation memory policy (last N turns vs summarized memory).
- Decide where chat lives in the UI (tab vs panel).

Phase B — Choose AI model + runtime (research spike)
- Decide the model family and size target (small, offline, CPU-only).
- Decide inference runtime **after** model choice:
  - prefer **Ollama** (simple local engine) backed by GGUF-style quantized models where possible
  - alternative: `llama.cpp` bindings directly in Python
- Decide “no internet access” constraints:
  - confirm model is self-contained (no tools, no browsing)
- Establish the cost rubric:
  - RAM footprint
  - acceptable latency for short replies
  - tokens/sec on a cheap CPU instance
  - license compatibility
  - ease of deployment

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

### 7.5 SQL achievements + history + deployment + SOAP notifications (mostly implemented; AWS deploy pending)

**Implemented in-repo:** PostgreSQL activity history, achievements, notification preferences, daily challenges, notification delivery records, `notification-soap-service`, and a single Compose stack for the main app (see `docker-compose.yml`).

**Still open for “MVP + deploy”:** **`ROADMAP.md`** — only **AWS (EC2)** is in the committed “Next” path; extra moods/cosmetics and formal production model docs sit in **Backlog** there. **Final roadmap item:** cheap **AWS** deploy — nothing is listed after that.

This section stays as the architectural narrative; subsections below mix historical “planned” wording with **current** notes where behavior already shipped.

#### 7.5.1 Goals

Primary goals:
- learn **SQL** in a meaningful way inside this project
- learn **containerization** as a real deployment preparation step
- learn **Linux/AWS deployment** instead of staying local-only
- learn **SOAP** in a bounded, non-cluttering way through a side-service

Product goals:
- give the player visible long-term progression (achievements, journal, history)
- retain richer event data for future analytics and possible Elasticsearch indexing later
- add user-facing notification settings without stuffing SOAP concerns into the core gameplay model

#### 7.5.2 Persistence split (planned)

Current state:
- MongoDB stores live game state well (`pets`, `wallet`, `inventory`, configs, sessions)

Planned split:
- **MongoDB** keeps current gameplay state and flexible document-style config
- **SQL database** stores relational / historical / reporting-oriented data

Planned SQL scope:
- activity history / event log
- achievement definitions + user progress/unlocks
- optional challenge participation / leaderboard snapshots
- notification preferences and outbound notification records

Design rule:
- do **not** migrate everything to SQL just because SQL is being added
- use SQL where it is naturally stronger: history, relations, reporting, constraints, time-based records

#### 7.5.3 Phase 1 — Activity history and achievements in SQL

Purpose:
- build the app's first relational subsystem without destabilizing the existing Mongo gameplay core

Planned steps:
1. Use **PostgreSQL** as the first SQL engine.
2. Add a separate SQL schema for history / achievements.
3. Record gameplay events whenever important actions happen, for example:
   - login
   - feed / use consumable
   - buy item
   - change species / cosmetics / pet name
   - finish minigame
   - AI chat usage
4. Build achievement evaluation on top of that event stream.
5. Add APIs/UI for:
   - achievement list
   - unlocked achievements
   - recent activity / pet journal
   - lightweight stats summary

Initial data model direction:
- `activity_event`
  - append-only, rich metadata, intentionally verbose and well-structured for future analytics
- `achievement_definition`
  - achievement code, title, description, category, active flag
- `user_achievement`
  - unlocked achievements, unlock time, progress fields if needed
- `daily_challenge_definition`
  - shared generated challenges for one backend-local day
- `user_daily_challenge_progress`
  - per-user progress, completion timestamps, reward-granted state
- optional later:
  - `leaderboard_snapshot`

Analytics intention:
- keep enough normalized + raw metadata so that later we could feed Elasticsearch or another analytics/search system
- we are not adding Elasticsearch now, but the event model should avoid painting us into a corner

Achievement rollout direction:
- start with **permanent achievements only**
- once the event/history foundation is stable, extend it with lightweight daily challenges

Current implementation status:
- PostgreSQL is now wired into the backend as the first SQL subsystem.
- Flyway creates:
  - `activity_events`
  - `achievement_definitions`
  - `user_achievements`
  - `daily_challenge_definitions`
  - `user_daily_challenge_progress`
  - `notification_preferences`
  - `notification_deliveries`
- Starter permanent achievement definitions are seeded in the initial migration.
- The backend now records activity events for:
  - account registration
  - login
  - pet rename
  - species change
  - shop purchase
  - consumable usage
  - minigame completion
  - AI chat usage
- Achievement progress is now advanced from the SQL-backed event stream for matching event types.
- Notification preferences get a default SQL row on registration so the later SOAP/email phase has a stable anchor point.
- A first player-facing progress screen now reads that data back and shows:
  - daily challenges with per-user progress and reward state
  - permanent achievement progress
  - recent activity history / journal-style feed
- Shared daily challenges are now generated lazily from a small template pool:
  - same 3 challenges for all users for one backend-local day
  - currently mixes minigame-finish goals and low-cost consumable-use goals
  - rewards are granted automatically on first completion
- The app now exposes first-version notification preference APIs/UI for:
  - low-hunger reminder toggle
  - daily AI summary toggle
- Notification delivery attempts are now recorded in SQL with dedupe keys so scheduled sends do not spam users.
- The main backend now contains the first notification automation rules:
  - low-hunger reminder once per user per UTC day when hunger drops below the configured threshold
  - daily AI summary once per user per UTC day
- A standalone `notification-soap-service` now exists as the bounded SOAP side-service for real email delivery.
- The main backend now calls that SOAP service and developer-only endpoints can trigger both first-version notification types manually.

Implementation note:
- MongoDB still owns the live gameplay state.
- SQL is currently additive and focused on history/progression data.
- Activity history writes are best-effort so this new subsystem does not destabilize the existing gameplay flow while the MVP track is being built out.

#### 7.5.4 Phase 2 — Full stack containerization

Purpose:
- make the app reproducible locally and make AWS deployment much simpler later

Scope:
- frontend
- backend
- MongoDB
- SQL database
- MailHog (dev only)
- optional notification side-service profile

Current preference:
- use **one main Compose-based stack** for the main app if possible
- keep the AI gateway/container separate because it is its own side project

Current implementation status:
- `frontend/` now has a production Dockerfile using Vite build output + Nginx.
- `backend/` now has a production Dockerfile that packages and runs the Spring Boot jar.
- `docker-compose.yml` now runs:
  - frontend
  - backend
  - MongoDB
  - PostgreSQL
  - notification SOAP service
- MailHog remains available as a dev-only Compose profile rather than a required deployable service.
- Runtime config is wired through env vars so the stack can point at internal service names inside Compose.
- `tests/e2e/container-stack-smoke.ps1` now verifies the build + boot + API smoke path for the full containerized stack.

Important clarification:
- **containerization is not the same as deployment**
- containerization prepares the app for deployment; AWS deployment is the next phase

#### 7.5.5 Phase 3 — AWS deployment

Purpose:
- learn Linux/AWS operations and make the project actually deployable

Cost-first direction:
- prefer simpler, cheaper infrastructure first
- only move to more managed/complex services when needed

Planned path:
1. Choose the initial deployment style:
   - cheapest/simple path: **Linux VM / EC2 + Docker Compose**
   - later evolution path: managed container platform if justified
2. Deploy the containerized stack to AWS.
3. Add the operational basics:
   - env vars / secrets
   - health checks
   - logs
   - backups
   - TLS / domain
   - restart policy
4. Validate the AI gateway and notification side-service integration in that environment.

Learning goal:
- containerization is the preparation layer
- AWS deployment is the runtime/ops layer on top of that

#### 7.5.6 Phase 4 — SOAP notification side-service

Purpose:
- learn SOAP in a bounded integration that has real product value

Important design rule:
- SOAP is **not** the core style of this app
- we use it in a contained side-service so the main pet app does not become cluttered with legacy-style concerns

Concept:
- create a separate notification-oriented service with a SOAP API
- the pet app backend becomes a SOAP client of that service
- user-facing settings live in the pet app

Planned user-facing feature:
- add a notification toggle/settings area in the pet app
- examples:
  - reminders when the pet is hungry / tired
  - daily summary
  - achievement unlocked notification
  - optional AI/pet reminder messages later

Planned service responsibilities:
- receive SOAP requests like "send reminder", "send achievement notice", "test notification"
- keep notification delivery concerns isolated from the main game backend
- optionally persist notification records / attempts in SQL

Current implementation status:
- implemented as standalone project `notification-soap-service/`
- exposes a single `sendNotification` SOAP operation
- sends real email through SMTP (MailHog in local dev)
- main backend acts as SOAP client through a small raw-XML integration
- first wired notification types:
  - low-hunger reminder
  - daily AI summary
- notification attempts are persisted in SQL (`notification_deliveries`) for audit/dedupe

Initial notification scope:
- real email delivery from the side-service (not mock-only)
- first notification types:
  - hungry-pet reminder when hunger drops below a low threshold (current direction: `< 15%`)
  - daily AI summary / daily pet notification
- both should have their own toggle button in settings

Why this is a good fit:
- teaches SOAP without turning the whole project into a SOAP app
- gives the integration a real reason to exist
- naturally connects with notification settings in the main app

#### 7.5.7 Suggested implementation order

Recommended order:
1. SQL event history foundation
2. achievements on top of history
3. add SOAP notification side-service
4. containerize the app stack
5. deploy to AWS

Why this order:
- achievements/history create product value immediately
- SOAP side-service now has a natural place because notification preferences and SQL audit rows already exist
- containerization helps every later phase
- AWS deployment is easier once the stack is containerized

#### 7.5.8 Open questions for this roadmap

SQL/history:
- How raw should `activity_event` be? Minimal business events or very verbose event payloads?
- Which actions must be tracked from day one?
  - current direction: core pet actions + minigame results + AI chat usage from the first version

Achievements:
- permanent achievements first is the current direction
- daily challenges are now implemented as a thin layer on top of the activity-event stream
- Should achievements be purely backend-driven, or can some UI-only milestones exist?

Containerization/deploy:
- one main deployable Compose setup for the main app now exists in `docker-compose.yml`
- AI remains separate from that stack
- For first AWS deployment, we currently prefer the simpler Linux VM / EC2 path.

Notifications / SOAP:
- First channel: real email delivery.
- Future channel abstraction is allowed later, but not required for the first version.
- Which additional events beyond low-hunger + daily AI summary should be allowed by default?

## 8) Development Rule

When changing behavior:
1. Update this file first.
2. Update seeds/schema/API docs.
3. Implement code.
4. Verify frontend build and backend compile.
