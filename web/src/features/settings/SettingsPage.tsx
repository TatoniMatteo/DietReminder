import { permits } from '../../lib/access'
import { t } from '../../lib/i18n'
import { useEffect, useState, type FormEvent } from 'react'
import { UserRound, ShieldCheck, ShoppingBasket } from 'lucide-react'
import { api } from '../../lib/api'
import type { Account, Profile, Providers } from '../../lib/types'
import { GoogleButton } from '../auth/GoogleButton'
import { ErrorNotice, Spinner } from '../../components/ui'
import { DriveBackups } from './DriveBackups'
import { AppearanceSettings } from './AppearanceSettings'
import { PasswordSettings } from './PasswordSettings'
export function SettingsPage({
  account,
  providers,
  refreshAccount,
}: {
  account: Account
  providers: Providers
  refreshAccount: () => Promise<void>
}) {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    void api<Profile>('/api/user/profile')
      .then(setProfile)
      .catch((e) => setError((e as Error).message))
  }, [])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!permits(account, 'PROFILE_WRITE')) {
      setError(t('I tuoi permessi non consentono questa operazione.'))
      return
    }
    const form = new FormData(event.currentTarget)
    setBusy(true)
    setError('')
    try {
      setProfile(
        await api<Profile>('/api/user/profile', 'POST', {
          username: form.get('username'),
        }),
      )
      setNotice(t('Profilo aggiornato.'))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function link(credential: string) {
    setError('')
    setBusy(true)
    try {
      await api('/api/auth/google/link', 'POST', { idToken: credential })
      await refreshAccount()
      setNotice(t('Google collegato. Ora puoi usarlo per accedere su ogni dispositivo.'))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">{t('FALLO TUO')}</span>
          <h1>
            {t('Il tuo spazio')}
            <span className="heading-dot">.</span>
          </h1>
          <p className="muted">{t('I dettagli che fanno la differenza, per te.')}</p>
        </div>
      </div>
      {error && <ErrorNotice message={error} />}{' '}
      {notice && (
        <p role="status" className="notice success">
          {notice}
        </p>
      )}
      <div className="settings-layout">
        <aside className="settings-summary" aria-label={t('Sommario impostazioni')}>
          <span className="eyebrow">{t('IMPOSTAZIONI')}</span>
          {(
            [
              ['profile', t('Profilo')],
              ['appearance', t('Aspetto')],
              ['security', t('Password e sicurezza')],
              ['shopping', t('Spesa e promemoria')],
              ['google', t('Accesso Google')],
              ['backup', t('Backup e dati')],
            ] as const
          ).map(([id, title]) => (
            <button
              key={id}
              onClick={() =>
                document.getElementById(`settings-${id}`)?.scrollIntoView({
                  behavior:
                    matchMedia('(prefers-reduced-motion: reduce)').matches ||
                    document.documentElement.dataset.motion === 'reduced'
                      ? 'instant'
                      : 'smooth',
                  block: 'start',
                })
              }
            >
              {title}
            </button>
          ))}
        </aside>
        <div className="settings-sections">
          <section className="settings-section" id="settings-profile">
            <h2>
              <UserRound size={21} />
              {t('Il tuo profilo')}
            </h2>
            <p className="muted">{account.email}</p>
            {profile ? (
              <form
                onSubmit={(e) => {
                  void submit(e)
                }}
              >
                <label>
                  {t('Come ti chiami?')}
                  <input name="username" defaultValue={profile.username} required maxLength={100} />
                </label>
                <button className="button primary" disabled={busy}>
                  {busy ? <Spinner /> : t('Salva profilo')}
                </button>
              </form>
            ) : (
              <Spinner />
            )}
          </section>
          <AppearanceSettings />
          <PasswordSettings />
          <section className="settings-section" id="settings-shopping">
            <h2>
              <ShoppingBasket size={21} />
              {t('Spesa e promemoria')}
            </h2>
            <p className="muted">
              {t(
                'Giorni d’acquisto, categorie e promemoria si configurano per ciascuna lista. Gli avvisi vengono recapitati sull’app Android.',
              )}
            </p>
            <a className="button secondary" href="#shopping">
              {t('Gestisci le liste della spesa')}
            </a>
          </section>
          <section className="settings-section" id="settings-google">
            <h2>
              <ShieldCheck size={21} />
              {t('Accesso Google')}
            </h2>
            <p className="muted">
              {account.googleLinked
                ? t('Google è collegato al tuo account. Puoi usarlo al prossimo accesso.')
                : t('Collega la stessa email per accedere senza digitare la password.')}
            </p>
            {!account.googleLinked && (
              <GoogleButton providers={providers} onCredential={link} onError={setError} />
            )}{' '}
            {account.googleLinked && (
              <span className="tag">
                <ShieldCheck size={15} />
                {t('Identità collegata')}
              </span>
            )}
          </section>
          {permits(account, 'BACKUP_MANAGE') && <DriveBackups />}
        </div>
      </div>
    </>
  )
}
