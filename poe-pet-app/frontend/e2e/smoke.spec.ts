import { test, expect } from '@playwright/test'
import { fetchVerificationTokenForEmail, registerUser, verifyEmail } from './helpers'

test.describe('app smoke', () => {
  test('register via API, verify MailHog, login in UI, open shop and chat', async ({ page }) => {
    const email = `pw${Date.now()}@example.com`
    const password = 'Abcde1'

    await registerUser(email, password)
    const token = await fetchVerificationTokenForEmail(email)
    await verifyEmail(token)

    await page.goto('/login')
    await page.getByPlaceholder('Email').fill(email)
    await page.getByPlaceholder('Password').fill(password)
    await page.getByRole('button', { name: /^login$/i }).click()

    await expect(page).toHaveURL(/\/app\/shop/)
    await expect(page.getByTestId('shop-heading-consumables')).toBeVisible()
    await expect(page.getByTestId('shop-heading-pets')).toBeVisible()
    await expect(page.getByTestId('shop-heading-cosmetics')).toBeVisible()

    await page.getByRole('link', { name: 'Chat' }).click()
    await expect(page.getByTestId('chat-input')).toBeVisible()
    await page.getByTestId('chat-input').fill('Hello friend!')
    await page.getByTestId('chat-send').click()
    await expect(page.getByTestId('chat-log')).toContainText('Hello friend!', { timeout: 20_000 })
  })
})
