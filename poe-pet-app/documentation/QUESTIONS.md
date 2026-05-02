# Poe Pet App — Questions and Answers

## Resolved decisions

- Auth: login required; one account ↔ one email ↔ one pet document.
- Email verification required before login.
- Backend: Maven + Spring Boot; frontend: React + TypeScript + Vite.
- Time simulation: lazy from `pets.lastSimulationAt`.
- Minigame energy: per-game cost from `minigames` collection.
- Rewards / happiness: DB-driven strategies; preview via dashboard + `GET /api/minigames/reward-preview`.
- Shop: consumables (`inventory_items`) and **cosmetics** (`COSMETIC` shop rows → `pets.ownedVisualAssetCodes`).
- Pet visuals: **layered stage** (background + pet mood PNG + optional foreground); **Customize** equips scene + per-mood overrides (starter or owned only).
- Hunger-critical mood is **`playing_dead`** (UI “Playing dead”), not `dead`.
- Pet art: AI-generated PNGs acceptable; use a **reference image per species** for consistency; prefer alpha for layering.
- Local Mongo: Docker Compose; seed **`mongodb/init/01-seed.js`** on **first volume create** only (see `mongodb/README.md`).
- Password policy and JWT refresh: as implemented in backend.

## Open items (non-blocking)

- Exact tuning coefficients for hunger / happiness / energy curves.
- Default shop prices and cosmetic prices (currently dev-friendly).
- Puzzle swap: optional user uploads vs only curated / stock / AI URLs (`App.tsx` `startPuzzle`).
- Connect 4 / other AI depths and balancing.
- Whether minigames stay modal vs dedicated routes.
- Cloud region and hosting choice for a future deploy.
