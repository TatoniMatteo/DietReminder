import { useState } from 'react'
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
      title={t('Sei l’amministratore')}
      onClose={() => {
        void acknowledge()
      }}
    >
      <p>
        {t(
          'La tua email è verificata. Il tuo account amministra questa installazione di Diet Reminder.',
        )}
      </p>
      <h3>{t('I tuoi poteri')}</h3>
      <ul>
        <li>{t('Approva o rifiuta le richieste dopo la verifica dell’email.')}</li>
        <li>{t('Assegna i permessi di lettura, modifica e backup secondo le necessità.')}</li>
        <li>
          {t(
            'Gestisci sospensioni, sessioni e aggiornamenti Android dal pannello Amministrazione.',
          )}
        </li>
      </ul>
      <h3>{t('Le tue responsabilità')}</h3>
      <ul>
        <li>{t('Verifica le richieste e assegna solo i permessi necessari.')}</li>
        <li>
          {t(
            'Proteggi le credenziali, rispetta la riservatezza e controlla il registro delle operazioni.',
          )}
        </li>
        <li>
          {t(
            'Mantieni almeno un amministratore attivo. Il sistema protegge l’ultimo amministratore.',
          )}
        </li>
      </ul>
      <p>{t('Questa conferma viene salvata nell’account e vale su tutti i dispositivi.')}</p>
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
