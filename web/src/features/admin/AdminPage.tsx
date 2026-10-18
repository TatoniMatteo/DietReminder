import { Select } from '../../components/Select'
import { t, locale } from '../../lib/i18n'
import { useEffect, useState } from 'react'
import {
  Users,
  UserCheck,
  HardDrive,
  Layers,
  Search,
  ShieldCheck,
  Ban,
  KeyRound,
  Check,
  ChevronLeft,
  ChevronRight,
  Activity,
} from 'lucide-react'
import { permissions, type Permission } from '../../lib/access'
import { enrollmentLabels } from '../auth/EnrollmentPage'
import { api } from '../../lib/api'
import type { AdminUser, Audit, Overview, UserPage } from '../../lib/types'
import { ConfirmDialog, ErrorNotice, Spinner } from '../../components/ui'
const permissionLabels: Record<Permission, string> = {
  WORKSPACE_READ: 'Consultare piani e spesa',
  DIETS_WRITE: 'Modificare piani e pasti',
  SHOPPING_WRITE: 'Modificare la spesa',
  PROFILE_WRITE: 'Modificare il profilo',
  BACKUP_MANAGE: 'Gestire i backup Google Drive',
}
export function AdminPage() {
  const [tab, setTab] = useState<'overview' | 'users'>('overview')
  const [overview, setOverview] = useState<Overview | null>(null)
  const [users, setUsers] = useState<UserPage | null>(null)
  const [audit, setAudit] = useState<Audit[]>([])
  const [enrollmentFilter, setEnrollmentFilter] = useState('')
  const [reviewing, setReviewing] = useState<AdminUser | null>(null)
  const [grants, setGrants] = useState<Permission[]>([])
  const [reason, setReason] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [action, setAction] = useState<{ kind: 'status' | 'revoke'; user: AdminUser } | null>(null)
  async function load() {
    try {
      const [o, a] = await Promise.all([
        api<Overview>('/api/admin/overview'),
        api<Audit[]>('/api/admin/audit'),
      ])
      setOverview(o)
      setAudit(a)
    } catch (e) {
      setError((e as Error).message)
    }
  }
  useEffect(() => {
    void load()
  }, [])
  useEffect(() => {
    let active = true
    const timer = setTimeout(() => {
      void api<UserPage>(
        `/api/admin/users?search=${encodeURIComponent(search)}&page=${page}&enrollmentStatus=${enrollmentFilter}`,
      )
        .then((value) => {
          if (active) setUsers(value)
        })
        .catch((e) => {
          if (active) setError((e as Error).message)
        })
    }, 200)
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [search, page, overview, enrollmentFilter])
  function startReview(user: AdminUser) {
    setReviewing(user)
    setGrants(user.permissions ?? [...permissions])
    setReason('')
    setError('')
  }
  async function review(approved: boolean) {
    if (!reviewing || busy) return
    setBusy(true)
    setError('')
    try {
      await api(`/api/admin/users/${reviewing.id}/review`, 'POST', {
        version: reviewing.version ?? 0,
        approved,
        permissions: grants,
        reason,
      })
      setReviewing(null)
      setNotice(t('Decisione registrata. L’utente riceverà una notifica via email.'))
      await load()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function perform() {
    if (!action) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      if (action.kind === 'status')
        await api(`/api/admin/users/${action.user.id}/status`, 'PATCH', {
          enabled: !action.user.enabled,
        })
      else await api(`/api/admin/users/${action.user.id}/revoke-sessions`, 'POST')
      setAction(null)
      setNotice(t('Operazione completata e registrata.'))
      await load()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  const actionNames: Record<string, string> = {
    ACCOUNT_APPROVED: t('Richiesta approvata'),
    ACCOUNT_REJECTED: t('Richiesta rifiutata'),
    USER_ENABLED: t('Utente riattivato'),
    USER_SUSPENDED: t('Utente sospeso'),
    SESSIONS_REVOKED: t('Sessioni revocate'),
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            <ShieldCheck size={14} />
            {t('AMMINISTRAZIONE')}
          </span>
          <h1>
            {t('Tutto sotto controllo')}
            <span className="heading-dot">.</span>
          </h1>
          <p className="muted">{t('Le informazioni utili. Le azioni che servono.')}</p>
        </div>
        <span className="tag">
          <ShieldCheck size={13} />
          {t('Area protetta')}
        </span>
      </div>
      <div className="admin-tabs segmented">
        <button className={tab === 'overview' ? 'selected' : ''} onClick={() => setTab('overview')}>
          <Activity size={17} />
          {t('Panoramica')}
        </button>
        <button className={tab === 'users' ? 'selected' : ''} onClick={() => setTab('users')}>
          <Users size={17} />
          {t('Utenti')}
        </button>
      </div>
      {error && (
        <ErrorNotice
          message={error}
          retry={() => {
            setError('')
            void load()
          }}
        />
      )}{' '}
      {notice && (
        <p className="notice success" role="status">
          {notice}
        </p>
      )}
      {tab === 'overview' && (
        <>
          {overview ? (
            <div className="stats-grid">
              {[
                {
                  icon: Users,
                  value: overview.users,
                  label: t('Utenti registrati'),
                  detail: t('{0} con Google', { '0': overview.googleUsers }),
                },
                {
                  icon: UserCheck,
                  value: overview.activeUsers,
                  label: t('Account attivi'),
                  detail: t('{0} sospesi', { '0': overview.users - overview.activeUsers }),
                },
                {
                  icon: Layers,
                  value: overview.workspaces,
                  label: t('Workspace condivisi'),
                  detail: t('Android e web'),
                },
                {
                  icon: HardDrive,
                  value: overview.driveConnections,
                  label: t('Google Drive collegati'),
                  detail: t('Backup personali'),
                },
              ].map(({ icon: Icon, value, label, detail }) => (
                <article className="surface stat-card" key={label}>
                  <span className="stat-icon">
                    <Icon size={23} />
                  </span>
                  <strong>{value}</strong>
                  <span>{label}</span>
                  <small>{detail}</small>
                </article>
              ))}
            </div>
          ) : (
            <Spinner />
          )}
          <section className="surface settings-card">
            <div className="section-heading">
              <h2>{t('Operazioni recenti')}</h2>
              <span className="muted small">{t('Ultime 50 operazioni amministrative')}</span>
            </div>
            {audit.length === 0 ? (
              <p className="muted">{t('Nessuna operazione registrata.')}</p>
            ) : (
              <div className="audit-list">
                {audit.map((a) => (
                  <div className="audit-row" key={a.id}>
                    <span className="audit-icon">
                      <ShieldCheck size={17} />
                    </span>
                    <span>
                      <strong>{actionNames[a.action] ?? a.action}</strong>
                      <small>
                        {t('Destinatario:')}{' '}
                        {a.targetId === 'android'
                          ? t('App Android')
                          : (users?.users.find((u) => u.id === a.targetId)?.email ?? a.targetId)}
                        <br />
                        {t('Eseguito da:')}{' '}
                        {users?.users.find((u) => u.id === a.actorId)?.email ?? a.actorId}
                      </small>
                    </span>
                    <time>
                      {new Date(a.createdAt).toLocaleString(locale(), {
                        dateStyle: 'short',
                        timeStyle: 'short',
                      })}
                    </time>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
      {tab === 'users' && (
        <section className="surface settings-card">
          <div className="section-heading">
            <h2>{t('Persone, prima dei numeri')}</h2>
            <label className="search-field">
              <Search size={18} />
              <input
                aria-label={t('Cerca utenti')}
                value={search}
                placeholder={t('Cerca nome o email')}
                onChange={(e) => {
                  setSearch(e.target.value)
                  setPage(0)
                }}
              />
            </label>
          </div>
          <label>
            {t('Stato della richiesta')}
            <Select
              aria-label={t('Stato della richiesta')}
              value={enrollmentFilter}
              onValueChange={(value) => {
                setEnrollmentFilter(value)
                setPage(0)
              }}
            >
              <option value="">{t('Tutti gli utenti')}</option>
              {Object.entries(enrollmentLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {t(label)}
                </option>
              ))}
            </Select>
          </label>
          {reviewing && (
            <section className="app-card" aria-label={t('Valuta richiesta')}>
              <h2>
                {t('Valuta richiesta')}: {reviewing.username}
              </h2>
              <p>{reviewing.email}</p>
              <p>{t('L’email è verificata. Seleziona i permessi da assegnare.')}</p>
              {permissions.map((permission) => (
                <label className="checkbox-label" key={permission}>
                  <input
                    type="checkbox"
                    checked={grants.includes(permission)}
                    disabled={busy || permission === 'WORKSPACE_READ'}
                    onChange={(e) =>
                      setGrants(
                        e.target.checked
                          ? [...grants, permission]
                          : grants.filter((x) => x !== permission),
                      )
                    }
                  />
                  {t(permissionLabels[permission])}
                </label>
              ))}
              <label>
                {t('Motivazione per l’utente')}
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  maxLength={1000}
                />
              </label>
              <div className="modal-actions">
                <button
                  className="button secondary"
                  disabled={busy}
                  onClick={() => setReviewing(null)}
                >
                  {t('Annulla')}
                </button>
                {reviewing.enrollmentStatus !== 'APPROVED' && (
                  <button
                    className="button danger"
                    disabled={busy || !reason.trim()}
                    onClick={() => {
                      void review(false)
                    }}
                  >
                    {t('Rifiuta richiesta')}
                  </button>
                )}
                <button
                  className="button primary"
                  disabled={busy}
                  onClick={() => {
                    void review(true)
                  }}
                >
                  {busy ? (
                    <Spinner />
                  ) : (
                    t(
                      reviewing.enrollmentStatus === 'APPROVED'
                        ? 'Salva permessi'
                        : 'Approva richiesta',
                    )
                  )}
                </button>
              </div>
            </section>
          )}
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>{t('Utente')}</th>
                  <th>{t('Accesso')}</th>
                  <th>{t('Stato')}</th>
                  <th>{t('Registrazione')}</th>
                  <th>{t('Azioni')}</th>
                </tr>
              </thead>
              <tbody>
                {users?.users.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <strong>{user.username}</strong>
                      <small>{user.email}</small>
                      {user.role === 'ADMIN' && (
                        <span className="tag small">{t('Amministratore')}</span>
                      )}
                    </td>
                    <td>{user.googleLinked ? 'Google' : t('Email')}</td>
                    <td>
                      <span className={`status-label ${user.enabled ? 'enabled' : 'suspended'}`}>
                        <span className="dot" />
                        {!user.enabled
                          ? t('Sospeso')
                          : user.enrollmentStatus && user.enrollmentStatus !== 'APPROVED'
                            ? t(enrollmentLabels[user.enrollmentStatus])
                            : t('Attivo')}
                      </span>
                    </td>
                    <td>{new Date(user.createdAt).toLocaleDateString(locale())}</td>
                    <td>
                      <div className="table-actions">
                        {user.role !== 'ADMIN' &&
                          user.emailVerified !== false &&
                          (user.enrollmentStatus === 'PENDING_APPROVAL' ||
                            user.enrollmentStatus === 'APPROVED') && (
                            <button className="text-button" onClick={() => startReview(user)}>
                              {t(
                                user.enrollmentStatus === 'APPROVED'
                                  ? 'Permessi'
                                  : 'Valuta richiesta',
                              )}
                            </button>
                          )}
                        <button
                          className="icon-button"
                          aria-label={t('Revoca sessioni di {0}', { '0': user.email })}
                          title={t('Revoca tutte le sessioni')}
                          onClick={() => setAction({ kind: 'revoke', user })}
                        >
                          <KeyRound size={18} />
                        </button>
                        {user.role !== 'ADMIN' && (
                          <button
                            className="icon-button"
                            aria-label={`${user.enabled ? t('Sospendi') : t('Riattiva')} ${user.email}`}
                            onClick={() => setAction({ kind: 'status', user })}
                          >
                            {user.enabled ? <Ban size={18} /> : <Check size={18} />}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {users?.users.length === 0 && <p className="muted">{t('Nessun utente trovato.')}</p>}
          <div className="pagination">
            <span className="muted small">
              {users?.total ?? 0} {t('utenti · Pagina')} {page + 1} {t('di')}{' '}
              {Math.max(1, users?.pages ?? 1)}
            </span>
            <button
              className="icon-button"
              aria-label={t('Pagina precedente')}
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              <ChevronLeft size={20} />
            </button>
            <button
              className="icon-button"
              aria-label={t('Pagina successiva')}
              disabled={!users || page + 1 >= users.pages}
              onClick={() => setPage(page + 1)}
            >
              <ChevronRight size={20} />
            </button>
          </div>
        </section>
      )}
      {action && (
        <ConfirmDialog
          title={
            action.kind === 'revoke'
              ? t('Revocare le sessioni?')
              : action.user.enabled
                ? t('Sospendere l’account?')
                : t('Riattivare l’account?')
          }
          description={
            action.kind === 'revoke'
              ? t(
                  '{0} dovrà accedere di nuovo su tutti i dispositivi. I suoi dati rimarranno disponibili.',
                  { '0': action.user.email },
                )
              : action.user.enabled
                ? t(
                    '{0} non potrà più accedere. Le sessioni saranno revocate; i dati saranno conservati.',
                    { '0': action.user.email },
                  )
                : t('{0} potrà accedere di nuovo con le sue credenziali.', {
                    '0': action.user.email,
                  })
          }
          actionLabel={
            action.kind === 'revoke'
              ? t('Revoca sessioni')
              : action.user.enabled
                ? t('Sospendi account')
                : t('Riattiva account')
          }
          busy={busy}
          error={error}
          onConfirm={() => {
            void perform()
          }}
          onClose={() => setAction(null)}
        />
      )}
    </>
  )
}
