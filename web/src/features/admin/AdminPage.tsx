import { t, locale } from '../../lib/i18n'
import { useEffect, useState, type FormEvent } from 'react'
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
  Save,
  Smartphone,
} from 'lucide-react'
import { permissions, type Permission } from '../../lib/access'
import { enrollmentLabels } from '../auth/EnrollmentPage'
import { api } from '../../lib/api'
import type {
  AdminUser,
  Audit,
  ManagedPolicy,
  Overview,
  UserPage,
  VersionPolicy,
} from '../../lib/types'
import { ConfirmDialog, ErrorNotice, Spinner } from '../../components/ui'
const permissionLabels: Record<Permission, string> = {
  WORKSPACE_READ: 'Consultare piani e spesa',
  DIETS_WRITE: 'Modificare piani e pasti',
  SHOPPING_WRITE: 'Modificare la spesa',
  PROFILE_WRITE: 'Modificare il profilo',
  BACKUP_MANAGE: 'Gestire i backup Google Drive',
}
export function AdminPage() {
  const [tab, setTab] = useState<'overview' | 'users' | 'releases'>('overview')
  const [overview, setOverview] = useState<Overview | null>(null)
  const [users, setUsers] = useState<UserPage | null>(null)
  const [audit, setAudit] = useState<Audit[]>([])
  const [policy, setPolicy] = useState<ManagedPolicy | null>(null)
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
      const [o, a, p] = await Promise.all([
        api<Overview>('/api/admin/overview'),
        api<Audit[]>('/api/admin/audit'),
        api<ManagedPolicy>('/api/admin/version-policy'),
      ])
      setOverview(o)
      setAudit(a)
      setPolicy(p)
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
  async function publish(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!policy) return
    const form = new FormData(e.currentTarget)
    const updated: VersionPolicy = { ...policy.policy }
    for (const key of [
      'minSupportedVersionCode',
      'deprecatedVersionCode',
      'latestVersionCode',
      'minSupportedMajor',
    ] as const)
      updated[key] = Number(form.get(key))
    updated.latestVersionName = String(form.get('latestVersionName'))
    updated.updateUrl = String(form.get('updateUrl'))
    setBusy(true)
    setError('')
    setNotice('')
    try {
      setPolicy(
        await api<ManagedPolicy>('/api/admin/version-policy', 'PUT', {
          policy: updated,
          revision: policy.revision,
        }),
      )
      setNotice(
        t('Informazioni pubblicate. Android le riceverà al prossimo controllo aggiornamenti.'),
      )
      setAudit(await api<Audit[]>('/api/admin/audit'))
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
    RELEASE_POLICY_UPDATED: t('Aggiornamento Android pubblicato'),
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
        <button className={tab === 'releases' ? 'selected' : ''} onClick={() => setTab('releases')}>
          <Smartphone size={17} />
          {t('Aggiornamenti')}
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
            <select
              aria-label={t('Stato della richiesta')}
              value={enrollmentFilter}
              onChange={(e) => {
                setEnrollmentFilter(e.target.value)
                setPage(0)
              }}
            >
              <option value="">{t('Tutti gli utenti')}</option>
              {Object.entries(enrollmentLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {t(label)}
                </option>
              ))}
            </select>
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
      {tab === 'releases' && policy && (
        <section className="surface settings-card release-settings">
          <div className="section-heading">
            <h2>
              <Smartphone size={21} />
              {t('Aggiornamenti Android')}
            </h2>
            <span className="tag">
              {t('Revisione')} {policy.revision}
            </span>
          </div>
          <p className="muted">
            {t(
              'Queste informazioni sono distribuite dal server. Il collegamento può puntare alla release APK su GitHub o al tuo servizio di download.',
            )}
          </p>
          <form
            key={policy.revision}
            onSubmit={(e) => {
              void publish(e)
            }}
          >
            <div className="form-row">
              <label>
                {t('Ultima versione')}
                <input
                  name="latestVersionName"
                  required
                  defaultValue={policy.policy.latestVersionName}
                  placeholder="1.3.0"
                />
              </label>
              <label>
                {t('Codice ultima versione')}
                <input
                  name="latestVersionCode"
                  type="number"
                  required
                  min={1}
                  defaultValue={policy.policy.latestVersionCode}
                />
              </label>
            </div>
            <div className="form-row">
              <label>
                {t('Codice minimo supportato')}
                <input
                  name="minSupportedVersionCode"
                  type="number"
                  min={1}
                  required
                  defaultValue={policy.policy.minSupportedVersionCode}
                />
                <small>{t('Le versioni inferiori saranno considerate obsolete.')}</small>
              </label>
              <label>
                {t('Codice sotto cui avvisare')}
                <input
                  name="deprecatedVersionCode"
                  type="number"
                  min={1}
                  required
                  defaultValue={policy.policy.deprecatedVersionCode}
                />
                <small>{t('Tra minimo supportato e ultima versione.')}</small>
              </label>
            </div>
            <label>
              {t('Major minima supportata')}
              <input
                name="minSupportedMajor"
                type="number"
                min={1}
                required
                defaultValue={policy.policy.minSupportedMajor}
              />
            </label>
            <label>
              {t('Indirizzo aggiornamento')}
              <input type="url" name="updateUrl" required defaultValue={policy.policy.updateUrl} />
            </label>
            <p className="notice">
              {t(
                'Pubblica soltanto versioni già scaricabili. Alzare la versione minima può bloccare le modifiche nelle vecchie app.',
              )}
            </p>
            <button className="button primary" disabled={busy}>
              {busy ? <Spinner /> : <Save size={18} />}
              {t('Pubblica informazioni')}
            </button>
          </form>
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
