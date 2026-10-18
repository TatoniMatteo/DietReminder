import { usePermission } from '../../lib/access'
import { t } from '../../lib/i18n'
import { useState, useRef, type FormEvent } from 'react'
import { Leaf, Plus, Check, CalendarDays, Pencil, Trash2, ArrowUpRight } from 'lucide-react'
import { useWorkspace } from '../../app/WorkspaceContext'
import { EmptyState, Modal, ConfirmDialog, Spinner, ErrorNotice } from '../../components/ui'
import { nextId, removeDiet } from '../../lib/workspace'
import type { Diet } from '../../lib/types'
export function DietsPage({ navigate }: { navigate: (route: string) => void }) {
  const { workspace, save, saving: savingRemote, error } = useWorkspace()
  const canWrite = usePermission('DIETS_WRITE')
  const saving = savingRemote || !canWrite
  const s = workspace!.snapshot
  const draftVersion = useRef(workspace!.version)
  function startEdit(value: Diet | 'new') {
    if (!canWrite) return
    draftVersion.current = workspace!.version
    setEditing(value)
  }
  function startDelete(value: Diet) {
    if (!canWrite) return
    draftVersion.current = workspace!.version
    setDeleting(value)
  }
  const [editing, setEditing] = useState<Diet | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Diet | null>(null)
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const name = String(new FormData(e.currentTarget).get('name')).trim()
    const diet: Diet =
      editing === 'new'
        ? {
            id: nextId(s),
            uuid: crypto.randomUUID(),
            name,
            isActive: s.diets.length === 0,
            nextMealWindowMinutes: 90,
            disabledNotificationDays: '',
          }
        : { ...(editing as Diet), name }
    if (
      await save(
        { ...s, diets: [...s.diets.filter((x) => x.id !== diet.id), diet] },
        draftVersion.current,
      )
    )
      setEditing(null)
  }
  return (
    <>
      {!canWrite && (
        <p className="notice" role="status">
          {t('Questa sezione è disponibile in sola lettura con i tuoi permessi.')}
        </p>
      )}
      <div className="page-heading">
        <div>
          <span className="eyebrow">{t('LA BASE DELLE TUE ABITUDINI')}</span>
          <h1>
            {t('I tuoi piani')}
            <span className="heading-dot">.</span>
          </h1>
          <p className="muted">{t('Organizza la settimana a modo tuo.')}</p>
        </div>
        <button className="button primary" onClick={() => startEdit('new')} disabled={saving}>
          <Plus size={19} />
          {t('Nuovo piano')}
        </button>
      </div>
      {s.diets.length === 0 ? (
        <EmptyState
          icon={<Leaf size={30} />}
          title={t('Ogni abitudine inizia da qui')}
          description={t(
            'Dai un nome al tuo piano alimentare e comincia a riempirlo di buone idee.',
          )}
          action={
            <button className="button secondary" onClick={() => startEdit('new')}>
              {t('Crea un piano')}
            </button>
          }
        />
      ) : (
        <div className="diet-grid">
          {s.diets.map((diet, i) => (
            <article key={diet.id} className={`diet-card surface ${diet.isActive ? 'active' : ''}`}>
              <div className="diet-top">
                <span className={`diet-number color-${i % 3}`}>
                  <Leaf size={26} />
                </span>
                {diet.isActive && (
                  <span className="tag">
                    <span className="dot" />
                    {t('Piano attivo')}
                  </span>
                )}
                <div className="diet-tools">
                  <button
                    className="icon-button"
                    aria-label={t('Modifica {0}', { '0': diet.name })}
                    onClick={() => startEdit(diet)}
                  >
                    <Pencil size={17} />
                  </button>
                  <button
                    className="icon-button"
                    aria-label={t('Rimuovi {0}', { '0': diet.name })}
                    onClick={() => startDelete(diet)}
                  >
                    <Trash2 size={17} />
                  </button>
                </div>
              </div>
              <h2>{diet.name}</h2>
              <p className="muted">
                <CalendarDays size={16} /> {s.meals.filter((x) => x.dietId === diet.id).length}{' '}
                {t('pasti ·')}{' '}
                {new Set(s.meals.filter((x) => x.dietId === diet.id).map((x) => x.dayOfWeek)).size}{' '}
                {t('giorni organizzati')}
              </p>
              <div className="diet-footer">
                <button className="text-button" onClick={() => navigate(`week?diet=${diet.id}`)}>
                  {t('Vai alla settimana')}
                  <ArrowUpRight size={16} />
                </button>
                <button
                  disabled={diet.isActive || saving}
                  className="button secondary small"
                  onClick={() => {
                    void save({
                      ...s,
                      diets: s.diets.map((x) => ({ ...x, isActive: x.id === diet.id })),
                    })
                  }}
                >
                  {diet.isActive ? (
                    <>
                      <Check size={15} />
                      {t('In uso')}
                    </>
                  ) : (
                    t('Usa questo piano')
                  )}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
      {editing && (
        <Modal
          title={editing === 'new' ? t('Un nuovo inizio') : t('Modifica il piano')}
          onClose={() => setEditing(null)}
        >
          <form
            onSubmit={(e) => {
              void submit(e)
            }}
          >
            {error && <ErrorNotice message={error} />}
            <label>
              {t('Nome del piano')}
              <input
                name="name"
                autoFocus
                defaultValue={editing === 'new' ? '' : editing.name}
                placeholder={t('La mia settimana mediterranea')}
                required
                maxLength={500}
              />
            </label>
            <p className="muted small">
              {t('Pasti e ingredienti saranno disponibili anche su Android.')}
            </p>
            <div className="modal-actions">
              <button className="button secondary" type="button" onClick={() => setEditing(null)}>
                {t('Annulla')}
              </button>
              <button className="button primary" disabled={saving}>
                {saving ? <Spinner /> : t('Salva piano')}
              </button>
            </div>
          </form>
        </Modal>
      )}
      {deleting && (
        <ConfirmDialog
          title={t('Rimuovere questo piano?')}
          description={t(
            '“{0}” e tutti i suoi pasti e ingredienti verranno rimossi anche dagli altri dispositivi. Le liste della spesa saranno conservate.',
            { '0': deleting.name },
          )}
          actionLabel={t('Rimuovi piano')}
          busy={saving}
          error={error}
          onClose={() => setDeleting(null)}
          onConfirm={() => {
            void save(removeDiet(s, deleting.id), draftVersion.current).then((ok) => {
              if (ok) setDeleting(null)
            })
          }}
        />
      )}
    </>
  )
}
