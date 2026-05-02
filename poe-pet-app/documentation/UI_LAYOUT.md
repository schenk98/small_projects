# Poe Pet App — UI layout (implemented shape)

## Goals

- Pet-first: stats and coin balance always visible.
- Simple **top nav links** (Shop, Minigames, Inventory, Customize, Settings) — Pou-like “rooms” without heavy nesting.
- **Layered center card**: optional **background** image → **pet** (mood PNG) → optional **foreground** overlay.

## Main shell (`GameApp`)

1. **Top bar** — coins, optional Developer menu (privileged), hunger / happiness / energy bars.
2. **Pet stage card** — species + mood label; layered visuals (see `App.css` `.pet-stage-layers`, `.pet-stage-fg`).
3. **Nav row** — route links under the stage.

## Shop (`/app/shop`)

- **Consumables** — buy adds quantity to inventory.
- **Cosmetics** — one-time buy; unlocks visual asset codes for backgrounds / foregrounds (equip in Customize).

## Customize (`/app/customize`)

- Species toggle (dog / cat).
- **Scene**: background dropdown, foreground dropdown (only starter + **owned** assets).
- **Mood images**: one select per mood slot (`happy` … `playing_dead`); options = starter + owned for that mood/species.

## Minigames (`/app/minigames`)

- DB-driven list with energy cost + payout preview text.
- Each game opens a **modal** with scrollable result / board snapshot on completion.

## Inventory (`/app/inventory`)

- Consumables only (quantity + Use). Cosmetics do not appear here; ownership is on the pet document.

## Settings (`/app/settings`)

- Account / logout / developer hint.

## Future structural refactor (optional)

Splitting `App.tsx` into `PetStage`, `ShopView`, `CustomizeView`, etc. is optional; shared logic already lives under **`frontend/src/lib/`** (`gameApi`, `gameTypes`, `petVisuals`, `rewardPreview`).
