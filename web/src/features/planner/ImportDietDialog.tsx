import { useState, useRef } from 'react'
import { Modal, ErrorNotice, Spinner } from '../../components/ui'
import { useWorkspace } from '../../app/WorkspaceContext'
import { parseDietJson, importDiet, type ImportedDiet } from '../../lib/diet-import'
import { t } from '../../lib/i18n'

export function ImportDietDialog({ onClose }: { onClose: () => void }) {
  const { workspace, save, saving, error } = useWorkspace()
  const version = useRef(workspace!.version)
  const [raw, setRaw] = useState('')
  const [data, setData] = useState<ImportedDiet | null>(null)
  const [message, setMessage] = useState('')
  const [overwrite, setOverwrite] = useState(false)
  const [reading, setReading] = useState(false)
  const conflict = data?.uuid && workspace!.snapshot.diets.find((d) => d.uuid === data.uuid)
  function change(value: string) {
    setRaw(value)
    setData(null)
    setOverwrite(false)
    setMessage('')
  }
  return (
    <Modal
      title={t('Importa dieta da JSON')}
      onClose={() => {
        if (!saving) onClose()
      }}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (!data) {
            try {
              setData(parseDietJson(raw))
              setMessage('')
            } catch (e) {
              setMessage((e as Error).message)
            }
          } else if (!conflict || overwrite) {
            void save(importDiet(workspace!.snapshot, data, overwrite), version.current).then(
              (ok) => {
                if (ok) onClose()
              },
            )
          }
        }}
      >
        {(message || error) && <ErrorNotice message={message || error} />}
        <label>
          {t('File JSON')}
          <input
            type="file"
            accept=".json,application/json"
            disabled={saving || reading}
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (!file) return
              if (file.size > 5 * 1024 * 1024) {
                setMessage(t('Il file supera il limite di 5 MB.'))
                return
              }
              setReading(true)
              void file
                .text()
                .then(change)
                .catch(() => setMessage(t('Impossibile leggere il file.')))
                .finally(() => setReading(false))
            }}
          />
        </label>
        <label>
          {t('Oppure incolla il JSON')}
          <textarea
            rows={8}
            value={raw}
            onChange={(e) => change(e.target.value)}
            disabled={saving || reading}
            required
          />
        </label>
        {data && (
          <div className="surface import-preview">
            <h3>{data.name}</h3>
            <p>
              {t('{0} pasti · {1} ingredienti', {
                '0': data.meals.length,
                '1': data.meals.reduce(
                  (n, m) => n + m.courses.reduce((n, c) => n + c.items.length, 0),
                  0,
                ),
              })}
            </p>
            {conflict && (
              <label className="import-choice">
                <input
                  type="checkbox"
                  checked={overwrite}
                  onChange={(e) => setOverwrite(e.target.checked)}
                />
                {t('Sostituisci la dieta esistente e tutti i suoi pasti.')}
              </label>
            )}
          </div>
        )}
        <div className="modal-actions">
          <button type="button" className="button secondary" disabled={saving} onClick={onClose}>
            {t('Annulla')}
          </button>
          <button
            className="button primary"
            disabled={saving || reading || !raw.trim() || !!(data && conflict && !overwrite)}
          >
            {saving ? <Spinner /> : data ? t('Importa dieta') : t('Mostra anteprima')}
          </button>
        </div>
      </form>
    </Modal>
  )
}
