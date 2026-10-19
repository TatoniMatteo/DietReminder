import { expect, type Locator } from '@playwright/test'

export async function selectOption(control: Locator, value: string | { label: string }) {
  const label =
    typeof value === 'string'
      ? await control.evaluate((element, wanted) => {
          const options = element.parentElement!.querySelector('select')!.options
          return Array.from(options).find((option) => option.value === wanted)?.textContent ?? null
        }, value)
      : value.label
  expect(label).not.toBeNull()
  await control.click()
  await control
    .page()
    .getByRole('listbox')
    .getByRole('option', { name: label!, exact: true })
    .click()
  await expect(control.page().getByRole('listbox')).toHaveCount(0)
}
