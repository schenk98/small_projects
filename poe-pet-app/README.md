# Poe Pet App

Docs-first monorepo for a virtual pet web app.

## Start Here

Read documents in this order before coding:

1. `documentation/SOURCE_OF_TRUTH.md`
2. `documentation/QUESTIONS.md`
3. `documentation/ROADMAP.md`
4. `documentation/UI_LAYOUT.md`
5. `documentation/testing/TEST_STRATEGY.md`

## Folders

- `frontend/` - React + TypeScript app
- `backend/` - Java + Spring Boot API
- `mongodb/` - local DB setup and seeds
- `documentation/` - architecture, plans, open questions, tests

## Working Rule

Every implementation must be traceable to the source-of-truth docs.

## Local Startup

- **Recommended (one command)** — Docker (Mongo + MailHog), frees ports **5173** and **8080**, **`mvn compile`**, then opens **two** terminals (frontend `npm run dev`, backend `mvn spring-boot:run`):
  - Windows: `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`
  - Bash (Git Bash/WSL): `bash ./start-all.sh`

The scripts assume you run them from the **`poe-pet-app`** directory (they `cd` to their own folder if needed).

## Verify local stack

1. **Docker**: `docker compose ps` — `mongodb` and `mailhog` should be `Up`.
2. **Backend**: open `http://localhost:8080` — connection reset or empty page is normal; the API is under `/api/...` and needs a Bearer token after login.
3. **Frontend**: open `http://localhost:5173` — log in; top bar should load without “Loading failed”.
4. **Minigame payouts**: after login, open **Minigames**. You should see **“Payout preview:”** lines under each game. If you only see a gray note about the backend, the JVM process is still an **old build**: stop Java on port **8080**, then from `backend/` run `mvn spring-boot:run` again (or re-run `start-all.ps1`).
5. **Optional API check** (with a valid access token from the browser devtools → Network → any `/api/dashboard` request):  
   `GET http://localhost:8080/api/minigames/reward-preview` with header `Authorization: Bearer <token>` should return JSON with `coinMultiplier`, `energyCosts`, `higherLower`, etc.

**MongoDB note:** energy costs, shop catalog, minigames, and **`pet_visual_assets`** (moods + scene cosmetics) all come from the DB. After a **git pull** that changes seeds, either **reset the volume** so `init/01-seed.js` runs again, or run that script manually — see **`mongodb/README.md`** for step-by-step commands and verification (`pet_visual_assets`, `shop_items`, `minigames`).

### Privileged developer tools

Backend reads `APP_PRIVILEGED_EMAILS` (comma-separated list, matches login email after normalization). Set it when starting Spring Boot or in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) as `app.privilegedEmails`. Alternatively set `privileged: true` on a user document in MongoDB (`users` collection).

### CORS (browser access policy)

Backend CORS uses an explicit allowlist (safe-by-default).

- Dev default: `http://localhost:5173`
- Override with `APP_CORS_ALLOWED_ORIGINS` (comma-separated), e.g.:
  - `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend.example`

Re-seed or update `minigames` / `shop_items` in Atlas if you need the lowered energy costs and new consumables in an existing DB.

What `start-all` does:

- Starts MongoDB + MailHog via Docker Compose (seed on first volume create).
- Stops processes listening on **5173** and **8080** (dev servers from a previous session).
- Runs **`mvn -DskipTests compile`** in `backend/` so the API matches current sources.
- Launches **frontend** and **backend** in separate terminal windows (Windows) or background jobs (Bash).

## Tests

- **All local automated tests** (backend `mvn test`, frontend Vitest, frontend production build):
  - `powershell -ExecutionPolicy Bypass -File .\run-all-tests.ps1`
- Backend unit tests only:
  - `cd backend && mvn test`
- Frontend unit tests only:
  - `cd frontend && npm run test`
- API E2E smoke test (requires running backend + mongo + mailhog):
  - `powershell -ExecutionPolicy Bypass -File .\tests\e2e\api-smoke.ps1`
