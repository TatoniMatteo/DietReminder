import { t, localizedArray } from '../lib/i18n'
import { useEffect, useState } from 'react'
import { useLanguage } from '../lib/i18n'
import {
  CalendarDays,
  Leaf,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  RefreshCw,
  Settings2,
  ShieldCheck,
  ShoppingBasket,
} from 'lucide-react'
import { api, setCsrf } from '../lib/api'
import type { Providers, WebSession } from '../lib/types'
import { AccountContext, ready as accountReady } from '../lib/access'
import { EnrollmentPage } from '../features/auth/EnrollmentPage'
import { AdminIntroduction } from '../features/auth/AdminIntroduction'
import { AuthPage } from '../features/auth/AuthPage'
import { PlannerPage } from '../features/planner/PlannerPage'
import { DietsPage } from '../features/planner/DietsPage'
import { ShoppingPage } from '../features/shopping/ShoppingPage'
import { SettingsPage } from '../features/settings/SettingsPage'
import { AdminPage } from '../features/admin/AdminPage'
import { useWorkspace, WorkspaceProvider } from './WorkspaceContext'
import { EmptyState, ErrorNotice, Spinner } from '../components/ui'

const routes = localizedArray([
  {
    id: 'week',
    get label() {
      return t('La settimana')
    },
    icon: CalendarDays,
  },
  {
    id: 'diets',
    get label() {
      return t('I miei piani')
    },
    icon: Leaf,
  },
  {
    id: 'shopping',
    get label() {
      return t('La spesa')
    },
    icon: ShoppingBasket,
  },
  {
    id: 'settings',
    get label() {
      return t('Impostazioni')
    },
    icon: Settings2,
  },
])
export default function App() {
  useLanguage()
  const [session, setSession] = useState<WebSession | null>(null)
  const [ready, setReady] = useState(false)
  const [authNotice, setAuthNotice] = useState('')
  const [providers, setProviders] = useState<Providers>({
    clientId: '',
    signInEnabled: false,
    driveEnabled: false,
  })

  function accept(value: WebSession) {
    setProviders((current) => ({ ...current, firstRegistrationRequired: false }))
    setCsrf(value.csrfToken)
    setSession((current) =>
      current &&
      current.account.userId === value.account.userId &&
      (current.account.version ?? 0) > (value.account.version ?? 0)
        ? current
        : value,
    )
  }

  useEffect(() => {
    void Promise.allSettled([
      api<WebSession>('/api/web/auth/me')
        .then(accept)
        .catch(() => setSession(null)),
      api<Providers>('/api/auth/providers')
        .then(setProviders)
        .catch(() => {}),
    ]).finally(() => setReady(true))
    const expire = (event: Event) => {
      setAuthNotice((event as CustomEvent<{ notice?: string }>).detail?.notice ?? '')
      setCsrf('')
      setSession(null)
    }
    const changed = () => {
      void api<WebSession>('/api/web/auth/me')
        .then(accept)
        .catch(() => setSession(null))
    }
    window.addEventListener('session-expired', expire)
    window.addEventListener('account-status-changed', changed)
    return () => {
      window.removeEventListener('session-expired', expire)
      window.removeEventListener('account-status-changed', changed)
    }
  }, [])
  useEffect(() => {
    if (!session) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible')
        void api<WebSession>('/api/web/auth/me')
          .then(accept)
          .catch(() => {})
    }, 30000)
    return () => clearInterval(timer)
  }, [session?.account.userId])
  if (!ready)
    return (
      <div className="boot-screen">
        <span className="brand-icon">
          <img className="brand-logo" src="/logo.svg" alt="Diet Reminder" />
        </span>
        <Spinner />
        <span className="muted">{t('Il tuo spazio sta arrivando…')}</span>
      </div>
    )
  if (!session)
    return <AuthPage providers={providers} onSession={accept} initialNotice={authNotice} />
  const refreshAccount = async () => accept(await api<WebSession>('/api/web/auth/me'))
  if (!accountReady(session.account))
    return (
      <EnrollmentPage
        account={session.account}
        refresh={refreshAccount}
        logout={async () => {
          await api('/api/web/auth/logout', 'POST')
          setCsrf('')
          setSession(null)
        }}
      />
    )
  return (
    <AccountContext.Provider value={session.account}>
      {session.account.role === 'ADMIN' && session.account.adminIntroductionPending && (
        <AdminIntroduction refresh={refreshAccount} />
      )}
      <WorkspaceProvider key={session.account.userId}>
        <Shell
          session={session}
          providers={providers}
          refreshAccount={async () => accept(await api<WebSession>('/api/web/auth/me'))}
          onLogout={() => {
            setCsrf('')
            setSession(null)
          }}
        />
      </WorkspaceProvider>
    </AccountContext.Provider>
  )
}

