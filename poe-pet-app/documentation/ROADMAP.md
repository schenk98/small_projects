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
