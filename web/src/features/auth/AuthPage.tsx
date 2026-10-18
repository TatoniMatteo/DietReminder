import { isPasswordValid } from '../../lib/password'
import { t } from '../../lib/i18n'
import { useState, type FormEvent } from 'react'
import { ArrowRight, CalendarDays, ShoppingBasket, Utensils, Sparkles } from 'lucide-react'
import { api } from '../../lib/api'
import type { Providers, WebSession } from '../../lib/types'
import { GoogleButton } from './GoogleButton'
import { ErrorNotice, Spinner } from '../../components/ui'
import { PasswordCriteria } from '../../components/PasswordCriteria'
import { LanguageSelector } from '../../components/LanguageSelector'
export function AuthPage({
  providers,
  onSession,
  initialNotice = '',
}: {
  initialNotice?: string
  providers: Providers
  onSession: (session: WebSession) => void
}) {
  const [selectedMode, setMode] = useState<'login' | 'register' | 'forgot' | 'reset'>('login')
  const firstRegistration = providers.firstRegistrationRequired === true
  const mode = firstRegistration ? 'register' : selectedMode
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(initialNotice)
  const [busy, setBusy] = useState(false)
  const [draftPassword, setDraftPassword] = useState('')
  function changeMode(value: typeof mode) {
    setMode(value)
    setDraftPassword('')
  }
  async function signInGoogle(credential: string) {
    setBusy(true)
    setError('')
    try {
      onSession(await api<WebSession>('/api/web/auth/google', 'POST', { idToken: credential }))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    if (
      (mode === 'register' || mode === 'reset') &&
      !isPasswordValid(String(form.get('password') ?? ''))
    )
      return
    setBusy(true)
    setError('')
    setNotice('')
    const email = form.get('email') as string
    const password = form.get('password') as string
    try {
      if (mode === 'forgot') {
        await api('/api/auth/forgot-password', 'POST', { email })
        setNotice(t('Se l’account esiste, riceverai un codice per reimpostare la password.'))
        changeMode('reset')
      } else if (mode === 'reset') {
        await api('/api/auth/reset-password', 'POST', { token: form.get('token'), password })
        setNotice(t('Password aggiornata. Puoi accedere.'))
        changeMode('login')
      } else
        onSession(
          await api<WebSession>(`/api/web/auth/${mode}`, 'POST', {
            email,
            password,
            ...(mode === 'register' ? { username: form.get('username') } : {}),
          }),
        )
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="auth-page">
      <section className="auth-story">
        <div className="brand">
          <span className="brand-icon">
            <img className="brand-logo" src="/logo.svg" alt="" />
          </span>
          <span>
            Diet Reminder<small>{t('UN PO’ PIÙ DI EQUILIBRIO')}</small>
          </span>
        </div>
        <div className="auth-story-content">
          <span className="eyebrow">
            <Sparkles size={14} />
            {t('LA TUA ROUTINE, CON CURA')}
          </span>
          <h1>
            {t('Il giusto spazio.')}
            <br />
            <em>{t('Per stare bene.')}</em>
          </h1>
          <p>
            {t('Pianifica con calma, fai spazio alle tue abitudini.')}
            <br />
            {t('Il tuo percorso alimentare, sempre con te.')}
          </p>
          <div className="story-preview">
            <div className="story-date">
              <CalendarDays size={18} />
              {t('La tua settimana')}
              <span className="tag">{t('In equilibrio')}</span>
            </div>
            <div className="preview-meal">
              <span className="meal-icon breakfast">
                <Utensils size={19} />
              </span>
              <span>
                {t('Una buona colazione')}
                <small>{t('Il modo migliore per cominciare')}</small>
              </span>
              <strong>08:00</strong>
            </div>
            <div className="preview-meal">
              <span className="meal-icon lunch">
                <ShoppingBasket size={19} />
              </span>
              <span>
                {t('La spesa, già organizzata')}
                <small>{t('Solo quello che ti serve')}</small>
              </span>
              <ArrowRight size={18} />
            </div>
          </div>
          <div className="story-footer">
            <span className="dot" />
            {t('Android e web, sempre sincronizzati')}
          </div>
        </div>
        <span className="auth-copyright">{t('Meno pensieri. Più quotidianità.')}</span>
      </section>
      <section className="auth-form-panel">
        <div className="auth-form-wrap">
          <LanguageSelector />
          <span className="eyebrow">{t('IL TUO SPAZIO PERSONALE')}</span>
          <h2>
            {firstRegistration
              ? t('Sei il primo a registrarti!')
              : mode === 'register'
                ? t('Cominciamo da te.')
                : mode === 'forgot'
                  ? t('Ritrova il tuo accesso.')
                  : mode === 'reset'
                    ? t('Una nuova password.')
                    : t('Bentornato.')}
          </h2>
          <p className="muted">
            {firstRegistration
              ? t(
                  'Crea il primo account: diventerai l’amministratore di questa installazione. Potrai approvare gli utenti e assegnare i loro permessi.',
                )
              : mode === 'register'
                ? t('Crea il tuo account e porta le tue abitudini su ogni dispositivo.')
                : t('Accedi e riprendi il filo della tua settimana.')}
          </p>
          {!firstRegistration && (mode === 'login' || mode === 'register') && (
            <div className="segmented auth-tabs">
              <button
                className={mode === 'login' ? 'selected' : ''}
                onClick={() => {
                  changeMode('login')
                  setError('')
                }}
              >
                {t('Accedi')}
              </button>
              <button
                className={mode === 'register' ? 'selected' : ''}
                onClick={() => {
                  changeMode('register')
                  setError('')
                }}
              >
                {t('Crea account')}
              </button>
            </div>
          )}
          {error && <ErrorNotice message={error} />}{' '}
          {notice && (
            <p className="notice success" role="status">
              {notice}
            </p>
          )}
          <form
            onSubmit={(e) => {
              void submit(e)
            }}
            key={mode}
          >
            {mode === 'register' && (
              <label>
                {t('Come ti chiami?')}
                <input
                  name="username"
                  autoComplete="nickname"
                  required
                  maxLength={100}
                  placeholder={t('Il tuo nome')}
                />
              </label>
            )}
            {mode !== 'reset' && (
              <label>
                {t('Email')}
                <input
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  maxLength={254}
                  placeholder="nome@esempio.it"
                />
              </label>
            )}
            {mode === 'reset' && (
              <label>
                {t('Codice ricevuto via email')}
                <input name="token" required autoComplete="one-time-code" />
              </label>
            )}
            {mode !== 'forgot' && (
              <label>
                Password
                <input
                  name="password"
                  onChange={(e) => setDraftPassword(e.target.value)}
                  type="password"
                  required
                  minLength={mode === 'login' ? 1 : 12}
                  maxLength={128}
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  placeholder={mode === 'login' ? t('La tua password') : t('Almeno 12 caratteri')}
                />
              </label>
            )}
            {(mode === 'register' || mode === 'reset') && (
              <PasswordCriteria password={draftPassword} />
            )}
            {mode === 'login' && (
              <button
                type="button"
                className="text-button forgot-link"
                onClick={() => changeMode('forgot')}
              >
                {t('Password dimenticata?')}
              </button>
            )}
            <button
              className="button primary full"
              disabled={
                busy ||
                ((mode === 'register' || mode === 'reset') && !isPasswordValid(draftPassword))
              }
            >
              {busy ? (
                <Spinner />
              ) : (
                <>
                  {mode === 'register'
                    ? t('Crea il tuo spazio')
                    : mode === 'forgot'
                      ? t('Invia il codice')
                      : mode === 'reset'
                        ? t('Aggiorna password')
                        : t('Entra nel tuo spazio')}{' '}
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
          {mode === 'login' || mode === 'register' ? (
            <>
              <div className="divider">
                <span>{t('oppure')}</span>
              </div>
              <GoogleButton providers={providers} onCredential={signInGoogle} onError={setError} />
            </>
          ) : (
            <button className="text-button full" onClick={() => changeMode('login')}>
              {t('Torna all’accesso')}
            </button>
          )}
          <p className="auth-footnote">
            {t('I tuoi dati, nel tuo account.')}
            <br />
            {t('Pianificazione e spesa si aggiornano anche nell’app Android.')}
          </p>
        </div>
      </section>
    </div>
  )
}
