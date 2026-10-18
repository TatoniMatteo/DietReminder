import { test, expect, createList, addItem, seed, workspace } from './helpers/integration'

test('custom shopping items keep separate usage and purchase days, category and quantities', async ({
  signedIn: page,
}) => {
  await createList(page)
  await addItem(page, 'Mele', '2', '1')
  await page.getByRole('button', { name: 'Lunedì', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Acquistato: Mele', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Martedì', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Acquistato: Mele', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Tutti', exact: true }).click()
  await page.getByRole('button', { name: 'Organizza Mele', exact: true }).click()
  await page.getByLabel('Nome alimento', { exact: true }).fill('Pere')
  await page.getByLabel('Quantità', { exact: true }).fill('3')
  await page.getByLabel('Categoria', { exact: true }).fill('Mercato')
  await page.getByRole('button', { name: 'Salva alimento' }).click()
  await expect(page.getByRole('region', { name: 'Organizza alimento' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Acquistato: Pere', exact: true }).click()
  await expect(
    page.getByRole('button', { name: 'Da acquistare: Pere', exact: true }),
  ).toHaveAttribute('aria-pressed', 'true')
  await page.reload()
  expect((await workspace(page.request)).snapshot.items[0]).toMatchObject({
    name: 'Pere',
    amount: '3',
    unit: 'PIECES',
    category: 'Mercato',
    dayOfWeek: 2,
    purchaseDayOfWeek: 1,
    isBought: true,
  })
  await expect(page.getByRole('heading', { name: 'Mercato', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Da acquistare: Pere', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Acquistato: Pere', exact: true })).toHaveAttribute(
    'aria-pressed',
    'false',
  )
  await page.getByRole('button', { name: 'Rimuovi Pere', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Organizza Pere', exact: true })).toHaveCount(0)
  expect((await workspace(page.request)).snapshot.items).toEqual([])
})

test('fresh portions are purchased independently and removing the item removes all daily allocations', async ({
  signedIn: page,
}) => {
  const original = await seed(page)
  await page.getByRole('link', { name: 'La spesa', exact: true }).click()
  await page
    .getByRole('button', { name: 'Acquistato: Insalata fresca, Lunedì', exact: true })
    .click()
  await expect(
    page.getByRole('button', { name: 'Da acquistare: Insalata fresca, Lunedì', exact: true }),
  ).toHaveAttribute('aria-pressed', 'true')
  let saved = (await workspace(page.request)).snapshot
  expect(saved.items[0].isBought).toBe(false)
  expect(saved.days.map((d) => d.isBought)).toEqual([true, false])
  await page
    .getByRole('button', { name: 'Acquistato: Insalata fresca, Martedì', exact: true })
    .click()
  await expect(
    page.getByRole('button', { name: 'Da acquistare: Insalata fresca, Martedì', exact: true }),
  ).toHaveAttribute('aria-pressed', 'true')
  saved = (await workspace(page.request)).snapshot
  expect(saved.items[0].isBought).toBe(true)
  expect(saved.days.every((d) => d.isBought)).toBe(true)
  await page.reload()
  await page.getByRole('button', { name: 'Rimuovi Insalata fresca', exact: true }).first().click()
  await expect(page.getByRole('button', { name: /Organizza Insalata fresca/ })).toHaveCount(0)
  saved = (await workspace(page.request)).snapshot
  expect(saved.items).toEqual([])
  expect(saved.days).toEqual([])
  expect(saved.meals).toEqual(original.meals)
})

test('lists are isolated; deleting one cascades its items but preserves the other list and plan', async ({
  signedIn: page,
}) => {
  const original = await seed(page)
  await createList(page, 'Dispensa', 'Piano completo')
  await addItem(page, 'Sale')
  await page.getByRole('button', { name: /Spesa collegata.*da acquistare/ }).click()
  await expect(page.getByRole('button', { name: 'Acquistato: Sale', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Rimuovi lista', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Annulla', exact: true }).click()
  expect((await workspace(page.request)).snapshot.lists).toHaveLength(2)
  await page.getByRole('button', { name: 'Rimuovi lista', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Rimuovi lista', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Dispensa', exact: true })).toBeVisible()
  const saved = (await workspace(page.request)).snapshot
  expect(saved.lists).toHaveLength(1)
  expect(saved.lists[0]).toMatchObject({ name: 'Dispensa', dietId: 1 })
  expect(saved.items.map((i) => i.name)).toEqual(['Sale'])
  expect(saved.days).toEqual([])
  expect(saved.diets).toEqual(original.diets)
})

test('shopping reminder enable, time and disable persist across reloads', async ({
  signedIn: page,
}) => {
  await createList(page)
  for (const enabled of [true, false]) {
    await page.getByRole('button', { name: 'Promemoria spesa', exact: true }).click()
    await page.getByLabel('Ricordami gli alimenti per domani').setChecked(enabled)
    await page.getByLabel('Orario indicativo').fill('19:15')
    await page.getByRole('button', { name: 'Salva promemoria', exact: true }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await page.reload()
    expect((await workspace(page.request)).snapshot.lists[0]).toMatchObject({
      remindersEnabled: enabled,
      reminderTimeMinutes: 1155,
    })
  }
})
