import { test, expect, createPlan, monday, seed, workspace } from './helpers/integration'
import type { MealType } from '../src/lib/types'

test('new accounts have empty plans, meals and shopping after reload', async ({
  signedIn: page,
}) => {
  await expect(page.getByText('Una settimana tutta da immaginare')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Aggiungi pasto', exact: true })).toBeDisabled()
  await page.getByRole('link', { name: 'I miei piani' }).click()
  await expect(page.getByText('Ogni abitudine inizia da qui')).toBeVisible()
  await page.getByRole('link', { name: 'La spesa', exact: true }).click()
  await expect(page.getByText('La prossima spesa comincia qui')).toBeVisible()
  await page.reload()
  const saved = await workspace(page.request)
  expect(saved.snapshot.diets).toEqual([])
  expect(saved.snapshot.meals).toEqual([])
  expect(saved.snapshot.lists).toEqual([])
})

test('plans can be renamed, switched, and deletion can be cancelled', async ({
  signedIn: page,
}) => {
  await createPlan(page, 'Primo piano')
  await createPlan(page, 'Secondo piano')
  const card = page
    .getByRole('article')
    .filter({ has: page.getByRole('heading', { name: 'Secondo piano' }) })
  await card.getByRole('button', { name: 'Usa questo piano' }).click()
  await expect(card.getByRole('button', { name: 'In uso' })).toBeDisabled()
  await page.getByRole('button', { name: 'Modifica Secondo piano', exact: true }).click()
  await page.getByLabel('Nome del piano').fill('Piano rinominato')
  await page.getByRole('button', { name: 'Salva piano' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.getByRole('button', { name: 'Rimuovi Piano rinominato', exact: true }).click()
  await page.getByRole('button', { name: 'Annulla', exact: true }).click()
  await page.reload()
  const saved = (await workspace(page.request)).snapshot
  expect(saved.diets.map((d) => [d.name, d.isActive])).toEqual([
    ['Primo piano', false],
    ['Piano rinominato', true],
  ])
  await monday(page)
  await expect(page.getByLabel('Piano alimentare')).toHaveValue(
    String(saved.diets.find((d) => d.isActive)!.id),
  )
})

for (const [type, title] of [
  ['BREAKFAST', 'Colazione'],
  ['MORNING_SNACK', 'Spuntino mattutino'],
  ['LUNCH', 'Pranzo'],
  ['AFTERNOON_SNACK', 'Merenda'],
  ['DINNER', 'Cena'],
  ['OTHER', 'Pausa speciale'],
] as [MealType, string][]) {
  test(`meal ${type} persists time, notes, quantities and notification preferences`, async ({
    signedIn: page,
  }) => {
    await createPlan(page)
    await monday(page)
    await page.getByRole('button', { name: 'Aggiungi pasto', exact: true }).click()
    await page.getByLabel('Pasto', { exact: true }).selectOption(type)
    if (type === 'OTHER') await page.getByLabel('Nome del pasto', { exact: true }).fill(title)
    await page.getByLabel('Orario', { exact: true }).fill('09:45')
    await page.getByLabel('Una nota per te').fill('Nota persistente')
    await page.getByLabel('Promemoria per questo pasto').uncheck()
    await page.getByRole('button', { name: 'Aggiungi', exact: true }).click()
    await page.getByLabel('Ingrediente 1', { exact: true }).fill('Latte')
    await page.getByLabel('Quantità 1', { exact: true }).fill('125.5')
    await page.getByLabel('Unità 1', { exact: true }).selectOption('MILLILITERS')
    await page.getByRole('button', { name: 'Salva pasto', exact: true }).click()
    await expect(page.getByRole('region', { name: 'Editor pasto' })).toHaveCount(0)
    await page.reload()
    await monday(page)
    await page.locator('summary').filter({ hasText: title }).click()
    await expect(page.getByText('Latte', { exact: true })).toBeVisible()
    await expect(page.getByText('125.5 ml', { exact: true })).toBeVisible()
    const saved = (await workspace(page.request)).snapshot
    expect(saved.meals[0]).toMatchObject({
      type,
      dayOfWeek: 1,
      timeMinutes: 585,
      description: 'Nota persistente',
      isNotificationEnabled: false,
      customTypeLabel: type === 'OTHER' ? title : null,
    })
    expect(saved.foods[0]).toMatchObject({ name: 'Latte', amount: '125.5', unit: 'MILLILITERS' })
  })
}

test('meal edits remove a course and its ingredients without leaving orphan records', async ({
  signedIn: page,
}) => {
  await seed(page)
  await monday(page)
  await page.locator('summary').click()
  await page.getByRole('button', { name: 'Modifica', exact: true }).click()
  await page.getByRole('button', { name: 'Aggiungi portata', exact: true }).click()
  await page.getByLabel('Nome della portata 2', { exact: true }).fill('Contorno')
  await page
    .getByRole('button', { name: 'Aggiungi ingrediente alla portata 2', exact: true })
    .click()
  await page.getByLabel('Ingrediente 2', { exact: true }).fill('Carote')
  await page.getByRole('button', { name: 'Rimuovi portata 1', exact: true }).click()
  await page.getByLabel('Una nota per te').fill('Pranzo modificato')
  await page.getByRole('button', { name: 'Salva pasto' }).click()
  await expect(page.getByRole('region', { name: 'Editor pasto' })).toHaveCount(0)
  await page.reload()
  const saved = (await workspace(page.request)).snapshot
  expect(saved.meals[0].description).toBe('Pranzo modificato')
  expect(saved.courses).toHaveLength(1)
  expect(saved.courses[0]).toMatchObject({ name: 'Contorno', order: 0 })
  expect(saved.foods).toHaveLength(1)
  expect(saved.foods[0]).toMatchObject({ name: 'Carote', courseId: saved.courses[0].id })
})

test('deleting a meal cascades courses and foods but preserves shopping', async ({
  signedIn: page,
}) => {
  const original = await seed(page)
  await monday(page)
  await page.locator('summary').click()
  await page.getByRole('button', { name: 'Rimuovi', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Rimuovi pasto', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.reload()
  const saved = (await workspace(page.request)).snapshot
  expect(saved.meals).toEqual([])
  expect(saved.courses).toEqual([])
  expect(saved.foods).toEqual([])
  expect(saved.items).toEqual(original.items)
  expect(saved.days).toEqual(original.days)
})

test('deleting the active plan activates the remaining plan and unlinks its shopping list', async ({
  signedIn: page,
}) => {
  const original = await seed(page)
  await createPlan(page, 'Piano alternativo')
  await page.getByRole('button', { name: 'Rimuovi Piano completo', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Rimuovi piano', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.reload()
  const saved = (await workspace(page.request)).snapshot
  expect(saved.diets).toHaveLength(1)
  expect(saved.diets[0]).toMatchObject({ name: 'Piano alternativo', isActive: true })
  expect(saved.meals).toEqual([])
  expect(saved.courses).toEqual([])
  expect(saved.foods).toEqual([])
  expect(saved.lists[0].dietId).toBeNull()
  expect(saved.items).toEqual(original.items)
})
