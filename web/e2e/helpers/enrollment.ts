import { expect, type APIRequestContext, type Page } from '@playwright/test'
export const allPermissions = [
  'WORKSPACE_READ',
  'DIETS_WRITE',
  'SHOPPING_WRITE',
  'PROFILE_WRITE',
  'BACKUP_MANAGE',
]
export async function emailCode(request: APIRequestContext, email: string) {
  const inbox = process.env.MAILPIT_TEST_URL ?? 'http://127.0.0.1:8025'
  expect(['127.0.0.1', 'localhost']).toContain(new URL(inbox).hostname)
  let messageId = ''
  await expect
    .poll(
      async () => {
        const result = await (
          await request.get(`${inbox}/api/v1/search`, { params: { query: `to:${email}` } })
        ).json()
        messageId =
          result.messages?.find(
            (m: { Subject: string; To: { Address: string }[] }) =>
              m.Subject.includes('Verifica email') && m.To.some((x) => x.Address === email),
          )?.ID ?? ''
        return !!messageId
      },
      { timeout: 15000 },
    )
    .toBe(true)
  const message = await (await request.get(`${inbox}/api/v1/message/${messageId}`)).json()
  const code = (message.Text as string).match(/[A-Za-z0-9_-]{43}/)?.[0]
  expect(code).toBeTruthy()
  expect(message.HTML).toContain('CODICE PERSONALE')
  expect(message.HTML).toContain('cid:dietreminder-logo')
  expect(message.HTML).toContain(code!)
  return code!
}
export async function administratorToken(request: APIRequestContext) {
  const response = await request.post('/api/auth/login', {
    headers: { Cookie: '' },
    data: { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD },
  })
  expect(response.ok()).toBe(true)
  const session = await response.json()
  expect(session.account.role).toBe('ADMIN')
  return session.token as string
}
export async function verifyFromInbox(page: Page, email: string) {
  const code = await emailCode(page.request, email)
  await page.getByLabel(/^(Codice di verifica|Verification code)$/).fill(code)
  await page.getByRole('button', { name: /^(Conferma email|Verify email)$/ }).click()
  await expect(
    page.getByRole('heading', { name: /^(In attesa di approvazione|Awaiting approval)$/ }),
  ).toBeVisible()
}
export async function completeEnrollment(page: Page, email: string) {
  await verifyFromInbox(page, email)
  const { account } = await (await page.request.get('/api/web/auth/me')).json()
  const token = await administratorToken(page.request)
  const approval = await page.request.post(`/api/admin/users/${account.userId}/review`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { version: account.version, approved: true, permissions: allPermissions },
  })
  expect(approval.ok()).toBe(true)
  await page.getByRole('button', { name: /^(Controlla stato|Check status)$/ }).click()
  await expect(page.getByRole('link', { name: /^(Impostazioni|Settings)$/ })).toBeVisible()
}
