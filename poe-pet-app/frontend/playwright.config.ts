import { defineConfig, devices } from '@playwright/test'

/**
 * Browser E2E against a running stack (Vite + Spring + Mongo + MailHog + …).
 * Start the app first (e.g. poe-pet-app/start-all.ps1 + docker compose), then:
 *   cd frontend && npx playwright install chromium
 *   npm run test:e2e
 *
 * Env:
 * - PLAYWRIGHT_BASE_URL (default http://localhost:5173)
 * - PLAYWRIGHT_API_URL (default http://localhost:8080)
 * - PLAYWRIGHT_MAILHOG_URL (default http://localhost:8025)
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
