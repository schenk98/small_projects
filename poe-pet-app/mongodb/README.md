# MongoDB (local Docker)

This folder feeds **`docker-compose.yml`** at the repo root. The **`poe-pet-app`** directory is the compose project root.

## What runs automatically

- **`init/`** is mounted at **`/docker-entrypoint-initdb.d`** (read-only).
- The official **`mongo:7`** image executes **`*.js`** and **`*.sh`** there **only on first database init** (empty `mongo_data` volume).
- The canonical seed is **`init/01-seed.js`**: it clears and repopulates `shop_items`, `pet_visual_assets`, `minigames`, and related cleanup for legacy collections.

## Bundled JSON (optional / legacy)

- **`seed-shop-items.json`**, **`seed-minigames.json`** — also mounted under `/seed/` in compose for tooling that imports JSON separately. The live **`01-seed.js`** embeds the same catalog inline; keep them in sync if you maintain both.

## Re-seeding after a schema or catalog change

1. **Full reset (recommended for dev)** — from **`poe-pet-app`**:

   ```powershell
   docker compose down -v
   docker compose up -d mongodb
   ```

   Wait a few seconds, then verify:

   ```powershell
   docker exec poe-pet-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin --eval "db.getSiblingDB('poe_pet').getCollectionNames()"
   ```

   You should see at least `pet_visual_assets`, `shop_items`, `minigames`.

2. **Keep data, patch pets only** — run **`scripts/migrate-playing-dead-and-cosmetics.js`** in `mongosh`, then merge new `pet_visual_assets` / `shop_items` rows manually or re-run the insert blocks from `01-seed.js`.

## Shop visibility

Shop rows support **`playerVisible: false`** to hide items from `GET /api/shop/items` without deleting them.
