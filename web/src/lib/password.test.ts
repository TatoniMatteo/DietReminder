import { describe, expect, it } from 'vitest'
import { isPasswordValid } from './password'
describe('new password policy', () => {
  it.each(['Una password lunga!', 'Àccento sicuro!'])(
    'accepts %s without requiring a digit',
    (value) => expect(isPasswordValid(value)).toBe(true),
  )
  it.each([
    'una password lunga!',
    'UNA PASSWORD LUNGA!',
    'Una password lunga',
    'Breve!',
    'A!' + 'a'.repeat(127),
  ])('rejects missing criteria or invalid length: %s', (value) =>
    expect(isPasswordValid(value)).toBe(false),
  )
})
