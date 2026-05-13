# Poe Pet App — Roadmap

High-level milestones. Detailed behavior lives in **`SOURCE_OF_TRUTH.md`**.

## Done (current local PoC)

- [x] Repo layout, Docker Mongo + MailHog, `start-all` scripts.
- [x] Spring Boot API: auth (register, verify, login, refresh), JWT, pet simulation, wallet, shop purchase, inventory use.
- [x] React app: auth screens, game shell, top stats, **layered pet stage** (background / pet / foreground), **AI chat on the home screen** (gateway / fallback).
- [x] **Pet visuals**: species dog/cat/penguin/fox/hamster/tiger/lion/horse/parrot/unicorn/midnight_cat/panda/goldfish/lizard, moods including **`playing_dead`**, catalog API, customize mood slots + scene layers.
- [x] **Cosmetics shop**: `COSMETIC` items grant `ownedVisualAssetCodes`; equip via **`POST /api/pet-visuals/equip-layers`**.
- [x] **Pets shop**: `SPECIES` items unlock extra `speciesCode` values into `ownedSpeciesCodes` (starters: dog + cat only).
- [x] Minigames hub: Higher/Lower, Puzzle Swap, Connect 4, Minesweeper, Checkers (with snapshots / summaries where applicable).
- [x] Developer tools (privileged): coins, refill stats, set stats %.
- [x] Shop UX: thumbnails; cosmetics/pets **Owned** states; tabbed shop (Consumables / Cosmetics / Pets).
- [x] Deployable stack foundation (frontend + backend + MongoDB + PostgreSQL + SOAP notification service in one Compose setup).
- [x] Broader automated tests: Playwright UI smoke (`frontend/e2e/smoke.spec.ts`) plus optional SOAP→MailHog check (`frontend/e2e/mail-soap.spec.ts`); see strategy doc.
- [x] **AI Pet (local)**: Local SLM Gateway, `POST /api/ai/chat`, `GET /api/ai/info`, guardrails + tests; **rich chat context** (daily challenges, recent activity, consumables/minigame catalogs from DB, per-species persona + optional `pets.aiPersonalityBrief` in Mongo).

## Next (this alpha)

**Committed next step:** only **AWS** (below). AI already runs locally via the gateway; extra mood art / cosmetics catalog work and formal “which model on which CPU tier” write-ups are **not** gating this phase.

---

## Last milestone: AWS

**Final planned step:** leave the laptop and run the app in the cloud.

- [ ] **Deploy the stack to AWS** on a cheap, learnable path: Linux VM / **EC2** first (e.g. `eu-central-1`), Compose or equivalent, secrets, backups, and minimal ops notes. AI gateway stays a separate deployable unit as today.

When this box is checked, treat “MVP + deploy” as complete for this PoC; scale-up (GPU, multi-region, etc.) is out of scope unless you open a new roadmap.

---

## Backlog (deferred — revisit later)

Not part of the current alpha commitment; pick up after AWS or whenever you want more content.

- [ ] More **`PET_MOOD`** catalog rows + matching **`COSMETIC`** unlocks (extra expressions per species).
- [ ] **AI production hardening:** document a default model + licensing and CPU/Ollama (or other) targets for a future hosted stack — optional once AWS and real traffic exist.
