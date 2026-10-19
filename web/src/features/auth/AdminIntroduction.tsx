import { useState } from 'react'
import { UserCheck, SlidersHorizontal, History } from 'lucide-react'
import { api } from '../../lib/api'
import { t } from '../../lib/i18n'
import { Modal, ErrorNotice, Spinner } from '../../components/ui'
export function AdminIntroduction({ refresh }: { refresh: () => Promise<void> }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function acknowledge() {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await api('/api/auth/admin-introduction', 'POST')
      await refresh()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal
      title={t('Benvenuto in Diet Reminder')}
      onClose={() => {
        void acknowledge()
      }}
    >
      <div className="admin-introduction">
        <p>
          {t(
            'Hai creato il primo account di questo spazio: puoi aiutare le altre persone a iniziare a usare Diet Reminder.',
          )}
        </p>
        <div className="admin-introduction-point">
          <UserCheck size={22} aria-hidden="true" />
          <p>
            {t(
              'Quando qualcuno si registra e verifica la sua email, trovi la richiesta in Amministrazione. Da lì puoi approvarla oppure lasciare un messaggio per spiegare perché non è stata accolta.',
            )}
          </p>
        </div>
        <div className="admin-introduction-point">
          <SlidersHorizontal size={22} aria-hidden="true" />
          <p>
            {t(
              'Per ogni persona scegli cosa può consultare o modificare. Puoi cambiare questi permessi anche in seguito, quando serve.',
            )}
          </p>
        </div>
        <div className="admin-introduction-point">
          <History size={22} aria-hidden="true" />
          <p>
            {t(
              'Nello stesso pannello trovi gli account e le operazioni recenti. Se serve, puoi sospendere un accesso o chiudere le sessioni aperte, senza cancellare i dati.',
            )}
          </p>
        </div>
        <p className="muted small">
          {t('Per ora è tutto. Puoi tornare al pannello Amministrazione in qualsiasi momento.')}
        </p>
      </div>
      {error && <ErrorNotice message={error} />}
      <button
        className="button primary"
        disabled={busy}
        onClick={() => {
          void acknowledge()
        }}
      >
        {busy ? <Spinner /> : t('Ho capito, continua')}
      </button>
    </Modal>
  )
}
