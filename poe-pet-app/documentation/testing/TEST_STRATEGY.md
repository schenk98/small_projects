# Poe Pet App - Test Strategy

## Quality Goal

Build a stable PoC where time-based simulation, economy operations, and auth flows are reliable and repeatable.

## Test Pyramid (Moqaccino-style layered approach)

Interpretation for this project:

- many fast unit tests
- smaller set of integration tests
- focused API contract tests
- minimal but critical end-to-end flows

## Backend Test Layers

### Unit Tests (JUnit + Mockito)

- simulation service:
  - hunger/happiness/energy formulas
  - low-hunger penalties
  - boost modifier application and expiry
- minigame payout calculator
- item effect handlers
- auth token service

### Integration Tests (Spring Boot + Testcontainers Mongo)

- repository behavior
- transaction-like consistency for:
  - purchase -> coin deduction -> inventory update
  - minigame finish -> reward -> wallet update
- registration + verification token lifecycle

### API/Contract Tests

- endpoint request/response shape
- validation errors and status codes
- auth guard behavior

## Frontend Test Layers

### Component Tests (Vitest + React Testing Library)

- stat bars and status indicators
- shop item rows (consumables + cosmetics sections)
- layered pet stage (background URL / default gradient, foreground optional)
- customize: mood slot options filtered by ownership
- minigame widgets
- auth forms

### Integration/UI Flow Tests

- register -> verify -> login
- buy consumable -> use item -> pet stats update
- buy cosmetic -> owned codes include asset -> equip background / foreground -> dashboard shows equipped fields
- play higher/lower -> receive coins

### E2E Smoke (Playwright, optional in PoC)

- happy path lifecycle:
  - create account
  - log in
  - buy food
  - play minigame
  - see updated stats

Current executable E2E artifact:

- `tests/e2e/api-smoke.ps1` (API-level smoke covering auth + verification + purchase + inventory)

## One-shot local automation

From repo **`poe-pet-app`** root (backend + frontend unit tests and production build):

```powershell
.\run-all-tests.ps1
```

Frontend unit tests live under `frontend/src/**/*.test.ts` (Vitest, `npm run test`). The E2E smoke script still requires MongoDB, Mailhog, and the API on `localhost:8080` as documented in the main README.

## Non-Functional Checks

- Basic performance:
  - simulation endpoint under repeated requests
- Security:
  - password hashing present
  - token expiration tested
  - protected routes inaccessible without auth

## Definition of Done for Any Feature

1. Spec updated (`SOURCE_OF_TRUTH.md` or supporting docs).
2. Unit tests added/updated.
3. Integration tests added where persistence/time logic exists.
4. Frontend tests added for changed UI behavior.
5. Manual smoke checklist completed.
