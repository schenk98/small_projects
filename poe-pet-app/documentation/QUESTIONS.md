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
  To be tested - good for now
- Default shop prices and cosmetic prices (currently dev-friendly).
  To be tested - good for now
- Puzzle swap: optional user uploads vs only curated / stock / AI URLs (`App.tsx` `startPuzzle`).
  we should generate images of our main pet on some background for this, but temporary images are ok for now
- Connect 4 / other AI depths and balancing.
  this is already working well - no changes needed for now
- Whether minigames stay modal vs dedicated routes.
  I feel like having them modal is better for future expansion, changes etc.
- Cloud region and hosting choice for a future deploy.
  aws east europe or something like that

## Open items (AI Pet track)

These are tracked in more detail in `documentation/SOURCE_OF_TRUTH.md` section **7.4.5**.

- Model choice: which small model do we run (and why)? What are licensing constraints?
  add it to roadmap as whole step including investigation, take into account our usecase etc.
- Runtime choice: `llama.cpp` / GGUF vs `transformers` / HF vs other.
  I would prefer ollama, but we might want to wait for our desicion regarding model first - add it to roadmap
- Hosting: CPU-only vs GPU; latency and cost targets; which AWS instance family?
  cpu only, this is just side project, that will have up to like 3 users at a time and up to 20 registered accounts. In the future we can scale up, but we will go with this usecase
- Security: how does pet backend authenticate to AI service (API key vs mTLS vs signed JWT)?
  since it will be my app running copy of model, we will just have like token generated or something - we should explore industry standard
- Data policy: do we store chat history? If yes, retention and where?
  I guess we should store like current session, but it could be in memory - second option is to have it on side of ai app, one session per pet or something with removing older parts of conversation - like remember last 5 messages + personality context or something
- Reliability: fallback behavior when AI service is down; timeouts; rate limiting strategy.
  when the ai service is down fallback will be like meowing and barking (like several random strings - _barks cheerfully_, _Woof?_, _Makes curious noise_, ...)
