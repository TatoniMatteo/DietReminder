import { test, expect } from './helpers/integration'

test('expanded dropdowns follow light and dark palettes and appearance labels have breathing room', async ({
  signedIn: page,
}) => {
  await page.getByRole('link', { name: 'Impostazioni', exact: true }).click()
  const colors: string[] = []
  for (const theme of ['Chiaro', 'Scuro']) {
    await page.getByRole('button', { name: theme, exact: true }).click()
    const control = page.getByRole('combobox', { name: 'Lingua', exact: true })
    await control.click()
    const menu = page.getByRole('listbox', { name: 'Lingua', exact: true })
    await expect(menu).toBeVisible()
    await expect(menu.getByRole('option', { name: 'Italiano', exact: true })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    colors.push(await menu.evaluate((element) => getComputedStyle(element).backgroundColor))
    const bounds = await menu.boundingBox()
    const viewport = page.viewportSize()!
    expect(bounds!.x).toBeGreaterThanOrEqual(0)
    expect(bounds!.y).toBeGreaterThanOrEqual(0)
    expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(viewport.width)
    expect(bounds!.y + bounds!.height).toBeLessThanOrEqual(viewport.height)
    await page.screenshot({ path: test.info().outputPath(`dropdown-${theme}.png`) })
    await control.press('Escape')
    await expect(menu).toHaveCount(0)
    await expect(control).toBeFocused()
  }
  expect(colors[0]).not.toBe(colors[1])
  await page.getByRole('switch', { name: 'Interfaccia compatta' }).check()
  await page.getByRole('button', { name: 'Colore Lavanda', exact: true }).click()
  await page.getByRole('combobox', { name: 'Lingua', exact: true }).click()
  await expect(page.getByRole('listbox')).toBeVisible()
  await page.getByRole('heading', { name: 'Il tuo spazio.' }).click()
  await expect(page.getByRole('listbox')).toHaveCount(0)
  const gap = await page.locator('.preference-label').evaluate((element) => {
    const previous = element.previousElementSibling!.getBoundingClientRect()
    return element.getBoundingClientRect().top - previous.bottom
  })
  expect(gap).toBeGreaterThanOrEqual(20)
})

test('dropdown keyboard navigation commits choices, Escape cancels and Tab closes the menu', async ({
  signedIn: page,
}) => {
  await page.getByRole('link', { name: 'Impostazioni', exact: true }).click()
  const control = page.getByRole('combobox', { name: 'Lingua', exact: true })
  await control.focus()
  await control.press('ArrowDown')
  await control.press('End')
  await control.press('Escape')
  await expect(control).toHaveText('Italiano')
  await control.press('ArrowDown')
  await control.press('End')
  await control.press('Enter')
  const english = page.getByRole('combobox', { name: 'Language', exact: true })
  await expect(english).toHaveText('English')
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await english.press('ArrowDown')
  await english.press('Tab')
  await expect(page.getByRole('listbox')).toHaveCount(0)
  await english.focus()
  await english.press('i')
  await english.press('Enter')
  await expect(page.locator('html')).toHaveAttribute('lang', 'it')
  await page.reload()
  await expect(page.getByRole('combobox', { name: 'Lingua', exact: true })).toHaveText('Italiano')
})
