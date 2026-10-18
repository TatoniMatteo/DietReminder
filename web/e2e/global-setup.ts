import { request, expect, chromium, type FullConfig } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { emailCode } from './helpers/enrollment'
export default async function setup(config: FullConfig) {
  const api = await request.newContext({ baseURL: config.projects[0].use.baseURL })
  try {
    if (!process.env.E2E_ADMIN_EMAIL || !process.env.E2E_ADMIN_PASSWORD) {
      const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
      const password = `Owner-${crypto.randomUUID()}!`
      const configuration = await (await api.get('/api/auth/providers')).json()
      if (configuration.firstRegistrationRequired) {
        const browser = await chromium.launch()
        try {
          const page = await browser.newPage({ locale: 'it-IT' })
          await page.goto(config.projects[0].use.baseURL!)
          await expect(
            page.getByRole('heading', { name: 'Sei il primo a registrarti!' }),
          ).toBeVisible()
          await expect(page.getByLabel('Come ti chiami?')).toBeVisible()
          await expect(page.getByRole('button', { name: 'Accedi', exact: true })).toHaveCount(0)
          await expect(page.getByRole('button', { name: 'Password dimenticata?' })).toHaveCount(0)
          await expect(page.getByRole('button', { name: 'Crea il tuo spazio' })).toBeVisible()
          await page.screenshot({
            path: '/tmp/dietreminder-first-registration.png',
            fullPage: true,
          })
        } finally {
          await browser.close()
        }
      }
      const response = await api.post('/api/auth/register', {
        data: { email, password, username: 'Browser test administrator' },
      })
      expect(response.ok()).toBe(true)
      const session = await response.json()
      const output = join(config.projects[0].outputDir, 'bootstrap')
      await mkdir(output, { recursive: true })
      await writeFile(
        join(output, 'created-user.json'),
        JSON.stringify({ id: session.account.userId, email }),
      )
      if (session.account.role !== 'ADMIN')
        throw new Error(
          'Browser tests require a fresh isolated installation or E2E_ADMIN_EMAIL and E2E_ADMIN_PASSWORD for an already approved administrator. The test account cannot approve itself.',
        )
      const code = await emailCode(api, email)
      const verified = await api.post('/api/auth/verify-email', {
        headers: { Authorization: `Bearer ${session.token}` },
        data: { token: code },
      })
      expect(verified.ok()).toBe(true)
      process.env.E2E_ADMIN_EMAIL = email
      process.env.E2E_ADMIN_PASSWORD = password
    }
    const login = await api.post('/api/auth/login', {
      data: { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD },
    })
    expect(login.ok()).toBe(true)
    const account = (await login.json()).account
    expect(account.role).toBe('ADMIN')
    expect(account.emailVerified).toBe(true)
    expect(account.enrollmentStatus).toBe('APPROVED')
  } finally {
    await api.dispose()
  }
}
