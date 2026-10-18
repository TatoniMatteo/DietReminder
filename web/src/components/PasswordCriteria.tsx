import { hasUppercase, hasLowercase, hasSpecial } from '../lib/password'
import { t } from '../lib/i18n'
import { Circle, CircleCheck } from 'lucide-react'
export function PasswordCriteria({
  password,
  confirmation,
  currentPassword,
}: {
  password: string
  confirmation?: string
  currentPassword?: string
}) {
  const checks = [
    [t('Almeno 12 caratteri'), password.length >= 12],
    [t('Non più di 128 caratteri'), password.length > 0 && password.length <= 128],
    [t('Almeno una maiuscola'), hasUppercase(password)],
    [t('Almeno una minuscola'), hasLowercase(password)],
    [t('Almeno un carattere speciale'), hasSpecial(password)],
    ...(confirmation === undefined
      ? []
      : [[t('Le password coincidono'), password.length > 0 && password === confirmation] as const]),
    ...(currentPassword === undefined
      ? []
      : [
          [
            t('Diversa dalla password attuale'),
            password.length > 0 && password !== currentPassword,
          ] as const,
        ]),
  ] as const
  return (
    <ul className="password-criteria" aria-label={t('Criteri per la password')} aria-live="polite">
      {checks.map(([label, met]) => (
        <li key={label as string} data-met={met ? 'true' : 'false'}>
          {met ? <CircleCheck size={16} /> : <Circle size={16} />}
          <span>{label}</span>
        </li>
      ))}
    </ul>
  )
}
