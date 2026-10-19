import { secondaryContextOptions } from './helpers/integration'
import { test, expect, seed, workspace, session, login } from './helpers/integration'

async function findUser(admin: import('@playwright/test').Page, email: string) {
  await admin.getByRole('button', { name: 'Utenti', exact: true }).click()
  await admin.getByLabel('Cerca utenti').fill(email)
  await expect(admin.locator('tbody tr')).toHaveCount(1)
  await expect(admin.getByRole('cell').filter({ hasText: email })).toBeVisible()
}

test('administrator suspends and reactivates a real user while preserving its workspace and audit trail', async ({
  signedIn: page,
  admin,
  user,
}) => {
  const saved = await seed(page)
  await findUser(admin, user.email)
  await admin.getByRole('button', { name: `Sospendi ${user.email}`, exact: true }).click()
  await admin
    .getByRole('dialog')
    .getByRole('button', { name: 'Sospendi account', exact: true })
    .click()
  await expect(admin.getByRole('dialog')).toHaveCount(0)
  await expect(admin.getByText('Sospeso', { exact: true })).toBeVisible()
  expect((await page.request.get('/api/workspace')).status()).toBe(401)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
  await page.getByLabel('Email', { exact: true }).fill(user.email)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
  await expect(page.getByRole('alert')).toContainText('sospeso')
  await admin.getByRole('button', { name: `Riattiva ${user.email}`, exact: true }).click()
  await admin
    .getByRole('dialog')
    .getByRole('button', { name: 'Riattiva account', exact: true })
    .click()
  await expect(admin.getByRole('dialog')).toHaveCount(0)
  await login(page, user)
  expect((await workspace(page.request)).snapshot).toEqual(saved)
  const audit = await (await admin.request.get('/api/admin/audit')).json()
  expect(
    audit
      .filter((a: { targetId: string }) => a.targetId === user.account.userId)
      .map((a: { action: string }) => a.action),
  ).toEqual(expect.arrayContaining(['USER_SUSPENDED', 'USER_ENABLED']))
})

test('administrator session revocation signs out every browser session of the target account', async ({
  signedIn: page,
  admin,
  user,
  browser,
  baseURL,
}) => {
  const other = await browser.newContext({ baseURL, ...secondaryContextOptions() })
  try {
    const second = await other.newPage()
    await login(second, user)
    await findUser(admin, user.email)
    await admin
      .getByRole('button', { name: `Revoca sessioni di ${user.email}`, exact: true })
      .click()
    await admin
      .getByRole('dialog')
      .getByRole('button', { name: 'Revoca sessioni', exact: true })
      .click()
    await expect(admin.getByRole('dialog')).toHaveCount(0)
    for (const client of [page, second]) {
      expect((await client.request.get('/api/web/auth/me')).status()).toBe(401)
      await client.reload()
      await expect(client.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
    }
    await login(page, user)
    expect((await session(page)).account.userId).toBe(user.account.userId)
  } finally {
    await other.close()
  }
})

test('changing real user permissions disables writes in the UI and rejects direct API attempts', async ({
  signedIn: page,
  admin,
  user,
}) => {
  const saved = await seed(page)
  await findUser(admin, user.email)
  await admin.getByRole('button', { name: 'Permessi', exact: true }).click()
  await admin.getByLabel('Modificare piani e pasti').uncheck()
  await admin.getByLabel('Modificare la spesa').uncheck()
  await admin.getByLabel('Modificare il profilo').uncheck()
  await admin.getByLabel('Gestire i backup Google Drive').uncheck()
  await admin.getByRole('button', { name: 'Salva permessi', exact: true }).click()
  await expect(admin.getByRole('status')).toContainText('Decisione registrata')
  await page.reload()
  await page.getByRole('link', { name: 'I miei piani', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Nuovo piano' })).toBeDisabled()
  await page.getByRole('button', { name: 'Modifica Piano completo', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  const current = await workspace(page.request)
  const me = await session(page)
  const headers = { 'X-CSRF-Token': me.csrfToken, 'X-DietReminder-Web': '1' }
  const forbidden = await page.request.put('/api/workspace', {
    headers,
    data: {
      baseVersion: current.version,
      requestId: crypto.randomUUID(),
      snapshot: {
        ...current.snapshot,
        diets: current.snapshot.diets.map((d) => ({ ...d, name: 'Forbidden edit' })),
      },
    },
  })
  expect(forbidden.status()).toBe(403)
  expect((await forbidden.json()).code).toBe('PERMISSION_DENIED')
  await page.getByRole('link', { name: 'La spesa', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Nuova lista' })).toBeDisabled()
  expect(
    (
      await page.request.post('/api/user/profile', { headers, data: { username: 'Forbidden' } })
    ).status(),
  ).toBe(403)
  expect((await page.request.get('/api/backups/google')).status()).toBe(403)
  expect((await page.request.get('/api/admin/users')).status()).toBe(403)
  await page.goto('/#admin')
  await expect(page.getByRole('heading', { name: 'Uno spazio riservato' })).toBeVisible()
  expect((await workspace(page.request)).snapshot).toEqual(saved)
})

test('release metadata is code-owned and the administrator has no update editor', async ({
  admin,
}) => {
  await expect(admin.getByRole('button', { name: 'Aggiornamenti', exact: true })).toHaveCount(0)
  const response = await admin.request.get('/api/app/version-policy')
  expect(response.status()).toBe(200)
  const policy = await response.json()
  expect(policy).toMatchObject({
    deprecatedVersionCode: 5,
    latestVersionCode: 6,
    latestVersionName: '2.0.1',
  })
  expect([1, 2, 3, 4].every((code) => code < policy.deprecatedVersionCode)).toBe(true)
  const me = await session(admin)
  const removed = await admin.request.put('/api/admin/version-policy', {
    headers: { 'X-CSRF-Token': me.csrfToken, 'X-DietReminder-Web': '1' },
    data: { revision: 1, policy: { ...policy, latestVersionCode: 100 } },
  })
  expect(removed.status()).toBe(401)
  expect((await admin.request.get('/api/web/auth/me')).status()).toBe(200)
  expect(await (await admin.request.get('/api/app/version-policy')).json()).toEqual(policy)
})
