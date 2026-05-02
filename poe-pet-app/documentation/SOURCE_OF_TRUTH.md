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

## 5) Frontend UX (Current)

### 5.1 Puzzle swap image sources
- Implemented in `App.tsx` inside `startPuzzle`: **curated Unsplash URLs** (free stock) by default. Swap for other free-stock or self-hosted URLs, or AI-generated assets under `public/`, as needed.

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

## 8) Development Rule

When changing behavior:
1. Update this file first.
2. Update seeds/schema/API docs.
3. Implement code.
4. Verify frontend build and backend compile.
