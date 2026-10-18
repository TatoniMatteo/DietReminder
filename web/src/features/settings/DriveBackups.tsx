import { t, locale } from '../../lib/i18n'
import { useCallback, useEffect, useState } from 'react'
import {
  CloudUpload,
  CloudDownload,
  Link2,
  Unlink,
  RefreshCw,
  Trash2,
  HardDrive,
} from 'lucide-react'
import { api } from '../../lib/api'
import type { Backup, DriveStatus } from '../../lib/types'
import { useWorkspace } from '../../app/WorkspaceContext'
import { ConfirmDialog, ErrorNotice, Spinner } from '../../components/ui'
export function DriveBackups() {
  const { workspace, refresh } = useWorkspace()
  const [status, setStatus] = useState<DriveStatus | null>(null)
  const [backups, setBackups] = useState<Backup[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [confirm, setConfirm] = useState<{
    kind: 'restore' | 'delete' | 'disconnect'
    backup?: Backup
    baseVersion?: number
  } | null>(null)
  const [connecting, setConnecting] = useState(false)
  const load = useCallback(async () => {
    try {
      const s = await api<DriveStatus>('/api/backups/google/status')
      setStatus(s)
      if (s.connected) {
        setBackups(await api<Backup[]>('/api/backups/google'))
        setConnecting(false)
      } else setBackups([])
    } catch (e) {
      setError((e as Error).message)
    }
  }, [])
  useEffect(() => {
    void load()
    const focus = () => {
      void load()
    }
    window.addEventListener('focus', focus)
    return () => window.removeEventListener('focus', focus)
  }, [load])
  useEffect(() => {
    if (!connecting) return
    const timer = setInterval(() => {
      void load()
    }, 3000)
    const timeout = setTimeout(() => setConnecting(false), 5 * 60 * 1000)
    return () => {
      clearInterval(timer)
      clearTimeout(timeout)
    }
  }, [connecting, load])
  async function connect() {
    const popup = window.open(
      'about:blank',
      'dietreminder-google-drive',
      'popup,width=550,height=720',
    )
    if (!popup) {
      setError(t('Consenti l’apertura della finestra Google nel browser e riprova.'))
      return
    }
    popup.opener = null
    setBusy(true)
    setError('')
    try {
      const { url } = await api<{ url: string }>('/api/backups/google/authorize', 'POST')
      popup.location.href = url
      setConnecting(true)
    } catch (e) {
      popup.close()
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function create() {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await api('/api/backups/google', 'POST')
      await load()
      setNotice(t('La tua copia è al sicuro su Google Drive.'))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  async function perform() {
    if (!confirm) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      if (confirm.kind === 'disconnect') {
        await api('/api/backups/google/connection', 'DELETE')
        setNotice(t('Google Drive scollegato. Le copie già create restano nel tuo spazio Google.'))
      } else if (confirm.kind === 'delete')
        await api(`/api/backups/google/${confirm.backup!.id}`, 'DELETE')
      else {
        await api('/api/backups/google/restore', 'POST', {
          fileId: confirm.backup!.id,
          baseVersion: confirm.baseVersion,
          requestId: crypto.randomUUID(),
        })
        await refresh()
        setNotice(t('Backup ripristinato. I dati verranno sincronizzati anche con Android.'))
      }
      setConfirm(null)
      await load()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="settings-section" id="settings-backup">
      <div className="section-heading">
        <span className="subheading">
          <HardDrive size={21} />
          {t('Il tuo spazio su Google Drive')}
        </span>
        {busy && <Spinner />}
      </div>
      <p className="muted">
        {t(
          'Una copia dei tuoi piani, ingredienti, liste e profilo. Privata, nel tuo account Google.',
        )}
      </p>
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
        <p role="status" className="notice success">
          {notice}
        </p>
      )}
      {!status ? (
        <p className="muted small">{t('Controllo del collegamento…')}</p>
      ) : !status.available ? (
        <p className="notice">
          {t('Il backup Google sarà disponibile quando il servizio sarà configurato.')}
        </p>
      ) : (
        <>
          <div className="connection-row">
            <span className={`status-icon ${status.connected ? 'connected' : ''}`}>
              <HardDrive size={22} />
            </span>
            <span>
              <strong>
                {status.connected ? t('Google Drive collegato') : t('Non ancora collegato')}
              </strong>
              <small>
                {status.email ?? t('Autorizza solo la cartella privata di Diet Reminder')}
              </small>
            </span>
            <button
              className="button secondary small"
              disabled={busy}
              onClick={() => {
                void connect()
              }}
            >
              <Link2 size={16} />
              {status.connected ? t('Ricollega') : t('Collega Google Drive')}
            </button>
          </div>
          {connecting && (
            <p role="status" className="muted small">
              {t(
                'Completa l’autorizzazione nella finestra Google. Lo stato si aggiornerà automaticamente.',
              )}
            </p>
          )}
          {status.connected && (
            <>
              <div className="section-heading backup-heading">
                <h3>
                  {t('Le tue copie')}
                  <span className="count-badge">{backups.length}</span>
                </h3>
                <button
                  className="button primary small"
                  disabled={busy}
                  onClick={() => {
                    void create()
                  }}
                >
                  <CloudUpload size={17} />
                  {t('Crea backup')}
                </button>
              </div>
              {backups.length === 0 && (
                <p className="muted small">
                  {t('Non hai ancora creato una copia. È un buon momento per farlo.')}
                </p>
              )}
              {backups.map((backup) => (
                <div className="backup-row" key={backup.id}>
                  <CloudDownload size={20} />
                  <span>
                    <strong>
                      {new Date(backup.createdTime).toLocaleString(locale(), {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      })}
                    </strong>
                    <small>
                      {backup.size
                        ? `${Math.ceil(Number(backup.size) / 1024)} KB`
                        : t('Backup completo dei dati condivisi')}
                    </small>
                  </span>
                  <button
                    className="text-button"
                    disabled={busy}
                    onClick={() =>
                      setConfirm({ kind: 'restore', backup, baseVersion: workspace!.version })
                    }
                  >
                    <RefreshCw size={15} />
                    {t('Ripristina')}
                  </button>
                  <button
                    className="icon-button"
                    aria-label={t('Elimina backup {0}', { '0': backup.createdTime })}
                    disabled={busy}
                    onClick={() => setConfirm({ kind: 'delete', backup })}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
              <button
                className="text-button muted"
                disabled={busy}
                onClick={() => setConfirm({ kind: 'disconnect' })}
              >
                <Unlink size={15} />
                {t('Scollega Google Drive')}
              </button>
            </>
          )}
        </>
      )}
      {confirm && (
        <ConfirmDialog
          title={
            confirm.kind === 'restore'
              ? t('Tornare a questa copia?')
              : confirm.kind === 'disconnect'
                ? t('Scollegare Google Drive?')
                : t('Eliminare questa copia?')
          }
          description={
            confirm.kind === 'restore'
              ? t(
                  'Il backup sostituirà piani, pasti, ingredienti, liste e profilo attuali su tutti i dispositivi. Le preferenze grafiche e i promemoria locali non cambiano. Crea prima una copia se vuoi conservare lo stato attuale.',
                )
              : confirm.kind === 'disconnect'
                ? t(
                    'Non potrai creare o ripristinare copie finché non ricolleghi l’account. I backup già salvati non saranno eliminati.',
                  )
                : t('Questa copia verrà eliminata definitivamente da Google Drive.')
          }
          actionLabel={
            confirm.kind === 'restore'
              ? t('Ripristina backup')
              : confirm.kind === 'disconnect'
                ? t('Scollega')
                : t('Elimina copia')
          }
          busy={busy}
          error={error}
          onConfirm={() => {
            void perform()
          }}
          onClose={() => setConfirm(null)}
        />
      )}
    </section>
  )
}
