import { createContext, useContext } from 'react'
import type { Account } from './types'
export const permissions = [
  'WORKSPACE_READ',
  'DIETS_WRITE',
  'SHOPPING_WRITE',
  'PROFILE_WRITE',
  'BACKUP_MANAGE',
] as const
export type Permission = (typeof permissions)[number]
export const AccountContext = createContext<Account | null>(null)
export function ready(account: Account) {
  return account.emailVerified !== false && (account.enrollmentStatus ?? 'APPROVED') === 'APPROVED'
}
export function permits(account: Account | null, permission: Permission) {
  return (
    !!account &&
    ready(account) &&
    (account.role === 'ADMIN' || (account.permissions ?? permissions).includes(permission))
  )
}
export function usePermission(permission: Permission) {
  return permits(useContext(AccountContext), permission)
}
