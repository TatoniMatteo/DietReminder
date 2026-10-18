import { selectOption } from './select'
import { test as base, expect, type APIRequestContext, type Page } from '@playwright/test'
import { administratorToken, allPermissions, emailCode } from './enrollment'
import type { Account, Snapshot, WebSession, Workspace } from '../../src/lib/types'

export const password = 'Browser-e2e-password-2026'
export interface TestUser {
  email: string
  password: string
  account: Account
  token: string
}

// Setup goes through the real registration, SMTP verification and review APIs.
// Every test owns an account; neither workspace data nor browser cookies are shared.
export async function createUser(
  request: APIRequestContext,
  permissions = allPermissions,
): Promise<TestUser> {
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  const registered = await request.post('/api/auth/register', {
    headers: { Cookie: '' },
    data: { email, password, username: 'Integration user' },
  })
  expect(registered.ok()).toBe(true)
  const session = await registered.json()
  const verified = await request.post('/api/auth/verify-email', {
    headers: { Cookie: '', Authorization: `Bearer ${session.token}` },
    data: { token: await emailCode(request, email) },
  })
  expect(verified.ok()).toBe(true)
  const account = await verified.json()
  const approval = await request.post(`/api/admin/users/${account.userId}/review`, {
    headers: { Cookie: '', Authorization: `Bearer ${await administratorToken(request)}` },
    data: { version: account.version, approved: true, permissions },
  })
  expect(approval.ok()).toBe(true)
  return { email, password, account: await approval.json(), token: session.token }
}
export async function login(page: Page, user: Pick<TestUser, 'email' | 'password'>) {
  await page.goto('/')
  await page.getByLabel('Email', { exact: true }).fill(user.email)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
  await expect(page.getByRole('link', { name: 'La settimana', exact: true })).toBeVisible()
}
export async function workspace(request: APIRequestContext): Promise<Workspace> {
  const response = await request.get('/api/workspace')
  expect(response.status()).toBe(200)
  return response.json()
}
export async function session(page: Page): Promise<WebSession> {
  const response = await page.request.get('/api/web/auth/me')
  expect(response.status()).toBe(200)
  return response.json()
}
export async function writeWorkspace(page: Page, snapshot: Snapshot, baseVersion?: number) {
  const me = await session(page)
  return page.request.put('/api/workspace', {
    headers: { 'X-CSRF-Token': me.csrfToken, 'X-DietReminder-Web': '1' },
    data: {
      baseVersion: baseVersion ?? (await workspace(page.request)).version,
      requestId: crypto.randomUUID(),
      snapshot,
    },
  })
}
export async function createPlan(page: Page, name = 'Piano integrazione') {
  await page.getByRole('link', { name: 'I miei piani', exact: true }).click()
  await page.getByRole('button', { name: 'Nuovo piano', exact: true }).click()
  await page.getByLabel('Nome del piano').fill(name)
  await page.getByRole('button', { name: 'Salva piano' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible()
}
export async function monday(page: Page) {
  await page.getByRole('link', { name: 'La settimana', exact: true }).click()
  await page.getByRole('button', { name: 'Lunedì', exact: true }).click()
}
export async function createList(page: Page, name = 'Spesa integrazione', plan?: string) {
  await page.getByRole('link', { name: 'La spesa', exact: true }).click()
  await page.getByRole('button', { name: 'Nuova lista', exact: true }).click()
  await page.getByLabel('Nome della lista').fill(name)
  if (plan) await selectOption(page.getByLabel('Piano associato'), { label: plan })
  await page.getByRole('button', { name: 'Crea lista', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible()
}
export async function addItem(page: Page, name: string, needed = '', purchase = '') {
  await page.getByLabel('Nuovo ingrediente').fill(name)
  await page.getByLabel('Quantità', { exact: true }).fill('2')
  await selectOption(page.getByLabel('Unità', { exact: true }), 'PIECES')
  await selectOption(page.getByLabel('Giorno di utilizzo', { exact: true }), needed)
  await selectOption(page.getByLabel('Giorno d’acquisto', { exact: true }), purchase)
  await page.getByRole('button', { name: 'Aggiungi ingrediente', exact: true }).click()
  await expect(page.getByRole('button', { name: `Acquistato: ${name}`, exact: true })).toBeVisible()
}

export const test = base.extend<{ user: TestUser; signedIn: Page; admin: Page }>({
  user: async ({ request }, use) => {
    await use(await createUser(request))
  },
  signedIn: async ({ page, user }, use) => {
    await login(page, user)
    await use(page)
  },
  admin: async ({ browser, baseURL, viewport, isMobile, hasTouch }, use) => {
    const context = await browser.newContext({
      baseURL,
      locale: 'it-IT',
      viewport,
      isMobile,
      hasTouch,
    })
    try {
      const page = await context.newPage()
      await login(page, {
        email: process.env.E2E_ADMIN_EMAIL!,
        password: process.env.E2E_ADMIN_PASSWORD!,
      })
      const introduction = page.getByRole('dialog', { name: 'Benvenuto in Diet Reminder' })
      if ((await session(page)).account.adminIntroductionPending) {
        await expect(introduction).toBeVisible()
        await expect(introduction.locator('.admin-introduction-point')).toHaveCount(3)
        await page.screenshot({ path: test.info().outputPath('admin-welcome.png') })
        await page.getByRole('button', { name: 'Ho capito, continua' }).click()
        await expect(introduction).toHaveCount(0)
      }
      await page.getByRole('link', { name: 'Amministrazione', exact: true }).click()
      await expect(page.getByRole('heading', { name: 'Tutto sotto controllo.' })).toBeVisible()
      await use(page)
    } finally {
      await context.close()
    }
  },
})
export { expect }

export function sampleSnapshot(): Snapshot {
  return {
    schemaVersion: 2,
    diets: [
      {
        id: 1,
        uuid: crypto.randomUUID(),
        name: 'Piano completo',
        isActive: true,
        nextMealWindowMinutes: 90,
        disabledNotificationDays: '',
      },
    ],
    meals: [
      {
        id: 2,
        dietId: 1,
        dayOfWeek: 1,
        type: 'LUNCH',
        timeMinutes: 780,
        description: 'Pranzo originale',
        customTypeLabel: null,
        isNotificationEnabled: true,
      },
    ],
    courses: [{ id: 3, mealId: 2, name: 'Primo', order: 0 }],
    foods: [{ id: 4, courseId: 3, name: 'Riso', amount: '80', unit: 'GRAMS', order: 0 }],
    lists: [
      {
        id: 5,
        name: 'Spesa collegata',
        createdAt: Date.now(),
        dietId: 1,
        remindersEnabled: false,
        reminderTimeMinutes: 1080,
      },
    ],
    items: [
      {
        id: 6,
        shoppingListId: 5,
        name: 'Insalata fresca',
        amount: '',
        unit: 'GRAMS',
        isBought: false,
        isCustom: false,
        isFresh: true,
        dayOfWeek: null,
      },
    ],
    days: [1, 2].map((day) => ({
      id: 6 + day,
      shoppingListItemId: 6,
      dayOfWeek: day,
      amount: '150',
      unit: 'GRAMS',
      isBought: false,
    })),
  }
}
export async function seed(page: Page, snapshot = sampleSnapshot()) {
  expect((await writeWorkspace(page, snapshot)).status()).toBe(200)
  await page.reload()
  await expect(page.getByRole('link', { name: 'La settimana', exact: true })).toBeVisible()
  return (await workspace(page.request)).snapshot
}

export function secondaryContextOptions() {
  const { viewport, isMobile, hasTouch } = test.info().project.use
  return { viewport, isMobile, hasTouch, locale: 'it-IT' }
}
