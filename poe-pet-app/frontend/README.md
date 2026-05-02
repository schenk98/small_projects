# Poe Pet — frontend

React + TypeScript + Vite app for **Poe Pet**. Product rules and APIs are documented under **`../documentation/`** (start with **`SOURCE_OF_TRUTH.md`**).

## Commands

| Command | Purpose |
|--------|---------|
| `npm install` | Dependencies |
| `npm run dev` | Dev server (default `http://localhost:5173`) |
| `npm run build` | Production bundle |
| `npm run test` | Vitest unit tests |

## Layout (code)

- **`src/App.tsx`** — auth shell, **`GameApp`** (dashboard, shop, minigames, customize).
- **`src/lib/gameApi.ts`** — authenticated JSON client factory (`createJsonApiClient`).
- **`src/lib/gameTypes.ts`** — DTO shapes shared with API responses (`GameShopItem`, etc.).
- **`src/lib/petVisuals.ts`** — mood derivation, catalog types, `MOOD_LABELS`, `MOOD_SLOT_ORDER`.
- **`src/lib/rewardPreview.ts`** — dashboard reward-preview typing and merge helpers.
- **`src/auth/`** — login, register, verify, password reset screens.
- **`src/minigames/`** — game logic and UI pieces (checkers, minesweeper, …).

API base URL: **`src/config.ts`** (typically points at `http://localhost:8080`).
