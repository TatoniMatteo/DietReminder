import { selectOption } from './helpers/select'
import { secondaryContextOptions } from './helpers/integration'
import { test, expect } from '@playwright/test'
import { writeFile } from 'node:fs/promises'
import { administratorToken, verifyFromInbox } from './helpers/enrollment'

test('verified administrator sees its welcome once and reviews an applicant with selected permissions', async ({
  page,
  browser,
}, info) => {
  const owner = await browser.newContext(secondaryContextOptions())
  const admin = await owner.newPage()
  try {
    await admin.goto(process.env.WEB_TEST_URL ?? 'http://127.0.0.1:8088')
    await admin.getByLabel('Email', { exact: true }).fill(process.env.E2E_ADMIN_EMAIL!)
    await admin.getByLabel('Password', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD!)
    await admin.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
    await expect(admin.getByRole('link', { name: 'Amministrazione', exact: true })).toBeVisible()
    const session = await (await admin.request.get('/api/web/auth/me')).json()
    if (session.account.adminIntroductionPending) {
      await expect(admin.getByRole('dialog', { name: 'Benvenuto in Diet Reminder' })).toBeVisible()
      await expect(admin.getByText(/puoi aiutare le altre persone/)).toBeVisible()
      await expect(admin.locator('.admin-introduction-point')).toHaveCount(3)
      await admin.getByRole('button', { name: 'Ho capito, continua' }).click()
      await expect(admin.getByRole('dialog')).toHaveCount(0)
      await admin.reload()
      await expect(admin.getByRole('link', { name: 'Amministrazione', exact: true })).toBeVisible()
      await expect(admin.getByRole('dialog')).toHaveCount(0)
      const secondSession = await admin.request.post('/api/auth/login', {
        headers: { Cookie: '' },
        data: { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD },
      })
      expect((await secondSession.json()).account.adminIntroductionPending).toBe(false)
    }
    const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
    await page.goto('/')
    await page.getByRole('button', { name: 'Crea account', exact: true }).click()
    await page.getByLabel('Come ti chiami?').fill('Applicant')
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Password', { exact: true }).fill('Browser-e2e-password-2026')
    await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
    await expect(page.getByRole('heading', { name: 'Verifica la tua email' })).toBeVisible()
    const me = await (await page.request.get('/api/web/auth/me')).json()
    await writeFile(
      info.outputPath('created-user.json'),
      JSON.stringify({ id: me.account.userId, email }),
    )
    expect((await page.request.get('/api/workspace')).status()).toBe(403)
    await verifyFromInbox(page, email)
    await expect(page.getByRole('link', { name: 'La settimana', exact: true })).toHaveCount(0)
    await admin.getByRole('link', { name: 'Amministrazione', exact: true }).click()
    await admin.getByRole('button', { name: 'Utenti', exact: true }).click()
    await selectOption(
      admin.getByLabel('Stato della richiesta', { exact: true }),
      'PENDING_APPROVAL',
    )
    await admin.getByLabel('Cerca utenti').fill(email)
    await admin.getByRole('button', { name: 'Valuta richiesta', exact: true }).click()
    await admin.getByLabel('Modificare piani e pasti').uncheck()
    await admin.getByLabel('Modificare il profilo').uncheck()
    await admin.getByLabel('Gestire i backup Google Drive').uncheck()
    await admin.getByRole('button', { name: 'Approva richiesta', exact: true }).click()
    await expect(admin.getByRole('status')).toContainText('Decisione registrata')
    await page.getByRole('button', { name: 'Controlla stato' }).click()
    await expect(page.getByRole('link', { name: 'I miei piani' })).toBeVisible()
    await page.getByRole('link', { name: 'I miei piani' }).click()
    await expect(page.getByRole('button', { name: 'Nuovo piano' })).toBeDisabled()
    await expect(page.getByRole('status')).toContainText('sola lettura')
    const account = (await (await page.request.get('/api/web/auth/me')).json()).account
    expect(account.permissions).toEqual(['WORKSPACE_READ', 'SHOPPING_WRITE'])
    expect((await page.request.get('/api/backups/google')).status()).toBe(403)
    await page.screenshot({
      path: test.info().outputPath('dietreminder-approved-readonly.png'),
      fullPage: true,
    })
  } finally {
    await owner.close()
  }
})

test('rejected applicant sees its reason and cannot access the workspace', async ({
  page,
}, info) => {
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  await page.goto('/')
  await page.getByRole('button', { name: 'Crea account', exact: true }).click()
  await page.getByLabel('Come ti chiami?').fill('Rejected applicant')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill('Browser-e2e-password-2026')
  await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
  await expect(page.getByRole('heading', { name: 'Verifica la tua email' })).toBeVisible()
  const before = (await (await page.request.get('/api/web/auth/me')).json()).account
  await writeFile(
    info.outputPath('created-user.json'),
    JSON.stringify({ id: before.userId, email }),
  )
  await verifyFromInbox(page, email)
  const account = (await (await page.request.get('/api/web/auth/me')).json()).account
  const token = await administratorToken(page.request)
  const response = await page.request.post(`/api/admin/users/${account.userId}/review`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { version: account.version, approved: false, reason: 'Richiesta non riconosciuta' },
  })
  expect(response.ok()).toBe(true)
  await page.getByRole('button', { name: 'Controlla stato' }).click()
  await expect(page.getByRole('heading', { name: 'Richiesta rifiutata' })).toBeVisible()
  await expect(page.getByRole('status')).toContainText('Richiesta non riconosciuta')
  expect((await page.request.get('/api/workspace')).status()).toBe(403)
})
