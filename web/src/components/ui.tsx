import { t } from '../lib/i18n'
import { useEffect, useRef, type ReactNode } from 'react'
import { X, LoaderCircle, AlertCircle, ArrowRight } from 'lucide-react'

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: ReactNode
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  )
}
export function Spinner() {
  return <LoaderCircle className="spin" size={20} aria-label={t('Caricamento')} />
}
export function ErrorNotice({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="notice error" role="alert">
      <AlertCircle size={20} />
      <span>{message}</span>
      {retry && (
        <button className="text-button" onClick={retry}>
          {t('Riprova')}
          <ArrowRight size={15} />
        </button>
      )}
    </div>
  )
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: ReactNode
  onClose: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = ref.current!
    dialog.showModal()
    return () => dialog.close()
  }, [])
  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
      aria-labelledby="modal-title"
    >
      <div className="modal-heading">
        <h2 id="modal-title">{title}</h2>
        <button className="icon-button" aria-label={t('Chiudi')} onClick={onClose}>
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  )
}
export function ConfirmDialog({
  title,
  description,
  actionLabel,
  onConfirm,
  onClose,
  busy = false,
  error,
}: {
  title: string
  description: string
  actionLabel: string
  onConfirm: () => void
  onClose: () => void
  busy?: boolean
  error?: string
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="muted">{description}</p>
      {error && <ErrorNotice message={error} />}
      <div className="modal-actions">
        <button className="button secondary" onClick={onClose} disabled={busy}>
          {t('Annulla')}
        </button>
        <button className="button danger" onClick={onConfirm} disabled={busy}>
          {busy ? <Spinner /> : actionLabel}
        </button>
      </div>
    </Modal>
  )
}
