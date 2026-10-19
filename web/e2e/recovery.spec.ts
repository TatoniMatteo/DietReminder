import { completeEnrollment } from './helpers/enrollment'
import { test, expect } from '@playwright/test'
import { writeFile } from 'node:fs/promises'

test('password recovery reaches the local SMTP inbox and the code is single use', async ({
  page,
  request,
}, info) => {
  const inbox = process.env.MAILPIT_TEST_URL ?? 'http://127.0.0.1:8025'
  expect(['127.0.0.1', 'localhost']).toContain(new URL(inbox).hostname)
  const email = `browser-e2e-${crypto.randomUUID()}@dietreminder.local`
  const oldPassword = 'Browser-e2e-password-2026'
  const newPassword = 'Una password recuperata!'
  let messageId = ''
  await page.goto('/')
  await page.getByRole('button', { name: 'Crea account', exact: true }).click()
  await page.getByLabel('Come ti chiami?').fill('Recovery E2E')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(oldPassword)
  await page.getByRole('button', { name: 'Crea il tuo spazio' }).click()
  await completeEnrollment(page, email)
  await expect(page.getByRole('link', { name: 'Impostazioni', exact: true })).toBeVisible()
  const me = await (await page.request.get('/api/web/auth/me')).json()
  await writeFile(
    info.outputPath('created-user.json'),
    JSON.stringify({ id: me.account.userId, email }),
  )
  await page.getByRole('button', { name: 'Esci dall’account' }).click()
  await page.getByRole('button', { name: 'Password dimenticata?' }).click()
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByRole('button', { name: 'Invia il codice' }).click()
  await expect(page.getByRole('heading', { name: 'Una nuova password.' })).toBeVisible()
  try {
    await expect
      .poll(async () => {
        const data = await (
          await request.get(`${inbox}/api/v1/search`, { params: { query: `to:${email}` } })
        ).json()
        const message = data.messages?.find(
          (m: { Subject: string; To: { Address: string }[] }) =>
            m.Subject.includes('Password reset') && m.To.some((to) => to.Address === email),
        )
        messageId = message?.ID ?? ''
        return Boolean(messageId)
      })
      .toBe(true)
    const message = await (await request.get(`${inbox}/api/v1/message/${messageId}`)).json()
    expect(message.To.some((to: { Address: string }) => to.Address === email)).toBe(true)
    const code = (message.Text as string).match(/[A-Za-z0-9_-]{43}/)?.[0]
    expect(code).toBeTruthy()
    expect(message.HTML).toContain('CODICE PERSONALE')
    expect(message.HTML).toContain('cid:dietreminder-logo')
    expect(message.HTML).toContain('30 minuti')
    expect(message.HTML).toContain(code!)
    await expect(page.locator('.password-criteria li[data-met=true]')).toHaveCount(0)
    await page.getByLabel('Codice ricevuto via email').fill(code!)
    await page.getByLabel('Password', { exact: true }).fill(newPassword)
    await page.getByRole('button', { name: 'Aggiorna password' }).click()
    await expect(page.getByRole('heading', { name: 'Bentornato.' })).toBeVisible()
    const reuse = await request.post('/api/auth/reset-password', {
      data: { token: code, password: newPassword },
      headers: { 'X-DietReminder-Web': '1' },
    })
    expect(reuse.status()).toBe(400)
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Password', { exact: true }).fill(oldPassword)
    await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
    await expect(page.getByRole('alert')).toContainText('Email o password non corrette')
    await page.getByLabel('Password', { exact: true }).fill(newPassword)
    await page.getByRole('button', { name: 'Entra nel tuo spazio' }).click()
    await expect(page.getByRole('link', { name: 'Impostazioni', exact: true })).toBeVisible()
  } finally {
    if (messageId) await request.delete(`${inbox}/api/v1/messages`, { data: { IDs: [messageId] } })
  }
})
