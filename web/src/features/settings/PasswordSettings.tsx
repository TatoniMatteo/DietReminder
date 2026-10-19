import { isPasswordValid } from '../../lib/password'
import { t } from '../../lib/i18n'
import { useState, type FormEvent } from 'react'
import { KeyRound } from 'lucide-react'
import { api } from '../../lib/api'
import { ErrorNotice, Spinner } from '../../components/ui'
import { PasswordCriteria } from '../../components/PasswordCriteria'
export function PasswordSettings() {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [current, setCurrent] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [password, setPassword] = useState('')
  const valid =
    current.length > 0 &&
    isPasswordValid(password) &&
    password === confirmation &&
    password !== current
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!valid) return
    setBusy(true)
    setError('')
    try {
      await api('/api/auth/change-password', 'POST', {
        currentPassword: current,
        newPassword: password,
      })
      setCurrent('')
      setPassword('')
      setConfirmation('')
      window.dispatchEvent(
        new CustomEvent('session-expired', {
          detail: { notice: t('Password aggiornata. Accedi con la nuova password.') },
        }),
      )
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  function close() {
    if (!busy) {
      setOpen(false)
      setCurrent('')
      setPassword('')
      setConfirmation('')
      setError('')
    }
  }
  return (
    <section className="settings-section" id="settings-security">
      <h2>
        <KeyRound size={21} />
        {t('Password e sicurezza')}
      </h2>
      <p className="muted">{t('Per cambiare password, conferma quella attuale.')}</p>
      <button
        className="button secondary"
        aria-expanded={open}
        aria-controls="change-password-form"
        onClick={() => (open ? close() : setOpen(true))}
        disabled={busy}
      >
        {t('Cambia password')}
      </button>
      {open && (
        <div className="settings-password-form" id="change-password-form">
          <form
            onSubmit={(e) => {
              void submit(e)
            }}
          >
            {error && <ErrorNotice message={error} />}
            <label>
              {t('Password attuale')}
              <input
                type="password"
                value={current}
                onChange={(e) => setCurrent(e.target.value)}
                maxLength={128}
                required
                autoComplete="current-password"
              />
            </label>
            <label>
              {t('Nuova password')}
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={12}
                maxLength={128}
                required
                autoComplete="new-password"
              />
            </label>
            <label>
              {t('Conferma password')}
              <input
                type="password"
                value={confirmation}
                onChange={(e) => setConfirmation(e.target.value)}
                minLength={12}
                maxLength={128}
                required
                autoComplete="new-password"
              />
            </label>
            <PasswordCriteria
              password={password}
              confirmation={confirmation}
              currentPassword={current}
            />
            <p className="muted small">
              {t('Tutte le sessioni verranno chiuse. Accedi di nuovo con la nuova password.')}
            </p>
            <div className="modal-actions">
              <button type="button" className="button secondary" onClick={close} disabled={busy}>
                {t('Annulla')}
              </button>
              <button className="button primary" disabled={busy || !valid}>
                {busy ? <Spinner /> : t('Aggiorna password')}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
