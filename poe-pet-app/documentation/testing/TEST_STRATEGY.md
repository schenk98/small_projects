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

### Email / SMTP coverage (current)

Mail touches **three** layers; tests should stay fast (mocks) with one E2E assertion against MailHog where useful:

| Path | What sends mail | Automated coverage |
|------|-----------------|-------------------|
| Register + forgot password | Main backend `AppService` → `JavaMailSender` | `AppServiceMailTest` (verifies `send` + subject/body) |
| Low-hunger + daily summary | Main backend `NotificationAutomationService` → `NotificationSoapClient` (HTTP SOAP) | `NotificationAutomationServiceTest` (SOAP result + dedupe behavior) |
| SOAP → `SimpleMailMessage` | `notification-soap-service` `NotificationMailService` | `NotificationMailServiceTest`, `NotificationEndpointTest`, XML binding test |
| Full stack dev | MailHog inbox | `tests/e2e/api-smoke.ps1` pulls MailHog REST API and parses the **verification** email token; Playwright `frontend/e2e/smoke.spec.ts` covers UI login + shop + chat; optional `frontend/e2e/mail-soap.spec.ts` asserts **SOAP** low-hunger mail in MailHog when run with a privileged test account (`E2E_SOAP_EMAIL` / `E2E_SOAP_PASSWORD`) |

Gaps (acceptable for PoC): no automated test yet that waits for a **scheduled** notification sweep; Playwright SOAP test requires a one-off privileged user and a clean delivery key for that UTC day (or it may see `already_sent_today`).

### Unit Tests (JUnit + Mockito)

- simulation service:
  - hunger/happiness/energy formulas
  - low-hunger penalties
  - boost modifier application and expiry
- minigame payout calculator
- item effect handlers
- auth token service
- SQL projection / progression services:
  - achievement progress advancement from events
  - daily challenge generation and per-user reward progression
  - notification preference read/write defaults
  - progress/history query assembly
  - notification automation decision logic and dedupe behavior
  - SOAP notification client/service result handling
  - AI chat sanitization + assistant length clamp (`AppServiceAiChatTest`)

### Side-Service Tests (notification SOAP service)

- SOAP endpoint delegates correctly to the mail sender
- mail sender returns accepted/rejected responses without leaking transport exceptions

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
- current high-value examples:
  - progress summary endpoint shape (daily challenges + achievements + recent activity)
  - notification preference GET/POST shape
  - `GET /api/ai/info` gateway summary

## Frontend Test Layers

### Component Tests (Vitest + React Testing Library)

- stat bars and status indicators
- shop item rows (consumables + pets + cosmetics sections)
- layered pet stage (background URL / default gradient, foreground optional)
- customize: mood slot options filtered by ownership
- minigame widgets
- auth forms
- progress screen (daily challenges + achievements + recent activity rendering)
- settings screen notification toggles and save flow

### E2E Smoke (Playwright + PowerShell)

- **Playwright** (`frontend/`, `npm run test:e2e`): UI path register → MailHog verify (via API) → login → Shop sections → Chat send. Requires stack up (`PLAYWRIGHT_BASE_URL`, `PLAYWRIGHT_API_URL`, `PLAYWRIGHT_MAILHOG_URL`). First run: `cd frontend && npx playwright install chromium`.
- **Optional SOAP mail** (`frontend/e2e/mail-soap.spec.ts`): privileged user env `E2E_SOAP_EMAIL` / `E2E_SOAP_PASSWORD`, SOAP + MailHog enabled; asserts a low-hunger message lands in MailHog (subject contains `hungry`). May fail if that user already received the UTC-day delivery (dedupe).

Legacy API-only smokes:

- `tests/e2e/api-smoke.ps1` (API-level smoke covering auth + verification + purchase + inventory + progress summary + notification preference flows)
- `tests/e2e/container-stack-smoke.ps1` (build + boot + smoke-test the full Compose stack, including frontend/backend containers)

## One-shot local automation

From repo **`poe-pet-app`** root (backend + notification SOAP service + frontend unit tests and production build):

```powershell
.\run-all-tests.ps1
```

Frontend automated tests live under `frontend/src/**/*.test.ts` and `frontend/src/**/*.test.tsx` (Vitest, `npm run test`). The E2E smoke script still requires MongoDB, PostgreSQL, MailHog, and the API on `localhost:8080` as documented in the main README.
The container-stack smoke script additionally validates the deployable Compose path instead of the local mixed dev-server path.

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
