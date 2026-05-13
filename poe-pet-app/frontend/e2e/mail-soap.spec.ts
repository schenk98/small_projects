import { test, expect } from '@playwright/test'
import { loginAccessToken, waitForMailhogSubjectContaining } from './helpers'

/**
 * Sends email through the SOAP notification sidecar (same SMTP/MailHog as auth).
 * Requires a **privileged** account (APP_PRIVILEGED_EMAILS or Mongo users.privileged).
 *
 *   E2E_SOAP_EMAIL=you@test.local E2E_SOAP_PASSWORD='YourPass1' npm run test:e2e -- e2e/mail-soap.spec.ts
 */
test.describe('soap mail (optional)', () => {
  test('low-hunger dev trigger reaches MailHog', async ({ request }) => {
    const email = process.env.E2E_SOAP_EMAIL
    const password = process.env.E2E_SOAP_PASSWORD
    test.skip(!email || !password, 'Set E2E_SOAP_EMAIL and E2E_SOAP_PASSWORD to a privileged, verified user')

    const api = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080'
    const token = await loginAccessToken(email, password)

    const drainRes = await request.post(`${api}/api/dev/set-stats`, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      data: JSON.stringify({ hungerPercent: 0, happinessPercent: 1, energyPercent: 1 }),
    })
    expect(drainRes.ok(), await drainRes.text()).toBeTruthy()

    const notify = await request.post(`${api}/api/dev/notifications/test-low-hunger`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(notify.ok(), await notify.text()).toBeTruthy()
    const body = (await notify.json()) as { accepted?: boolean; message?: string }
    expect(body.accepted, JSON.stringify(body)).toBe(true)

    await waitForMailhogSubjectContaining('hungry')
  })
})
