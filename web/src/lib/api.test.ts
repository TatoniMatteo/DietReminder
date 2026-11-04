// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, setCsrf } from './api'
const fetchMock = vi.fn<typeof fetch>()
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  setCsrf('')
  fetchMock.mockReset()
})
afterEach(() => vi.unstubAllGlobals())
describe('HTTP and session boundaries', () => {
  it('sends same-origin cookies, CSRF and structured JSON without a bearer in storage', async () => {
    fetchMock.mockResolvedValue(new Response('{"saved":true}'))
    setCsrf('test-csrf')
    expect(await api('/api/workspace', 'PUT', { version: 1 })).toEqual({ saved: true })
    const [, options] = fetchMock.mock.calls[0]
    expect(options?.credentials).toBe('same-origin')
    expect(options?.headers).toMatchObject({
      'X-CSRF-Token': 'test-csrf',
      'X-DietReminder-Web': '1',
    })
    expect(options?.body).toBe('{"version":1}')
    expect(localStorage.length).toBe(0)
  })
  it('handles an empty success response', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    expect(await api('/api/auth/change-password', 'POST')).toBeUndefined()
  })
  it('revokes a protected session on 401 but does not revoke for failed login', async () => {
    const expired = vi.fn()
    window.addEventListener('session-expired', expired)
    fetchMock.mockImplementation(
      async () => new Response('{"code":"UNAUTHENTICATED"}', { status: 401 }),
    )
    await expect(api('/api/workspace')).rejects.toBeInstanceOf(ApiError)
    expect(expired).toHaveBeenCalledTimes(1)
    await expect(api('/api/web/auth/login', 'POST')).rejects.toBeInstanceOf(ApiError)
    expect(expired).toHaveBeenCalledTimes(1)
    window.removeEventListener('session-expired', expired)
  })
  it('keeps a session for wrong current password and exposes its localized error', async () => {
    fetchMock.mockResolvedValue(
      new Response('{"code":"INVALID_CURRENT_PASSWORD"}', { status: 400 }),
    )
    await expect(api('/api/auth/change-password', 'POST')).rejects.toMatchObject({
      status: 400,
      code: 'INVALID_CURRENT_PASSWORD',
      message: 'La password attuale non è corretta.',
    })
  })
  it('handles unavailable networks and non-JSON server errors', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('offline'))
    await expect(api('/api/workspace')).rejects.toThrow('Il server non è raggiungibile')
    fetchMock.mockResolvedValueOnce(new Response('bad gateway', { status: 502 }))
    await expect(api('/api/workspace')).rejects.toMatchObject({ code: 'SERVER_ERROR', status: 502 })
    fetchMock.mockResolvedValueOnce(new Response('{}', { status: 403 }))
    await expect(api('/api/admin/users')).rejects.toThrow('Non hai i permessi')
  })
})
