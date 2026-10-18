import { completeEnrollment } from './helpers/enrollment'
import { test, expect } from '@playwright/test'
import { writeFile } from 'node:fs/promises'

test('settings language, appearance, fixed footer and inline password change work with the server', async ({
  page,
}, info) => {
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  const oldPassword = 'Browser-e2e-password-2026'
  const newPassword = 'Una nuova password!'
  await page.goto('/')
  await page.getByLabel('Lingua', { exact: true }).selectOption('en')
  await expect(page.getByRole('heading', { name: 'Welcome back.' })).toBeVisible()
  await page.getByRole('button', { name: 'Create account', exact: true }).click()
  await page.getByLabel('What’s your name?').fill('Settings E2E')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill('lowercase password!')
  await expect(page.getByRole('button', { name: 'Create your space' })).toBeDisabled()
  await expect(
    page.locator('.password-criteria li').filter({ hasText: 'At least one uppercase letter' }),
  ).toHaveAttribute('data-met', 'false')
  await page.getByLabel('Password', { exact: true }).fill(oldPassword)
  await expect(page.locator('.password-criteria li[data-met=false]')).toHaveCount(0)
  await page.getByRole('button', { name: 'Create your space' }).click()
  await completeEnrollment(page, email)
  await expect(page.getByRole('link', { name: 'Settings', exact: true })).toBeVisible()
  const me = await (await page.request.get('/api/web/auth/me')).json()
  await writeFile(
    info.outputPath('created-user.json'),
    JSON.stringify({ id: me.account.userId, email }),
  )
  await page.getByRole('link', { name: 'Settings', exact: true }).click()
  await expect(page.getByText('Daily calorie goal', { exact: false })).toHaveCount(0)
  await page.getByRole('button', { name: 'Dark', exact: true }).click()
  await page.getByRole('switch', { name: 'Compact interface' }).check()
  await page.getByRole('switch', { name: 'Reduce animations' }).check()
  await page.getByRole('button', { name: 'Lavender color', exact: true }).click()
  await expect(page.locator('html')).toHaveAttribute('data-color-mode', 'custom')
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(page.locator('html')).toHaveAttribute('data-compact', 'true')
  await expect(page.locator('html')).toHaveAttribute('data-motion', 'reduced')
  for (const viewport of [
    { width: 1440, height: 1000 },
    { width: 393, height: 852 },
  ]) {
    await page.setViewportSize(viewport)
    await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight))
    const bounds = await page.locator('.page-footer').boundingBox()
    expect(Math.abs(bounds!.y + bounds!.height - viewport.height)).toBeLessThan(2)
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
    ).toBeTruthy()
    await page.screenshot({
      path: `/tmp/dietreminder-settings-${viewport.width}.png`,
      animations: 'disabled',
    })
  }
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.getByRole('button', { name: 'Change password', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.getByLabel('Current password', { exact: true }).fill('Wrong password!')
  await page.getByLabel('New password', { exact: true }).fill(newPassword)
  await page.getByLabel('Confirm password', { exact: true }).fill(newPassword)
  await expect(
    page.locator('#change-password-form .password-criteria li[data-met=false]'),
  ).toHaveCount(0)
  await page.getByRole('button', { name: 'Update password', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('current password')
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(200)
  await page.getByLabel('Current password', { exact: true }).fill(oldPassword)
  await page.getByRole('button', { name: 'Update password', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Welcome back.' })).toBeVisible()
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(401)
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(newPassword)
  await page.getByRole('button', { name: 'Enter your space' }).click()
  await expect(page.getByRole('link', { name: 'Settings', exact: true })).toBeVisible()
  await page.getByRole('link', { name: 'Settings', exact: true }).click()
  await page.getByLabel('Language', { exact: true }).selectOption('it')
  await expect(page.getByRole('link', { name: 'Impostazioni', exact: true })).toBeVisible()
})
