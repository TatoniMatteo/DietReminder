import { useEffect, useState, type FormEvent } from 'react'
import { MailCheck, Clock3, CircleX } from 'lucide-react'
import { t } from '../../lib/i18n'
import { api } from '../../lib/api'
import type { Account, Enrollment } from '../../lib/types'
import { ErrorNotice, Spinner } from '../../components/ui'
import { LanguageSelector } from '../../components/LanguageSelector'
export const enrollmentLabels: Record<string, string> = {
  PENDING_EMAIL: 'Verifica la tua email',
  PENDING_APPROVAL: 'In attesa di approvazione',
  APPROVED: 'Richiesta approvata',
  REJECTED: 'Richiesta rifiutata',
}
export function EnrollmentPage({
  account,
  refresh,
  logout,
}: {
  account: Account
  refresh: () => Promise<void>
  logout: () => Promise<void>
}) {
  const [history, setHistory] = useState<Enrollment['history']>([])
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const value = await api<Enrollment>('/api/auth/enrollment')
        if (active) setHistory(value.history)
        await refresh()
      } catch (e) {
        if (active) setError((e as Error).message)
      }
    }
    void load()
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') void load()
    }, 15000)
    return () => {
      active = false
      clearInterval(timer)
    }
  }, [account.userId])
  async function verify(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api('/api/auth/verify-email', 'POST', { token: code.trim() })
      await refresh()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function resend() {
    setBusy(true)
    setError('')
    try {
      await api('/api/auth/resend-verification', 'POST')
      setNotice(t('Ti abbiamo inviato un nuovo codice.'))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  const status = account.enrollmentStatus ?? 'PENDING_EMAIL'
  const Icon = status === 'PENDING_EMAIL' ? MailCheck : status === 'REJECTED' ? CircleX : Clock3
  return (
    <main className="auth-page enrollment-page">
      <section className="auth-form-panel">
        <div className="auth-form-wrap enrollment-card">
          <LanguageSelector />
          <Icon size={36} aria-hidden="true" />
          <h1>{t(enrollmentLabels[status])}</h1>
          <p>{account.email}</p>
          <ol className="enrollment-steps">
            <li>{t('Conferma email')}</li>
            <li>{t('Valutazione amministrativa')}</li>
            <li>{t('Accesso all’app')}</li>
          </ol>
          {status === 'PENDING_EMAIL' && (
            <>
              <p>{t('Inserisci il codice ricevuto via email. È valido per 24 ore.')}</p>
              {account.role === 'ADMIN' && (
                <p>{t('Sei il primo utente: dopo la verifica potrai amministrare l’app.')}</p>
              )}
              <form
                onSubmit={(e) => {
                  void verify(e)
                }}
              >
                <label>
                  {t('Codice di verifica')}
                  <input
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    autoComplete="one-time-code"
                    maxLength={43}
                    required
                  />
                </label>
                <button className="button primary full" disabled={busy || !code.trim()}>
                  {busy ? <Spinner /> : t('Conferma email')}
                </button>
              </form>
              <button
                className="text-button"
                disabled={busy}
                onClick={() => {
                  void resend()
                }}
              >
                {t('Invia un nuovo codice')}
              </button>
            </>
          )}
          {status === 'PENDING_APPROVAL' && (
            <p>
              {t(
                'La tua email è confermata. Un amministratore valuterà la richiesta e assegnerà i permessi. Riceverai una notifica via email.',
              )}
            </p>
          )}
          {status === 'REJECTED' && (
            <p role="status">
              {account.reviewReason ||
                t(
                  'La richiesta non è stata approvata. Contatta l’amministratore per informazioni.',
                )}
            </p>
          )}
          {error && <ErrorNotice message={error} />}
          {notice && <p role="status">{notice}</p>}
          {history.length > 0 && (
            <section aria-label={t('Stato della richiesta')}>
              <h2>{t('Stato della richiesta')}</h2>
              <ul>
                {history.map((event) => (
                  <li key={event.id}>
                    <strong>{t(enrollmentLabels[event.status])}</strong>
                    <p>{event.message}</p>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <button
            className="button secondary"
            disabled={busy}
            onClick={() => {
              void refresh().catch((e) => setError((e as Error).message))
            }}
          >
            {t('Controlla stato')}
          </button>
          <button
            className="text-button"
            onClick={() => {
              void logout().catch((e) => setError((e as Error).message))
            }}
          >
            {t('Esci dall’account')}
          </button>
        </div>
      </section>
    </main>
  )
}
