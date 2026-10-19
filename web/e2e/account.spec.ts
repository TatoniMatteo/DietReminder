import { test, expect, session } from './helpers/integration'
import { completeEnrollment } from './helpers/enrollment'

test('profile edits persist on the backend and survive logout and a new login', async ({
  signedIn: page,
  user,
}) => {
  await page.getByRole('link', { name: 'Impostazioni', exact: true }).click()
  await page.getByLabel('Come ti chiami?', { exact: true }).fill('Nome aggiornato dal browser')
  await page.getByRole('button', { name: 'Salva profilo', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('Profilo aggiornato')
  await expect(page.locator('.sidebar-profile strong')).toHaveText('Nome aggiornato dal browser')
  await expect(page.locator('.topbar .avatar')).toHaveText('N')
  const profile = await page.request.get('/api/user/profile')
  expect(profile.status()).toBe(200)
  expect((await profile.json()).username).toBe('Nome aggiornato dal browser')
  await page.reload()
  await expect(page.getByLabel('Come ti chiami?', { exact: true })).toHaveValue(
    'Nome aggiornato dal browser',
  )
  await page.getByRole('button', { name: 'Esci dall’account' }).click()
  await page.getByLabel('Email', { exact: true }).fill(user.email)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
  await expect(page.getByRole('link', { name: 'Impostazioni', exact: true })).toBeVisible()
  expect((await session(page)).account.username).toBe('Nome aggiornato dal browser')
})

test('unconfigured Google and Drive report their actual server availability without contacting Google', async ({
  signedIn: page,
}) => {
  const external: string[] = []
  page.on('request', (req) => {
    if (!['127.0.0.1', 'localhost'].includes(new URL(req.url()).hostname)) external.push(req.url())
  })
  await page.getByRole('link', { name: 'Impostazioni', exact: true }).click()
  await expect(
    page.getByText('Il backup Google sarà disponibile quando il servizio sarà configurato.'),
  ).toBeVisible()
  const status = await page.request.get('/api/backups/google/status')
  expect(status.status()).toBe(200)
  expect(await status.json()).toMatchObject({ available: false, connected: false })
  await expect(page.getByRole('button', { name: 'Collega Google Drive', exact: true })).toHaveCount(
    0,
  )
  expect(external).toEqual([])
})

test('invalid verification code keeps the account blocked; the delivered code completes enrollment', async ({
  page,
}) => {
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  await page.goto('/')
  await page.getByRole('button', { name: 'Crea account', exact: true }).click()
  await page.getByLabel('Come ti chiami?').fill('Verifica reale')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill('Browser-e2e-password-2026')
  await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
  await expect(page.getByRole('heading', { name: 'Verifica la tua email' })).toBeVisible()
  await page.getByLabel('Codice di verifica', { exact: true }).fill('codice-non-valido')
  await page.getByRole('button', { name: 'Conferma email', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Il codice non è valido')
  expect((await page.request.get('/api/workspace')).status()).toBe(403)
  await completeEnrollment(page, email)
  expect((await page.request.get('/api/workspace')).status()).toBe(200)
})

test('duplicate registration and wrong passwords show server errors without authenticating', async ({
  page,
  user,
}) => {
  await page.goto('/')
  await page.getByLabel('Email', { exact: true }).fill(user.email)
  await page.getByLabel('Password', { exact: true }).fill('Incorrect-password-2026')
  await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
  await expect(page.getByRole('alert')).toContainText('Email o password non corrette')
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(401)
  await page.getByRole('button', { name: 'Crea account', exact: true }).click()
  await page.getByLabel('Come ti chiami?').fill('Duplicato')
  await page.getByLabel('Email', { exact: true }).fill(user.email)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
  await expect(page.getByRole('alert')).toContainText('Esiste già un account')
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(401)
})
