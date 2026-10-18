// @vitest-environment jsdom
import { readFileSync } from 'node:fs'
import { beforeEach, expect, it, vi } from 'vitest'
const source = readFileSync('public/theme.js', 'utf8')
beforeEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('style')
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn(() => ({ matches: false, addEventListener: vi.fn() })),
  })
  Object.defineProperty(window, 'CSS', { configurable: true, value: { supports: () => false } })
  window.eval(source)
})
it('applies theme, compact layout, custom palette and reduced motion immediately and persists choices', () => {
  const engine = window.DietReminderAppearance
  expect(
    engine.update({
      theme: 'light',
      compact: true,
      colorMode: 'custom',
      seed: '#6750a4',
      reducedMotion: true,
    }),
  ).toBe(true)
  expect(document.documentElement.dataset.theme).toBe('light')
  expect(document.documentElement.dataset.compact).toBe('true')
  expect(document.documentElement.dataset.motion).toBe('reduced')
  expect(engine.get().seed).toBe('#6750a4')
  expect(document.documentElement.style.getPropertyValue('--primary')).not.toBe('')
  window.eval(source)
  expect(window.DietReminderAppearance.get()).toMatchObject({
    theme: 'light',
    compact: true,
    reducedMotion: true,
    seed: '#6750a4',
  })
})
it('falls back safely when storage contains unsupported values', () => {
  localStorage.setItem('dr-theme', 'invalid')
  localStorage.setItem('dr-seed', 'javascript:bad')
  localStorage.setItem('dr-language', 'en')
  window.eval(source)
  expect(window.DietReminderAppearance.get().seed).toBe('#246a42')
  expect(document.documentElement.lang).toBe('en')
})
it('can restore the exact brand palette after customization', () => {
  const engine = window.DietReminderAppearance
  engine.update({ colorMode: 'custom', seed: '#6750a4' })
  engine.update({ colorMode: 'brand', seed: '#246a42' })
  expect(document.documentElement.style.getPropertyValue('--primary')).toBe('')
})