function Shell({
  session,
  providers,
  onLogout,
  refreshAccount,
}: {
  session: WebSession
  providers: Providers
  onLogout: () => void
  refreshAccount: () => Promise<void>
}) {
  const [route, setRoute] = useState(location.hash.slice(1).split('?')[0] || 'week')
  const [collapsed, setCollapsed] = useState(false)
  const [error, setError] = useState('')
  const { workspace, loading, saving, error: workspaceError, conflict, refresh } = useWorkspace()
  useEffect(() => {
    const change = () => setRoute(location.hash.slice(1).split('?')[0] || 'week')
    window.addEventListener('hashchange', change)
    return () => window.removeEventListener('hashchange', change)
  }, [])
  const navigate = (id: string) => {
    location.hash = id
    window.scrollTo({ top: 0, behavior: 'instant' })
  }
  const account = session.account

  async function logout() {
    try {
      await api('/api/web/auth/logout', 'POST')
      onLogout()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const label =
    route === 'admin'
      ? t('Amministrazione')
      : (routes.find((x) => x.id === route)?.label ?? t('La settimana'))
  return (
    <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <aside className="sidebar">
        <a className="brand" href="#week">
          <span className="brand-icon">
            <img className="brand-logo" src="/logo.svg" alt="" />
          </span>
          <span className="brand-name">
            Diet Reminder<small>{t('IL TUO EQUILIBRIO QUOTIDIANO')}</small>
          </span>
        </a>
        <span className="nav-heading">{t('IL TUO QUOTIDIANO')}</span>
        <nav aria-label={t('Navigazione principale')}>
          {routes.map(({ id, label, icon: Icon }) => (
            <a
              href={`#${id}`}
              key={id}
              className={route === id ? 'active' : ''}
              aria-current={route === id ? 'page' : undefined}
            >
              <Icon size={21} />
              <span>{label}</span>
              {route === id && <i />}
            </a>
          ))}
        </nav>
        {account.role === 'ADMIN' && (
          <>
            <span className="nav-heading admin-nav-heading">{t('GESTISCI')}</span>
            <nav aria-label={t('Amministrazione')}>
              <a href="#admin" className={route === 'admin' ? 'active' : ''}>
                <ShieldCheck size={21} />
                <span>{t('Amministrazione')}</span>
              </a>
            </nav>
          </>
        )}
        <div className="sidebar-bottom">
          <div className="sidebar-profile">
            <span className="avatar">{account.username.slice(0, 1).toUpperCase()}</span>
            <span>
              <strong>{account.username}</strong>
              <small>
                {account.role === 'ADMIN' ? t('Amministratore') : t('Il tuo spazio personale')}
              </small>
            </span>
            <button
              className="icon-button"
              onClick={() => {
                void logout()
              }}
              aria-label={t('Esci dall’account')}
              title={t('Esci')}
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>
      <div className="main-column">
        <header className="topbar">
          <div className="breadcrumb">
            <button
              className="icon-button sidebar-toggle"
              aria-label={collapsed ? t('Espandi menu') : t('Riduci menu')}
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}
            </button>
            <span>{t('Il tuo quotidiano')}</span>
            <span className="breadcrumb-slash">/</span>
            <strong>{label}</strong>
          </div>
          <div className="topbar-right">
            <span className={`sync-label ${saving || loading || workspaceError ? 'saving' : ''}`}>
              <span className="dot" />
              {saving
                ? t('Salvataggio…')
                : loading
                  ? t('Sincronizzazione…')
                  : conflict
                    ? t('Da ricaricare')
                    : workspaceError
                      ? t('Da verificare')
                      : t('Sincronizzato')}
            </span>
            <button
              className="icon-button"
              aria-label={t('Sincronizza adesso')}
              disabled={saving || loading}
              onClick={() => {
                void refresh()
              }}
            >
              <RefreshCw size={18} className={loading ? 'spin' : ''} />
            </button>
            <span className="avatar small">{account.username.slice(0, 1).toUpperCase()}</span>
            <button
              className="icon-button mobile-logout"
              onClick={() => {
                void logout()
              }}
              aria-label={t('Esci dall’account')}
              title={t('Esci')}
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>
        <main id="main" className="main-content">
          {error && <ErrorNotice message={error} />}{' '}
          {workspaceError && (
            <ErrorNotice
              message={workspaceError}
              retry={() => {
                void refresh()
              }}
            />
          )}
          <div className="page-enter" key={route}>
            {route === 'admin' ? (
              account.role === 'ADMIN' ? (
                <AdminPage />
              ) : (
                <EmptyState
                  icon={<ShieldCheck size={28} />}
                  title={t('Uno spazio riservato')}
                  description={t('Questa pagina è accessibile soltanto agli amministratori.')}
                />
              )
            ) : !workspace ? (
              <div className="loading-workspace">
                <Spinner />
                <p className="muted">{t('Prepariamo la tua settimana…')}</p>
              </div>
            ) : route === 'diets' ? (
              <DietsPage navigate={navigate} />
            ) : route === 'shopping' ? (
              <ShoppingPage />
            ) : route === 'settings' ? (
              <SettingsPage
                account={account}
                providers={providers}
                refreshAccount={refreshAccount}
              />
            ) : (
              <PlannerPage navigate={navigate} />
            )}
          </div>
          <footer className="page-footer">
            <span>Diet Reminder</span>
            <span>{t('Un po’ più di equilibrio, ogni giorno.')}</span>
          </footer>
        </main>
      </div>
    </div>
  )
}
