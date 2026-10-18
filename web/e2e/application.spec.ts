import { completeEnrollment } from './helpers/enrollment'
import { test, expect } from '@playwright/test'
import { writeFile } from 'node:fs/promises'

test('real account, workspace edits, shopping, session persistence and responsive settings', async ({
  page,
}, info) => {
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  await page.goto('/')
  await page.getByRole('button', { name: 'Crea account', exact: true }).click()
  await page.getByLabel('Come ti chiami?').fill('Browser E2E')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill('Browser-e2e-password-2026')
  await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
  await completeEnrollment(page, email)
  await expect(page.getByRole('heading', { name: 'La tua settimana.' })).toBeVisible()
  const me = (await (await page.request.get('/api/web/auth/me')).json()) as {
    account: { userId: string; role: 'USER' | 'ADMIN' }
  }
  await writeFile(
    info.outputPath('created-user.json'),
    JSON.stringify({ id: me.account.userId, email }),
  )
  await page.getByRole('link', { name: 'I miei piani' }).click()
  await page.getByRole('button', { name: 'Nuovo piano' }).click()
  await page.getByLabel('Nome del piano').fill('Il piano del browser')
  await page.getByRole('button', { name: 'Salva piano' }).click()
  await expect(page.getByRole('heading', { name: 'Il piano del browser' })).toBeVisible()
  await page.getByRole('link', { name: 'La settimana', exact: true }).click()
  await page.getByRole('button', { name: 'Lunedì', exact: true }).click()
  await page.getByRole('button', { name: 'Aggiungi pasto', exact: true }).click()
  await page.getByLabel('Pasto', { exact: true }).selectOption('LUNCH')
  await page.getByLabel('Orario').fill('12:30')
  await page.getByLabel('Una nota per te').fill('Un pranzo semplice, dal web')
  await page.getByRole('button', { name: 'Aggiungi', exact: true }).click()
  await page.getByRole('textbox', { name: 'Ingrediente 1', exact: true }).fill('Pomodori')
  await page.getByRole('textbox', { name: 'Quantità 1', exact: true }).fill('150')
  await page.getByLabel('Nome della portata 1', { exact: true }).fill('Primo')
  await page.getByRole('button', { name: 'Aggiungi portata', exact: true }).click()
  await page.getByLabel('Nome della portata 2', { exact: true }).fill('Contorno')
  await page
    .getByRole('button', { name: 'Aggiungi ingrediente alla portata 2', exact: true })
    .click()
  await page.getByRole('textbox', { name: 'Ingrediente 2', exact: true }).fill('Zucchine')
  await page.getByRole('textbox', { name: 'Quantità 2', exact: true }).fill('200')
  await page.getByRole('button', { name: 'Sposta su portata 2', exact: true }).click()
  await page.getByLabel('Promemoria per questo pasto').uncheck()
  await page.setViewportSize({ width: 393, height: 852 })
  expect(
    await page
      .getByRole('region', { name: 'Editor pasto' })
      .evaluate((el) => el.scrollWidth <= el.clientWidth + 1),
  ).toBeTruthy()
  await page.screenshot({
    path: '/tmp/dietreminder-web-courses-mobile.png',
    animations: 'disabled',
  })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.screenshot({ path: '/tmp/dietreminder-web-courses.png', animations: 'disabled' })
  await page.getByRole('button', { name: 'Salva pasto' }).click()
  await expect(page.getByRole('region', { name: 'Editor pasto' })).not.toBeVisible()
  await page.locator('summary').filter({ hasText: 'Pranzo' }).click()
  await expect(page.getByText('Pomodori', { exact: true })).toBeVisible()
  const workspace = await (await page.request.get('/api/workspace')).json()
  expect(workspace.snapshot.foods[0].name).toBe('Pomodori')
  expect(workspace.snapshot.meals[0].timeMinutes).toBe(750)
  expect(workspace.snapshot.meals[0].isNotificationEnabled).toBe(false)
  expect(workspace.snapshot.courses.map((c: { name: string }) => c.name)).toEqual([
    'Contorno',
    'Primo',
  ])
  const firstCourse = workspace.snapshot.courses.find((c: { name: string }) => c.name === 'Primo')
  expect(
    workspace.snapshot.foods.find((f: { name: string }) => f.name === 'Pomodori').courseId,
  ).toBe(firstCourse.id)
  // A native client can publish a newer revision while a browser form is open.
  await page.getByRole('button', { name: 'Modifica', exact: true }).click()
  await page.getByLabel('Una nota per te').fill('Una modifica locale non ancora salvata')
  const remote = structuredClone(workspace.snapshot)
  remote.meals[0].description = 'Modifica arrivata da un altro dispositivo'
  const remoteWrite = await page.request.put('/api/workspace', {
    headers: { 'X-CSRF-Token': me.csrfToken, 'X-DietReminder-Web': '1' },
    data: { baseVersion: workspace.version, requestId: crypto.randomUUID(), snapshot: remote },
  })
  expect(remoteWrite.status()).toBe(200)
  await page.getByRole('button', { name: 'Salva pasto' }).click()
  await expect(page.getByRole('region', { name: 'Editor pasto' }).getByRole('alert')).toContainText(
    'I dati sono cambiati',
  )
  expect(
    (await (await page.request.get('/api/workspace')).json()).snapshot.meals[0].description,
  ).toBe(remote.meals[0].description)
  await page.getByRole('button', { name: 'Torna alla settimana', exact: true }).click()
  await page.getByRole('button', { name: 'Riprova', exact: true }).click()
  await expect(page.getByText(remote.meals[0].description, { exact: true })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('heading', { name: 'La tua settimana.' })).toBeVisible()
  await page.getByRole('link', { name: 'La spesa', exact: true }).click()
  await page.getByRole('button', { name: 'Nuova lista' }).click()
  await page.getByLabel('Nome della lista').fill('Spesa dal web')
  await page.getByRole('button', { name: 'Crea lista' }).click()
  await page.getByLabel('Nuovo ingrediente').fill('Mele')
  await page.getByLabel('Quantità', { exact: true }).fill('2')
  await page.getByLabel('Unità', { exact: true }).selectOption('PIECES')
  await page.getByRole('button', { name: 'Aggiungi ingrediente', exact: true }).click()
  await page.getByRole('button', { name: 'Acquistato: Mele' }).click()
  await expect(page.getByRole('button', { name: 'Da acquistare: Mele' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  const saved = await (await page.request.get('/api/workspace')).json()
  const native = structuredClone(saved.snapshot)
  native.items.push({
    id: 800,
    shoppingListId: native.lists[0].id,
    name: 'Insalata fresca',
    amount: '',
    unit: 'GRAMS',
    isBought: false,
    isCustom: false,
    isFresh: true,
    dayOfWeek: null,
  })
  native.days.push(
    ...[1, 2].map((day) => ({
      id: 800 + day,
      shoppingListItemId: 800,
      dayOfWeek: day,
      amount: '150',
      unit: 'GRAMS',
      isBought: false,
    })),
  )
  expect(
    (
      await page.request.put('/api/workspace', {
        headers: { 'X-CSRF-Token': me.csrfToken, 'X-DietReminder-Web': '1' },
        data: { baseVersion: saved.version, requestId: crypto.randomUUID(), snapshot: native },
      })
    ).status(),
  ).toBe(200)
  await page.reload()
  await page.getByRole('button', { name: 'Organizza Insalata fresca, Lunedì', exact: true }).click()
  await page.getByLabel('Categoria', { exact: true }).fill('Orto e mercato')
  await page
    .getByRole('region', { name: 'Organizza alimento' })
    .getByLabel('Giorno d’acquisto', { exact: true })
    .selectOption('7')
  await page
    .getByRole('region', { name: 'Organizza alimento' })
    .getByLabel('Quantità', { exact: true })
    .fill('175')
  await page.getByRole('button', { name: 'Salva alimento', exact: true }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  await page.getByRole('button', { name: 'Domenica', exact: true }).click()
  await expect(
    page.getByRole('button', { name: 'Acquistato: Insalata fresca, Lunedì', exact: true }),
  ).toBeVisible()
  await expect(page.getByText('Orto e mercato', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Tutti', exact: true }).click()
  const moved = (await (await page.request.get('/api/workspace')).json()).snapshot.days.find(
    (day: { id: number }) => day.id === 801,
  )
  expect(moved.dayOfWeek).toBe(1)
  expect(moved.purchaseDayOfWeek).toBe(7)
  expect(moved.amount).toBe('175')
  await page.getByRole('button', { name: 'Promemoria spesa', exact: true }).click()
  await page.getByLabel('Ricordami gli alimenti per domani').check()
  await page.getByLabel('Orario indicativo').fill('18:30')
  await page.getByRole('button', { name: 'Salva promemoria', exact: true }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  const reminded = (await (await page.request.get('/api/workspace')).json()).snapshot.lists[0]
  expect(reminded.remindersEnabled).toBe(true)
  expect(reminded.reminderTimeMinutes).toBe(1110)
  await page
    .getByRole('button', { name: 'Acquistato: Insalata fresca, Lunedì', exact: true })
    .click()
  await expect(
    page.getByRole('button', { name: 'Da acquistare: Insalata fresca, Lunedì', exact: true }),
  ).toHaveAttribute('aria-pressed', 'true')
  let roundtrip = await (await page.request.get('/api/workspace')).json()
  expect(roundtrip.snapshot.items.find((i: { id: number }) => i.id === 800).isBought).toBe(false)
  expect(roundtrip.snapshot.days.map((d: { isBought: boolean }) => d.isBought)).toEqual([
    true,
    false,
  ])
  await page
    .getByRole('button', { name: 'Acquistato: Insalata fresca, Martedì', exact: true })
    .click()
  await expect(
    page.getByRole('button', { name: 'Da acquistare: Insalata fresca, Martedì', exact: true }),
  ).toHaveAttribute('aria-pressed', 'true')
  roundtrip = await (await page.request.get('/api/workspace')).json()
  expect(roundtrip.snapshot.days.every((d: { isBought: boolean }) => d.isBought)).toBe(true)
  expect(roundtrip.snapshot.courses).toEqual(native.courses)
  expect(roundtrip.snapshot.foods).toEqual(native.foods)
  await page.screenshot({
    path: '/tmp/dietreminder-web-shopping-brand.png',
    animations: 'disabled',
  })
  await page.setViewportSize({ width: 393, height: 852 })
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
  ).toBeTruthy()
  await page.screenshot({
    path: '/tmp/dietreminder-shopping-web-mobile.png',
    fullPage: true,
    animations: 'disabled',
  })
  await page.setViewportSize({ width: 1440, height: 1000 })
  const isAdmin = me.account.role === 'ADMIN'
  expect((await page.request.get('/api/admin/overview')).status()).toBe(isAdmin ? 200 : 403)
  await page.goto('/#admin')
  if (isAdmin)
    await expect(page.getByRole('button', { name: 'Panoramica', exact: true })).toBeVisible()
  else await expect(page.getByRole('heading', { name: 'Uno spazio riservato' })).toBeVisible()
  await page.getByRole('link', { name: 'Impostazioni' }).click()
  await page.getByRole('switch', { name: 'Interfaccia compatta' }).check()
  await page.getByRole('button', { name: 'Chiaro', exact: true }).click()
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-compact', 'true')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.setViewportSize({ width: 393, height: 852 })
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
  ).toBeTruthy()
  await page.screenshot({
    path: '/tmp/dietreminder-web-mobile.png',
    fullPage: true,
    animations: 'disabled',
  })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.getByRole('button', { name: 'Esci dall’account' }).click()
  await expect(page.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
  expect((await page.request.get('/api/web/auth/me')).status()).toBe(401)
})

test('administrator dashboard supports users and validated release forms without provisioning real administrators', async ({
  page,
}) => {
  const user = {
    id: 'fixture-user',
    email: 'utente@example.test',
    username: 'Utente di test',
    role: 'USER',
    enabled: true,
    googleLinked: false,
    createdAt: Date.now(),
  }
  let policy = {
    revision: 1,
    policy: {
      minSupportedVersionCode: 1,
      deprecatedVersionCode: 1,
      latestVersionCode: 4,
      latestVersionName: '1.3.0',
      minSupportedMajor: 1,
      updateUrl: 'https://example.test/releases',
    },
  }
  let suspended = false
  await page.route('**/api/**', async (route) => {
    const req = route.request()
    const url = new URL(req.url())
    let data: unknown
    if (url.pathname === '/api/web/auth/me')
      data = {
        account: {
          userId: 'fixture-admin',
          username: 'Amministratore',
          email: 'admin@example.test',
          role: 'ADMIN',
          googleLinked: true,
        },
        csrfToken: 'fixture-csrf',
      }
    else if (url.pathname === '/api/auth/providers')
      data = { clientId: '', signInEnabled: false, driveEnabled: false }
    else if (url.pathname === '/api/workspace')
      data = {
        version: 0,
        snapshot: {
          schemaVersion: 1,
          diets: [],
          meals: [],
          courses: [],
          foods: [],
          lists: [],
          items: [],
          days: [],
        },
      }
    else if (url.pathname === '/api/admin/overview')
      data = {
        users: 12,
        activeUsers: 11,
        googleUsers: 7,
        workspaces: 9,
        driveConnections: 4,
        uptimeSeconds: 120,
      }
    else if (url.pathname === '/api/admin/audit')
      data = [
        {
          id: 1,
          action: 'USER_ENABLED',
          actorId: 'fixture-admin',
          targetId: user.id,
          createdAt: Date.now(),
        },
      ]
    else if (url.pathname === '/api/admin/users')
      data = {
        users:
          user.email.includes(url.searchParams.get('search') ?? '') ||
          user.username.includes(url.searchParams.get('search') ?? '')
            ? [user]
            : [],
        total: 1,
        page: 0,
        pages: 1,
      }
    else if (url.pathname.endsWith('/status')) {
      user.enabled = req.postDataJSON().enabled
      suspended = !user.enabled
      data = user
    } else if (url.pathname === '/api/admin/version-policy') {
      if (req.method() === 'PUT') {
        const body = req.postDataJSON()
        expect(body.revision).toBe(policy.revision)
        policy = { ...body, revision: policy.revision + 1 }
      }
      data = policy
    } else throw new Error(`Unexpected API request ${req.method()} ${url.pathname}`)
    await route.fulfill({ json: data })
  })
  await page.goto('/#admin')
  await expect(page.getByRole('heading', { name: 'Tutto sotto controllo.' })).toBeVisible()
  await expect(page.getByText('Utenti registrati', { exact: true })).toBeVisible()
  await page.screenshot({
    path: '/tmp/dietreminder-web-admin.png',
    fullPage: true,
    animations: 'disabled',
  })
  await page.getByRole('button', { name: 'Utenti', exact: true }).click()
  await page.getByRole('textbox', { name: 'Cerca utenti' }).fill('utente@example.test')
  await expect(page.getByText('utente@example.test', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Sospendi utente@example.test' }).click()
  await page.getByRole('button', { name: 'Sospendi account', exact: true }).click()
  await expect(page.getByText('Sospeso', { exact: true })).toBeVisible()
  expect(suspended).toBeTruthy()
  await page.getByRole('button', { name: 'Aggiornamenti', exact: true }).click()
  await page.getByLabel('Ultima versione', { exact: true }).fill('1.4.0')
  await page.getByLabel('Codice ultima versione', { exact: true }).fill('5')
  await page.getByRole('button', { name: 'Pubblica informazioni' }).click()
  await expect(page.getByText('Informazioni pubblicate.', { exact: false })).toBeVisible()
  expect(policy.policy.latestVersionCode).toBe(5)
  await page.setViewportSize({ width: 393, height: 852 })
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
  ).toBeTruthy()
})
