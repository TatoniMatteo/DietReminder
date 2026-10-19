export const hasUppercase = (value: string) => /\p{Lu}/u.test(value)
export const hasLowercase = (value: string) => /\p{Ll}/u.test(value)
export const hasSpecial = (value: string) => /[^\p{L}\p{N}\s]/u.test(value)
export const isPasswordValid = (value: string) =>
  value.length >= 12 &&
  value.length <= 128 &&
  hasUppercase(value) &&
  hasLowercase(value) &&
  hasSpecial(value)
