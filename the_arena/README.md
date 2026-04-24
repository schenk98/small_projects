# The Arena - Specification-Driven Development

This document is the single source of truth for this project.

We keep it updated:
- whenever we add/change a feature,
- whenever there is a design question or decision,
- after each meaningful command group so we stay aligned.

---

## 1) Project Goal

Build a **Turn-Based Combat Manager** backend in Java.

Scope focus:
- backend engine only (no graphical front-end),
- robust player/monster/loot handling,
- mixed persistence with MySQL + MongoDB,
- API-driven battle flow.

---

## 2) Architecture (Target)

- **MySQL + Hibernate (JPA)** for permanent data:
  - user accounts,
  - player progression (XP, level),
  - achievements.
- **MongoDB** for dynamic and flexible data:
  - item definitions with variable stats,
  - monster templates,
  - combat logs.
- **REST API (Spring Boot)** for gameplay commands:
  - start battle,
  - process turn actions,
  - return turn-by-turn results.

---

## 3) Roadmap

### Phase 1 - Domain (Java + Hibernate)
- Define entities and repository layer for player/account/progression.
- Implement level-up logic (XP gain after victory).
- Add unit tests:
  - JUnit for rules,
  - Mockito for repository mocking (no real DB required for logic tests).

### Phase 2 - Loot and Dynamic Content (MongoDB)
- Add MongoDB integration via Spring Data MongoDB.
- Model flexible item schema (different fields per item type).
- Create service to fetch random monster from Mongo for a player from MySQL.

### Phase 3 - Battle Engine (Spring Boot REST)
- Create battle controller:
  - `POST /arena/start`
  - `PATCH /arena/turn`
- Return deterministic turn result payloads/messages.

### Phase 4 - Containerization (Docker)
- Containerize app + databases for local development.
- Keep startup and reset workflow simple and repeatable.

---

## 4) Environment Preparation (Windows + Cursor)

Current status (verified):
- Docker: installed and working
- Java: installed and working (Temurin 21)
- Maven: installed and working (3.9.15)
- Spring Boot PoC: implemented and testable locally

Recommended baseline:
- Java: Temurin/OpenJDK 21 (LTS)
- Build tool: Maven (initially)
- Framework: Spring Boot 3.x
- Databases: MySQL 8 + MongoDB 7 (via Docker)

Suggested install commands (PowerShell, run manually):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Maven installation note:
- Winget source on this machine currently does not provide a standard Apache Maven package.
- Install Maven manually from the official binary zip and add it to PATH.

Manual Maven install (PowerShell example):

```powershell
# 1) Download latest binary zip from https://maven.apache.org/download.cgi
# 2) Extract to e.g. C:\Tools\apache-maven-3.9.11

[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\Tools\apache-maven-3.9.11", "Machine")
$machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
if ($machinePath -notlike "*%MAVEN_HOME%\\bin*") {
  [Environment]::SetEnvironmentVariable("Path", "$machinePath;%MAVEN_HOME%\bin", "Machine")
}
```

Then verify:

```powershell
java -version
mvn -version
docker --version
```

If `java`/`mvn` are still not found:
- Restart Cursor fully (or sign out/in to Windows) so the shell inherits new machine PATH.
- Confirm `JAVA_HOME` and `Path` in System Environment Variables include:
  - `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`
  - `%JAVA_HOME%\bin` in `Path`
