// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { PasswordSettings } from './PasswordSettings'
const fetchMock = vi.fn<typeof fetch>()
let root: Root, container: HTMLDivElement
beforeEach(async () => {
  Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
  container = document.createElement('div')
  document.body.append(container)
  root = createRoot(container)
  await act(async () => root.render(<PasswordSettings />))
})
afterEach(async () => {
  await act(async () => root.unmount())
  container.remove()
  vi.unstubAllGlobals()
})
const button = (label: string) =>
  [...container.querySelectorAll('button')].find((b) => b.textContent === label)!
async function input(index: number, value: string) {
  await act(async () => {
    const element = container.querySelectorAll('input')[index]
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(element, value)
    element.dispatchEvent(new Event('input', { bubbles: true }))
  })
}
it('expands in the page, validates all criteria and clears secrets on cancel', async () => {
  await act(async () => button('Cambia password').click())
  expect(container.querySelector('dialog')).toBeNull()
  expect(container.querySelectorAll('input')).toHaveLength(3)
  await input(0, 'Old password!')
  await input(1, 'Una password lunga!')
  await input(2, 'Una password lunga!')
  expect(button('Aggiorna password').disabled).toBe(false)
  expect(container.querySelectorAll('li[data-met=false]')).toHaveLength(0)
  await input(1, 'una password lunga!')
  await input(2, 'una password lunga!')
  expect(button('Aggiorna password').disabled).toBe(true)
  await act(async () => button('Annulla').click())
  await act(async () => button('Cambia password').click())
  expect([...container.querySelectorAll('input')].every((i) => i.value === '')).toBe(true)
})
it('keeps entered values for a rejected current password and closes sessions only after success', async () => {
  const expired = vi.fn()
  window.addEventListener('session-expired', expired)
  try {
    await act(async () => button('Cambia password').click())
    await input(0, 'Wrong password!')
    await input(1, 'Una password lunga!')
    await input(2, 'Una password lunga!')
    fetchMock.mockResolvedValueOnce(
      new Response('{"code":"INVALID_CURRENT_PASSWORD"}', { status: 400 }),
    )
    await act(async () =>
      container
        .querySelector('form')!
        .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })),
    )
    expect(container.querySelector('[role=alert]')?.textContent).toContain(
      'La password attuale non è corretta',
    )
    expect(container.querySelectorAll('input')[1].value).toBe('Una password lunga!')
    expect(expired).not.toHaveBeenCalled()
    await input(0, 'Old password!')
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))
    await act(async () =>
      container
        .querySelector('form')!
        .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })),
    )
    expect(expired).toHaveBeenCalledTimes(1)
    expect([...container.querySelectorAll('input')].every((i) => i.value === '')).toBe(true)
  } finally {
    window.removeEventListener('session-expired', expired)
  }
})
