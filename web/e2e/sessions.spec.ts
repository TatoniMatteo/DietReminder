import { secondaryContextOptions } from './helpers/integration'
import {
  test,
  expect,
  login,
  createUser,
  createPlan,
  monday,
  seed,
  session,
  workspace,
  writeWorkspace,
} from './helpers/integration'

test('two browser sessions detect stale meal edits and preserve the first saved revision', async ({
  signedIn: page,
  browser,
  user,
  baseURL,
}) => {
  await seed(page)
  const other = await browser.newContext({ baseURL, ...secondaryContextOptions() })
  try {
    const second = await other.newPage()
    await login(second, user)
    for (const editor of [page, second]) {
      await monday(editor)
      await editor.locator('summary').click()
      await editor.getByRole('button', { name: 'Modifica', exact: true }).click()
    }
    await page.getByLabel('Una nota per te').fill('Salvato dalla prima sessione')
    await page.getByRole('button', { name: 'Salva pasto' }).click()
    await expect(page.getByRole('region', { name: 'Editor pasto' })).toHaveCount(0)
    await second.getByLabel('Una nota per te').fill('Bozza della seconda sessione')
    await second.getByRole('button', { name: 'Salva pasto' }).click()
    await expect(
      second.getByRole('region', { name: 'Editor pasto' }).getByRole('alert'),
    ).toContainText('I dati sono cambiati')
    expect((await workspace(second.request)).snapshot.meals[0].description).toBe(
      'Salvato dalla prima sessione',
    )
    await expect(second.getByLabel('Una nota per te')).toHaveValue('Bozza della seconda sessione')
    await second.getByRole('button', { name: 'Torna alla settimana', exact: true }).click()
    await second.getByRole('button', { name: 'Riprova', exact: true }).click()
    await expect(second.getByText('Salvato dalla prima sessione', { exact: true })).toBeVisible()
  } finally {
    await other.close()
  }
})

test('workspaces and profiles remain isolated between two simultaneously signed in accounts', async ({
  signedIn: page,
  request,
  browser,
  baseURL,
}) => {
  await createPlan(page, 'Privato primo account')
  const secondUser = await createUser(request)
  const other = await browser.newContext({ baseURL, ...secondaryContextOptions() })
  try {
    const second = await other.newPage()
    await login(second, secondUser)
    expect((await workspace(second.request)).snapshot.diets).toEqual([])
    await createPlan(second, 'Privato secondo account')
    await page.reload()
    await second.reload()
    await expect(page.getByText('Privato secondo account', { exact: true })).toHaveCount(0)
    await expect(second.getByText('Privato primo account', { exact: true })).toHaveCount(0)
    expect((await workspace(page.request)).snapshot.diets.map((d) => d.name)).toEqual([
      'Privato primo account',
    ])
    expect((await workspace(second.request)).snapshot.diets.map((d) => d.name)).toEqual([
      'Privato secondo account',
    ])
    expect((await session(page)).account.userId).not.toBe((await session(second)).account.userId)
  } finally {
    await other.close()
  }
})

test('a connection loss does not save an optimistic edit and a real backend retry persists it', async ({
  signedIn: page,
  context,
}) => {
  await seed(page)
  await monday(page)
  await page.locator('summary').click()
  await page.getByRole('button', { name: 'Modifica', exact: true }).click()
  await page.getByLabel('Una nota per te').fill('Salvato dopo riconnessione')
  await context.setOffline(true)
  await page.getByRole('button', { name: 'Salva pasto', exact: true }).click()
  await expect(page.getByRole('region', { name: 'Editor pasto' }).getByRole('alert')).toContainText(
    'Il server non è raggiungibile',
  )
  await expect(page.getByLabel('Una nota per te')).toHaveValue('Salvato dopo riconnessione')
  await context.setOffline(false)
  expect((await workspace(page.request)).snapshot.meals[0].description).toBe('Pranzo originale')
  await page.getByRole('button', { name: 'Salva pasto', exact: true }).click()
  await expect(page.getByRole('region', { name: 'Editor pasto' })).toHaveCount(0)
  await page.reload()
  expect((await workspace(page.request)).snapshot.meals[0].description).toBe(
    'Salvato dopo riconnessione',
  )
})

test('cookie authentication rejects writes without the matching CSRF token and keeps data intact', async ({
  signedIn: page,
  context,
}) => {
  await seed(page)
  const current = await workspace(page.request)
  const me = await session(page)
  const cookies = await context.cookies()
  const cookie = cookies.find((c) => c.name === 'dietreminder_session')!
  expect(cookie.httpOnly).toBe(true)
  expect(cookie.sameSite).toBe('Lax')
  expect(await page.evaluate(() => document.cookie)).not.toContain('dietreminder_session')
  for (const csrf of ['', 'invalid-token']) {
    const response = await page.request.put('/api/workspace', {
      headers: { 'X-DietReminder-Web': '1', 'X-CSRF-Token': csrf },
      data: {
        baseVersion: current.version,
        requestId: crypto.randomUUID(),
        snapshot: { ...current.snapshot, diets: [] },
      },
    })
    expect(response.status()).toBe(403)
    expect((await response.json()).code).toBe('INVALID_CSRF')
  }
  expect(await workspace(page.request)).toEqual(current)
  expect(me.csrfToken).not.toBe('')
  expect((await writeWorkspace(page, current.snapshot)).status()).toBe(200)
})

test('logout revokes the browser session and a reload cannot reopen protected pages', async ({
  signedIn: page,
}) => {
  await createPlan(page)
  await page.getByRole('button', { name: 'Esci dall’account', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
  expect((await page.request.get('/api/workspace')).status()).toBe(401)
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(401)
  await page.goto('/#diets')
  await expect(page.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
  await expect(page.getByText('Piano integrazione', { exact: true })).toHaveCount(0)
})
