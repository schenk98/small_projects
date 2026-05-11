# Poe Pet App — Roadmap

High-level milestones. Detailed behavior lives in **`SOURCE_OF_TRUTH.md`**.

## Done (current local PoC)

- [x] Repo layout, Docker Mongo + MailHog, `start-all` scripts.
- [x] Spring Boot API: auth (register, verify, login, refresh), JWT, pet simulation, wallet, shop purchase, inventory use.
- [x] React app: auth screens, game shell, top stats, **layered pet stage** (background / pet / foreground).
- [x] **Pet visuals**: species dog/cat, moods including **`playing_dead`**, catalog API, customize mood slots + scene layers.
- [x] **Cosmetics shop**: `COSMETIC` items grant `ownedVisualAssetCodes`; equip via **`POST /api/pet-visuals/equip-layers`**.
- [x] Minigames hub: Higher/Lower, Puzzle Swap, Connect 4, Minesweeper, Checkers (with snapshots / summaries where applicable).
- [x] Developer tools (privileged): coins, refill stats, set stats %.

## Next

- [ ] Shop UX: thumbnails for backgrounds / cosmetics; disable “Buy” when already owned (server already rejects).
- [ ] More **`PET_MOOD`** catalog rows + matching `COSMETIC` unlocks (extra expressions per species).
- [ ] Additional species (fox, etc.) when `speciesCode` and seeds are extended.
- [ ] Deployable stack (frontend + API + managed Mongo + real SMTP).
- [ ] Broader automated tests (see **`documentation/testing/TEST_STRATEGY.md`**) and optional Playwright E2E.

## Next (AI Pet track)

Primary initiative: **AI Pet** (see `documentation/SOURCE_OF_TRUTH.md` section **7.4**).

- [ ] Define pet persona and chat UX placement (tab vs panel).
- [ ] Research spike: decide which AI model to run (small/offline) + licensing (**cost-first**).
- [ ] Decide inference runtime (prefer Ollama) + cheap CPU instance target and set latency/cost targets.
- [ ] Build standalone **Local SLM Gateway** side project (private API; no frontend) in separate folder/repo (`local-slm-gateway/`).
- [ ] Connect pet app backend → AI service; expose a frontend-friendly chat endpoint.
- [ ] Add basic contract tests + guardrails (max prompt/response sizes, timeouts, failure fallback).

## Next (SQL + deployment + SOAP track)

Primary initiative: **history / achievements / deployability / notifications** (see `documentation/SOURCE_OF_TRUTH.md` section **7.5**).

- [ ] Define the first PostgreSQL schema for `activity_event`, achievements, and notification preferences.
- [ ] Start recording rich activity history from core pet actions, minigame results, and AI chat usage.
- [ ] Build the first achievement system on top of that history data (permanent achievements first).
- [ ] Add player-facing UI for achievement progress and activity/journal history.
- [ ] Add daily challenges after the permanent-achievement foundation is stable.
- [ ] Containerize the whole main app stack in one Compose-based setup if possible (frontend, backend, MongoDB, SQL DB, dev mail).
- [ ] Deploy the stack to AWS on a cheap, learnable path: Linux VM / EC2 first.
- [ ] Add notification settings in the app and a SOAP notification side-service with real email delivery.
- [ ] Ship the first notification types: low-hunger reminder and daily AI summary, each with its own toggle in settings.