- Temporary session fix (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

---

## 5) Working Agreement

- This README is updated before/after meaningful implementation changes.
- New requirements go here first.
- If code and README diverge, README gets corrected immediately.
- "No code now" is respected: we only prepare environment and specifications.

---

## 6) Command Log

### 2026-04-24
- Checked current workspace structure.
- Checked active terminal state to avoid interfering with running work.
- Verified toolchain:
  - `java`: not installed / not on PATH
  - `mvn`: not installed / not on PATH
  - `gradle`: not installed / not on PATH
  - `docker`: installed
- Created project directory: `the_arena/`
- Created this specification README.
- Installed Java JDK 21 successfully via winget.
- Verified Java runs from direct executable path:
  - `C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe -version`
- Found environment mismatch:
  - Machine `JAVA_HOME` and machine `Path` include JDK.
  - Current Cursor terminal session does not yet include updated PATH.
- `winget install Apache.Maven` is not valid on this system; use `winget install Maven.Maven`.
- Maven package is currently unavailable in winget source on this machine; proceed with manual install.
- Diagnosed Maven PATH issue:
  - `C:\Users\jakub\apache-maven-3.9.15` contains Maven source tree (no `bin\mvn.cmd`).
  - Root cause: wrong archive type downloaded (source zip/tar), not binary zip.
  - Required fix: download `apache-maven-<version>-bin.zip`, then set `MAVEN_HOME` to extracted binary folder.
- Diagnosed second Maven path mismatch:
  - Binary Maven was extracted to `C:\Users\jakub\apache-maven-3.9.15-bin\apache-maven-3.9.15`.
  - `mvn.cmd` exists in `C:\Users\jakub\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin`.
  - Root cause: `MAVEN_HOME` pointed one folder too high (`...\apache-maven-3.9.15-bin`).
- Maven setup completed successfully:
  - `mvn -version` works and reports Apache Maven 3.9.15.
  - Java runtime used by Maven: Temurin 21.0.10.
- `where.exe` failed in one session because `$env:Path` was overwritten with user-only PATH (missing `C:\Windows\System32`).
  - This does not affect Maven setup itself.
  - Fix by opening a new terminal or restoring session PATH from Machine+User environment.
- Added Cursor workspace setup for Java development:
  - Created `.vscode/extensions.json` with recommended Java/Spring/Maven/Docker tooling.
  - Created `.vscode/settings.json` with Java build/test defaults and Maven-friendly options.
- User requested simpler approach without Cursor prompts:
  - Removed `.vscode/` folder from `the_arena`.
  - Shared extension list for optional manual install.
- Created minimal Java + Maven smoke-test project:
  - Added `pom.xml` configured for Java 21 and `exec-maven-plugin`.
  - Added `src/main/java/arena/App.java`.
  - Verified run command: `mvn -q compile exec:java`.
  - Verified output: `The Arena setup test: Java + Maven works.`
- Replaced smoke-test app with minimal Spring Boot PoC:
  - Updated `pom.xml` to Spring Boot 3.3.5 (`web`, `validation`, `test`).
  - Added REST endpoints: `/health`, `/arena/start`, `/arena/turn`.
  - Implemented in-memory battle session service with deterministic attack turns.
  - Added unit tests for battle flow (`BattleServiceTest`).
  - Verified build and tests: `mvn test` -> BUILD SUCCESS.
- Polished architecture and cleaned combat model:
  - Replaced rigid player/monster HP fields with general `Contestant` object.
  - Added `Weapon`, `Wearable`, and `WearableType` for flexible combat rules.
  - Refactored battle damage to use weapon damage and wearable defense.
- Added database foundations:
  - Added MySQL/JPA dependency, player entity/repository, and progression service.
  - Added MongoDB documents/repositories for monster templates, items, and combat logs.
  - Added Mongo seed config for starter records.
  - Added Docker Compose for MySQL + Mongo and docker profile config.
- Added startup automation scripts:
  - `scripts/run-arena.ps1` for Windows PowerShell.
  - `scripts/run-arena.sh` for bash.
  - Scripts validate prerequisites, ensure Docker daemon is running, start DB containers, wait for health checks, and run the app.
  - PowerShell script supports `-AutoStartDocker` to launch Docker Desktop automatically and wait for daemon readiness.
- Cleaned dead setup:
  - Removed old simple `arena.App` smoke test class.
- Verified all tests pass after refactor:
  - `mvn test` -> BUILD SUCCESS (3 tests).
- Added JWT + Bearer token authentication:
  - Added account entity/repository and auth service/controller.
  - Added Spring Security config and JWT filter.
- Added server-rendered UI pages:
  - `/ui/login` and `/ui/arena` templates.
- Implemented achievement hooks:
  - first login
  - first attack
  - first battle win
- Added persistent battle session/history foundation:
  - MySQL `battle_sessions` metadata with seed, round, next turn, state snapshot.
  - Mongo `battle_events` timeline (initial/turn/final snapshots).
  - Logout/termination snapshot endpoint (`POST /arena/snapshot`).
- Added item hierarchy and loot storages:
  - base `Item`, `Weapon`, `Wearable`, `Consumable`.
  - weighted storages: chest/corpse/box with agreed probabilities.
- Added fully containerized run support:
  - App container added to `docker-compose.yml`.
  - Added `Dockerfile` and `.dockerignore`.
- Revalidated build and tests after implementation:
  - `mvn test` -> BUILD SUCCESS.

### 2026-04-24 (MVP sync — same day, later)
- Mongo `monster_templates`: equipment slots reference `item_definitions._id` (hex string); seed upserts items first then resolves ids; legacy name-only slot values still resolve in `MonsterCatalogService` when not 24-hex.
- Battle replay fidelity: added `ContestantLoadoutSnapshotService` — embeds `initialPlayerEquipment` / `initialEnemyEquipment` (frozen at `POST /arena/start`) into MySQL `battle_sessions.state_json`, Mongo `INITIAL_SNAPSHOT`, and initial `battle_projections` payload.
- Tests: `ContestantLoadoutSnapshotServiceTest`; `BattleServiceTest` mocks snapshot service; `ArenaIntegrationTest` asserts loadout keys on replay.
- README: §7 MVP focus, §10 status board, §13.5–§13.8 aligned with implementation and this design thread.

---

## 7) Current MVP Focus (Active)

- Keep deterministic arena flow stable and testable.
- Keep auth production-ready enough for MVP:
  - short-lived access JWT,
  - refresh-token rotation endpoint,
  - explicit logout revocation of access token IDs (JTI blacklist).
- Persist won loot into user inventory in MySQL.
- Keep replay quick-result fast via Mongo projection materialization.
- **Battle-start equipment snapshots:** freeze each side’s loadout (embedded item maps) when a battle starts so replays and `battle_sessions.state_json` stay faithful if `item_definitions` or templates change later.
- Treat older roadmap/checklist sections as historical context only.

---

## 8) PoC Execution Steps (Historical)

Goal: deliver a working backend PoC for The Arena with MySQL + MongoDB + REST endpoints and basic turn flow.

### Step 0 - Baseline and repo hygiene
- Keep current Spring Boot PoC as sanity check.
- Ignore build outputs with `.gitignore` (`target/`).
- Keep README as source of truth; update after each milestone.

### Step 1 - Bootstrap Spring Boot project
- [DONE] Replaced minimal app with Spring Boot scaffold (Maven, Java 21).
- [DONE] Added dependencies:
  - Spring Web
  - Spring Data JPA
  - MySQL Driver
  - Spring Data MongoDB
  - H2 (local fallback datasource)
  - Validation
  - Spring Boot Test
- [DONE] Added health endpoint to confirm app startup.

### Step 2 - Permanent domain in MySQL
- Define JPA entities:
  - `Player` (id, username, level, xp, createdAt)
  - optionally `Achievement` placeholder for later
- Create repositories and service layer for player lifecycle.
- Add leveling rules:
  - XP gain on win
  - level-up threshold logic
  - carry-over XP after level-up

Current state:
- [DONE] Added `PlayerEntity` + `PlayerRepository`.
- [DONE] Added `PlayerProgressionService` with XP/level-up/carry-over logic.
- [DONE] Added Mockito unit test for progression logic.

### Step 3 - Unit tests for level logic
- Add JUnit + Mockito tests for service logic only.
- Mock repositories; avoid real DB in unit tests.
- Cover:
  - XP increase
  - single level-up
  - multi-level-up edge case
  - no level-up case

### Step 4 - Dynamic data in MongoDB
- Define Mongo documents:
  - `MonsterTemplate`
  - `ItemDefinition`
  - `CombatLog`
- Add service to fetch random monster template for battle start.
- Seed a few starter monsters/items for PoC.

Current state:
- [DONE] Added Mongo documents: `MonsterTemplateDocument`, `ItemDefinitionDocument`, `CombatLogDocument`.
- [DONE] Added Mongo repositories.
- [DONE] Added seed config for starter monsters/items.
- [DONE] Added combat log writes (best effort; non-blocking if Mongo unavailable).

### Step 5 - Battle API PoC
- Implement endpoints:
  - `POST /arena/start` -> create session with player + random monster
  - `PATCH /arena/turn` -> process one turn and return outcome text/data
- Keep session in-memory for PoC (simple and fast), persist only needed log snapshots in Mongo.

Current state:
- [DONE] In-memory session handling for PoC.
- [DONE] Generalized combat model:
  - `Contestant` (player or enemy)
  - `Weapon` (name + damage)
  - `Wearable` (armor/accessory/enchantment with defense bonus)
- [DONE] Damage calculation derived from weapon damage and wearable defense.
- [DONE] Error handling for invalid session/action.

### Step 6 - Local containers
- Add `docker-compose.yml` with:
  - MySQL
  - MongoDB
- Configure Spring profiles/env vars for local credentials.
- Verify app runs against both databases locally.

Current state:
- [DONE] Added `docker-compose.yml` with MySQL 8.4 + MongoDB 7.
- [DONE] Added `application-docker.yml` profile for MySQL/Mongo connections.

### Step 7 - PoC acceptance checklist
- App starts cleanly.
- Player can be created and loaded from MySQL.
- Battle can be started and at least one turn processed.
- XP updates after victory and level logic is tested.
- Basic combat log entries are stored in MongoDB.
- README includes run/test commands and endpoint examples.

---

## 9) Run and Test the Current PoC

Run app:

```powershell
mvn spring-boot:run
```

Check health:

```powershell
curl http://localhost:8080/health
```

Start a battle (PowerShell-native):

```powershell
$startBody = @{ playerName = "Jakub" } | ConvertTo-Json
$start = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/arena/start" -ContentType "application/json" -Body $startBody
$start
```

Play one turn (uses returned session id):

```powershell
$turnBody = @{ sessionId = $start.sessionId; action = "attack" } | ConvertTo-Json
Invoke-RestMethod -Method PATCH -Uri "http://localhost:8080/arena/turn" -ContentType "application/json" -Body $turnBody
```

Run tests:

```powershell
mvn test
```

Note:
- Integration test (`ArenaIntegrationTest`) uses Testcontainers with real MySQL + MongoDB.
- If Docker daemon is unavailable, that integration test is automatically skipped, while unit tests still run.

Run with Docker databases:

```powershell
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=docker"
```

Run fully containerized app + databases:

```powershell
docker compose up --build
```

One-command startup (recommended):

```powershell
.\scripts\run-arena.ps1
```

Optional fast mode (skip tests):

```powershell
.\scripts\run-arena.ps1 -SkipBuild
```

Auto-start Docker Desktop when it is not running:

```powershell
.\scripts\run-arena.ps1 -AutoStartDocker
```

Linux/macOS shell variant:

```bash
./scripts/run-arena.sh
```

Open UI:

```text
http://localhost:8080/ui/login
```

API auth flow example:

```powershell
$registerBody = @{ username = "jakub"; password = "secret123" } | ConvertTo-Json
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/auth/register" -ContentType "application/json" -Body $registerBody

$loginBody = @{ username = "jakub"; password = "secret123" } | ConvertTo-Json
$login = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/auth/login" -ContentType "application/json" -Body $loginBody
$token = $login.accessToken

$headers = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/arena/start" -Headers $headers -Body "{}"
```

Error response format (for edge cases):

```json
{
  "timestamp": "2026-04-24T18:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid username or password.",
  "path": "/auth/login",
  "details": {}
}
```

Battle history API examples:

```powershell
# quick result by battle id
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/arena/history/$($start.sessionId)/result" -Headers $headers

# full replay payload (initial snapshot + turns + final snapshot)
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/arena/history/$($start.sessionId)/replay" -Headers $headers

# any specific point in game by turn number
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/arena/history/$($start.sessionId)/turn/1" -Headers $headers
```

---

## 10) Implementation Status Board

### Done
- Java 21 + Spring Boot backend with REST endpoints:
  - `GET /health`
  - `POST /arena/start`
  - `PATCH /arena/turn`
  - `POST /arena/snapshot`
  - `GET /arena/history/{battleId}/result`
  - `POST /arena/history/{battleId}/replay`
  - `GET /arena/history/{battleId}/turn/{turnNumber}`
- JWT auth domain:
  - `POST /auth/register`
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `GET /auth/me/achievements`
  - auth attempt rate limiting guardrails (in-memory window limiter per user+IP)
  - refresh token rotation + access-token JTI revocation on logout
- Server-rendered UI:
  - `/ui/login`
  - `/ui/arena`
- Flexible combat domain:
  - `Contestant` shared for player/enemy (slots: left/right hand, body, accessory, carried)
  - base `Item` + `Weapon` + `Wearable` + `Accessory` + `Consumable`
  - `LootStorageType` weighted loot strategy
- Content authoring (Mongo-backed):
  - `GET/POST /content/items`, `GET/POST /content/contestants`
  - `monster_templates` equipment slots reference `item_definitions._id` (24-hex); legacy name strings in a slot still resolve by name when not a valid id
- Battle history fidelity:
  - `ContestantLoadoutSnapshotService` embeds `initialPlayerEquipment` / `initialEnemyEquipment` in MySQL `battle_sessions.state_json` and in Mongo `INITIAL_SNAPSHOT` (and initial `battle_projections` payload)
- Tests:
  - unit: `BattleServiceTest`, `PlayerProgressionServiceTest`, `ContestantLoadoutSnapshotServiceTest`
  - integration: `ArenaIntegrationTest` (Testcontainers; skips without Docker)
- MySQL/JPA integration:
  - `PlayerEntity`
  - `PlayerRepository`
  - `PlayerProgressionService` (XP gain, level up, XP carry-over)
- MongoDB integration:
  - `MonsterTemplateDocument`
  - `ItemDefinitionDocument`
  - `CombatLogDocument`
  - `BattleEventDocument` (timeline/replay stream)
  - seed config via `MongoSeedConfig`
- Deterministic battle replay foundation:
  - battle `seed` stored in MySQL session metadata
  - initial/turn/final events stored in Mongo timeline
  - logout/termination snapshot endpoint
  - materialized battle projection (`battle_projections`) for fast quick-result reads
- Inventory persistence:
  - won loot saved to MySQL `inventory_items`
  - `GET /inventory/me` to inspect account inventory
- Dockerization:
  - `docker-compose.yml` for MySQL + MongoDB + app container
  - `Dockerfile` for app image
- Startup automation scripts:
  - `scripts/run-arena.ps1` (`-SkipBuild`, `-AutoStartDocker`)
  - `scripts/run-arena.sh`
- Security hardening:
  - HTTP Basic removed from Spring Security configuration
  - battle ownership enforced for turn/snapshot/history actions
  - UI flow redirects unauthorized users to login

### Partially Done
- Battle session persistence/history:
  - session metadata + event timeline are persisted, runtime state still cached in-memory while active.
  - ownership validation is enforced for turn/snapshot/history access by authenticated user.
- Inventory lifecycle:
  - loot is persisted and readable, but equip/consume transactions are not implemented yet.

### Not Done
- (none in core MVP scope)

---

## 11) Historical Notes

This section is intentionally kept as history of earlier planning steps.
Current implementation contract is section 12 + status board above.

### A) User accounts + auth domain
- Add account/login flow.
- Add a simple web UI for text-based controls/buttons.
- Login must be part of UX flow.

### B) Achievements tied to account
Start with:
- first login
- first attack
- first battle win

### C) Item model + loot storages
- Introduce base `Item` model.
- Add item subtypes:
  - `Weapon`
  - `Wearable`
  - `Consumable` (new)
- Add 3 loot storages with weighted probabilities:
  - chest -> higher chance for weapons
  - corpse -> higher chance for wearables
  - box -> higher chance for consumables

### D) Persistent sessions + replayable history
- Persist battle state with:
  - timestamp
  - round number
  - whose turn is next
  - account relation
- Record full step-by-step battle history.
- Add replay capability for any past battle.
- Add deterministic seeding:
  - one seed per battle
  - all random behavior must use the seed
  - seed must be stored with battle history record

### E) Integration tests
- Implement last, after core app flow is stable.

---

## 12) Approved Implementation Blueprint (Locked)

This section captures the final decisions from discussion and is the active implementation contract.

### 12.1 Authentication and UI
- Authentication mode: JWT with Bearer token.
- UI mode: server-rendered web pages (simple controls/buttons, text-focused UX).
- Login is mandatory part of user flow.
- Bearer transport decision: Authorization header (`Authorization: Bearer <token>`).
- If JWT approach becomes impractical during implementation, fallback to session cookies is allowed only after documenting reason here.

### 12.2 Database responsibility split (no cross-db joins)
- MySQL (authoritative game/account data):
  - accounts/users
  - achievements
  - battle session metadata and current state pointers
  - progression state
- MongoDB (content + event timeline):
  - item definitions / loot config
  - battle event stream / replay timeline
  - dynamic templates
- Cross-database relation is application-level by IDs only (e.g., `accountId`, `battleId`), never SQL join between MySQL and MongoDB.

### 12.3 Deterministic battle replay model
- Store one initial snapshot for each battle:
  - participants
  - initial stats
  - seed
  - start timestamp
- Store turn-by-turn timeline events (append-only).
- Store final snapshot at battle end.
- Store snapshot when player logs out or terminates session.
- Rebuild/replay state from initial snapshot + deterministic event sequence.

### 12.4 Item and loot model
- Introduce base `Item` model and subtype hierarchy:
  - `Weapon`
  - `Wearable`
  - `Consumable`
- Create initial catalog:
  - 3 weapons
  - 3 wearables
  - 3 consumables
- Add loot storages with weighted drop probabilities:
  - chest -> weapon 60%, wearable 25%, consumable 15%
  - corpse -> wearable 60%, weapon 25%, consumable 15%
  - box -> consumable 60%, wearable 25%, weapon 15%

### 12.5 Achievements (phase 1 set)
- First login
- First attack
- First battle win
- Achievements are account-bound and extensible.

### 12.6 Delivery order
1. Update docs/spec (this section).
2. Implement auth + UI skeleton.
3. Implement DB split and persistent battle session/history model.
4. Implement item hierarchy + loot storages.
5. Wire achievements in gameplay.
6. Add integration tests last.

Implementation progress:
- [DONE] 1. Docs/spec update
- [DONE] 2. Auth + UI skeleton
- [DONE] 3. DB split and persistent session/history foundations
- [DONE] 4. Item hierarchy + loot storages
- [DONE] 5. Achievement hooks for first login, first attack, first win
- [DONE] 6. Integration tests (Testcontainers-based, auto-skipped when Docker is unavailable)

---

## 13) Equipment Slot Refactor + Content Authoring (Active Spec)

This section is the active contract for the next implementation wave and supersedes older simplified combat model assumptions.

### 13.1 Unified contestant model
- `Contestant` is a shared battle actor for players and enemies.
- Required fields:
  - `name`
  - `type` (at minimum: `PLAYER`, `ENEMY`)
  - `description`
  - `health`
  - equipped slots:
    - left hand item
    - right hand item
    - body wearable
    - accessory
  - carried inventory slots (default max size: 3)
- Monsters and players are both represented as contestant objects.

### 13.2 Item inheritance hierarchy
- Base `Item` contains shared metadata:
  - `name`
  - `description`
  - `image` (optional URL/path)
  - `itemType`
  - `specialEffect` token (string, optional)
- Item subtypes:
  - `Weapon`
    - hand mode (`SINGLE_HAND`, `TWO_HAND`, `LEFT_ONLY`, `RIGHT_ONLY`)
    - `physicalDamage`
    - `magicalDamage`
  - `Wearable` (body armor/robe/etc.)
    - `physicalReduction`
    - `magicalReduction`
    - `specialDefense` token
  - `Accessory`
    - `specialAccessory` token
  - `Consumable`
    - effect + potency (existing behavior retained)

### 13.3 Deterministic special hooks
- Battle resolution must keep deterministic behavior per battle seed.
- Three deterministic hook points are required:
  - weapon attack special trigger
  - defense special trigger
  - accessory special trigger
- Hook input is a string token from the item data.
- Unknown/empty tokens are valid and treated as no-op.

### 13.4 Battle calculation intent
- On attack turn:
  - equipped weapon contributes physical + magical damage
  - attack specials may modify output
- On defense:
  - equipped body wearable contributes physical + magical reductions
  - defense/accessory specials may modify mitigation
- Only equipped items may trigger specials.
- Carried items are for future swap/consume flow and do not passively trigger.

### 13.5 Mongo content model refinement
- Monster templates should no longer embed ad-hoc weapon stats as primitive fields.
- Monster and item definitions should be composable:
  - **`item_definitions`** documents are the canonical item rows (type + stats + specials).
  - **`monster_templates`** equipment slots store **references to `item_definitions._id`** (Mongo `ObjectId`, represented as a 24-char hex string in JSON/Java), not duplicate item text or loose name strings.
  - At runtime the battle layer loads each referenced document and maps it into the Java `Item` hierarchy (`Weapon`, `Wearable`, `Accessory`).
- Seed upserts items first, resolves their ids, then upserts each monster template with those ids (and removes legacy string slot keys if present).
- For one-off migration, older templates that still store a **name** in a slot field are still resolved by name only when the value is **not** a 24-hex id (compatibility); new content should use ids only.
- Seed data should include a larger starter catalog than the previous minimal set.

### 13.6 Authoring APIs (manual content creation)
- Add APIs to handcraft and store combat content from JSON:
  - list contestants/templates
  - create contestant/template
  - list items
  - create item
- These APIs are intentionally MVP/simple and aimed at manual authoring and testing.

### 13.7 Frontend usability improvements
- Arena UI should support:
  - easier battle control (start, attack, history actions)
  - viewing inventory
  - content authoring actions (create/list contestants and items)
- Keep UX simple and text-first, but reduce manual JSON friction with dedicated controls.

### 13.8 Battle-time equipment snapshots (replay fidelity)
- When `POST /arena/start` creates a `BattleSession`, the engine captures **immutable** JSON maps of each contestant’s equipment (`ContestantLoadoutSnapshotService.freezeAtBattleStart`).
- These maps are stored as:
  - `initialPlayerEquipment` / `initialEnemyEquipment` inside MySQL `battle_sessions.state_json` on every persist (same frozen maps each time; live HP is stored separately in that JSON).
  - The same keys on the Mongo `INITIAL_SNAPSHOT` event payload (and copied into the first `battle_projections` snapshot for quick reads).
- **Why:** `item_definitions` and templates can be rebalanced later; historical battles and replays still show what each fighter actually brought **at fight start**, independent of later catalog edits.
- **Not the same as** live combat state: current HP and turn messages still come from turn events and the in-memory session during an active fight.
