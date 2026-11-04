import { expect, it } from 'vitest'
import { permits, ready } from './access'
import type { Account } from './types'
const user: Account = {
  userId: '1',
  email: 'user@example.com',
  username: 'User',
  role: 'USER',
  googleLinked: false,
  emailVerified: true,
  enrollmentStatus: 'APPROVED',
  permissions: ['WORKSPACE_READ'],
}
it('requires verified email and approval even for administrators', () => {
  expect(ready({ ...user, emailVerified: false })).toBe(false)
  for (const enrollmentStatus of ['PENDING_EMAIL', 'PENDING_APPROVAL', 'REJECTED'] as const)
    expect(permits({ ...user, role: 'ADMIN', enrollmentStatus }, 'DIETS_WRITE')).toBe(false)
})
it('respects user grants and keeps active administrator privileges', () => {
  expect(permits(user, 'WORKSPACE_READ')).toBe(true)
  expect(permits(user, 'DIETS_WRITE')).toBe(false)
  expect(permits({ ...user, role: 'ADMIN' }, 'DIETS_WRITE')).toBe(true)
  expect(permits(null, 'WORKSPACE_READ')).toBe(false)
})
