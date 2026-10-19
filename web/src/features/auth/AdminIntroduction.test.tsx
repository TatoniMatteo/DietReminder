// @vitest-environment jsdom
import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { AdminIntroduction } from './AdminIntroduction'
const fetchMock = vi.fn<typeof fetch>()
let container: HTMLDivElement, root: ReturnType<typeof createRoot>
beforeEach(() => {
  Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value() {
      this.open = true
    },
  })
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value() {
      this.open = false
    },
  })
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
  container = document.createElement('div')
  document.body.append(container)
  root = createRoot(container)
})
afterEach(async () => {
  await act(async () => root.unmount())
  container.remove()
  vi.unstubAllGlobals()
})
it('explains account management and persists acknowledgement before refreshing the account', async () => {
  const refresh = vi.fn(async () => {})
  fetchMock.mockResolvedValue(new Response('{}'))
  await act(async () => root.render(<AdminIntroduction refresh={refresh} />))
  expect(container.textContent).toContain('puoi aiutare le altre persone')
  expect(container.textContent).toContain('Puoi cambiare questi permessi anche in seguito')
  expect(container.querySelectorAll('.admin-introduction-point svg')).toHaveLength(3)
  await act(async () =>
    [...container.querySelectorAll('button')]
      .find((x) => x.textContent === 'Ho capito, continua')!
      .click(),
  )
  expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/admin-introduction')
  expect(fetchMock.mock.calls[0][1]?.method).toBe('POST')
  expect(refresh).toHaveBeenCalledTimes(1)
})
it('keeps the introduction visible and offers a retry when acknowledgement fails', async () => {
  const refresh = vi.fn(async () => {})
  fetchMock.mockRejectedValueOnce(new TypeError('offline'))
  await act(async () => root.render(<AdminIntroduction refresh={refresh} />))
  const confirm = [...container.querySelectorAll('button')].find(
    (x) => x.textContent === 'Ho capito, continua',
  )!
  await act(async () => confirm.click())
  expect(container.querySelector('dialog')?.open).toBe(true)
  expect(container.querySelector('[role=alert]')).not.toBeNull()
  expect(refresh).not.toHaveBeenCalled()
  fetchMock.mockResolvedValueOnce(new Response('{}'))
  await act(async () => confirm.click())
  expect(refresh).toHaveBeenCalledTimes(1)
})
