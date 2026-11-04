// @vitest-environment jsdom
import { afterEach, expect, it } from 'vitest'
import { getLanguage, setLanguage, locale, t } from './i18n'
import { dayNames, mealNames } from './workspace'
afterEach(() => {
  setLanguage('it')
  localStorage.clear()
})
it('updates module labels, document, locale, persistence and interpolation without reload', () => {
  setLanguage('en')
  expect(document.documentElement.lang).toBe('en')
  expect(getLanguage()).toBe('en')
  expect(locale()).toBe('en-GB')
  expect(localStorage.getItem('dr-language')).toBe('en')
  expect(dayNames[0]).toBe('Monday')
  expect(mealNames.BREAKFAST).toBe('Breakfast')
  expect(t('Ingrediente {0}', { '0': 2 })).toBe('Ingredient 2')
  expect(t('An unknown custom label')).toBe('An unknown custom label')
  setLanguage('it')
  expect(dayNames[0]).toBe('Lunedì')
  expect(mealNames.BREAKFAST).toBe('Colazione')
})
it('propagates another tabs language and ignores invalid values', () => {
  window.dispatchEvent(new StorageEvent('storage', { key: 'dr-language', newValue: 'en' }))
  expect(getLanguage()).toBe('en')
  window.dispatchEvent(new StorageEvent('storage', { key: 'dr-language', newValue: 'invalid' }))
  expect(getLanguage()).toBe('en')
})
