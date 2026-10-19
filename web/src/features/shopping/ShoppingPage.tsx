import { Select } from '../../components/Select'
import { usePermission } from '../../lib/access'
import { t } from '../../lib/i18n'
import { useEffect, useState, useRef, type FormEvent } from 'react'
import { ShoppingBasket, Plus, Trash2, Check, ListChecks, Bell, Pencil } from 'lucide-react'
import { EmptyState, Modal, ConfirmDialog, Spinner, ErrorNotice } from '../../components/ui'
import { useWorkspace } from '../../app/WorkspaceContext'
import {
  nextId,
  unitNames,
  dayNames,
  formatQuantity,
  setShoppingBought,
  timeLabel,
} from '../../lib/workspace'
import { shoppingEntries, foodCategory, type ShoppingEntry } from '../../lib/shopping'
import type { FoodCategory, Unit } from '../../lib/types'
import { api } from '../../lib/api'

export function ShoppingPage() {
  const { workspace, save, saving: savingRemote, error } = useWorkspace()
  const canWrite = usePermission('SHOPPING_WRITE')
  const saving = savingRemote || !canWrite
  const s = workspace!.snapshot
  const draftVersion = useRef(workspace!.version)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedDay, setSelectedDay] = useState(-1)
  const [creating, setCreating] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [reminders, setReminders] = useState(false)
  const [editing, setEditing] = useState<ShoppingEntry | null>(null)
  const [catalog, setCatalog] = useState<FoodCategory[]>([])
  const [catalogError, setCatalogError] = useState('')
  const [newName, setNewName] = useState('')
  const list = s.lists.find((x) => x.id === selectedId) ?? s.lists[0]
  const entries = list ? shoppingEntries(s, list.id, catalog) : []
  const shown = entries.filter(
    (row) => selectedDay === -1 || (row.purchaseDay ?? 0) === selectedDay,
  )
  const days = [...new Set(shown.map((row) => row.purchaseDay ?? 0))].sort((a, b) => a - b)
  const bought = entries.filter((row) => row.bought).length
  useEffect(() => {
    void api<FoodCategory[]>('/api/app/shopping-categories')
      .then(setCatalog)
      .catch(() =>
        setCatalogError(
          t('Categorie automatiche non disponibili. Ricarica la pagina per riprovare.'),
        ),
      )
  }, [])
  function open(action: () => void) {
    if (!canWrite) return
    draftVersion.current = workspace!.version
    action()
  }
  async function createList(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const data = new FormData(e.currentTarget)
    const id = nextId(s)
    if (
      await save(
        {
          ...s,
          lists: [
            ...s.lists,
            {
              id,
              name: String(data.get('name')).trim(),
              createdAt: Date.now(),
              dietId: data.get('dietId') ? Number(data.get('dietId')) : null,
              remindersEnabled: false,
              reminderTimeMinutes: 1080,
            },
          ],
        },
        draftVersion.current,
      )
    ) {
      setSelectedId(id)
      setCreating(false)
    }
  }
  async function addItem(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!list || !newName.trim()) return
    const form = e.currentTarget,
      data = new FormData(form)
    const needed = data.get('needed') ? Number(data.get('needed')) : null
    const purchase = data.get('purchase') ? Number(data.get('purchase')) : null
    if (
      await save({
        ...s,
        items: [
          ...s.items,
          {
            id: nextId(s),
            shoppingListId: list.id,
            name: newName.trim(),
            amount: String(data.get('amount')),
            unit: data.get('unit') as Unit,
            isBought: false,
            isCustom: true,
            isFresh: false,
            dayOfWeek: needed,
            purchaseDayOfWeek: purchase,
            category: null,
          },
        ],
      })
    ) {
      form.reset()
      setNewName('')
    }
  }
  async function updateItem(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!editing) return
    const data = new FormData(e.currentTarget)
    const category = String(data.get('category') ?? '').trim() || null
    const needed = data.get('needed') ? Number(data.get('needed')) : null
    const purchase = data.get('purchase') ? Number(data.get('purchase')) : null
    const amount = String(data.get('amount')),
      unit = data.get('unit') as Unit
    const items = s.items.map((item) =>
      item.id === editing.item.id
        ? {
            ...item,
            name: String(data.get('name')).trim(),
            category,
            ...(!editing.day
              ? { amount, unit, dayOfWeek: needed, purchaseDayOfWeek: purchase }
              : {}),
          }
        : item,
    )
    const days = s.days.map((day) =>
      day.id === editing.day?.id ? { ...day, amount, unit, purchaseDayOfWeek: purchase } : day,
    )
    if (await save({ ...s, items, days }, draftVersion.current)) setEditing(null)
  }
  if (editing)
    return (
      <section className="dedicated-editor" aria-label={t('Organizza alimento')}>
        <div className="page-heading">
          <div>
            <button className="text-button" disabled={saving} onClick={() => setEditing(null)}>
              {t('Torna alla lista')}
            </button>
            <h1>{t('Organizza alimento')}</h1>
          </div>
        </div>
        <form
          onSubmit={(e) => {
            void updateItem(e)
          }}
        >
          {error && <ErrorNotice message={error} />}
          <label>
            {t('Nome alimento')}
            <input name="name" required maxLength={500} defaultValue={editing.item.name} />
          </label>
          <div className="form-row">
            <label>
              {t('Quantità')}
              <input
                name="amount"
                maxLength={200}
                defaultValue={editing.day?.amount ?? editing.item.amount}
              />
            </label>
            <label>
              {t('Unità')}
              <Select
                aria-label={t('Unità')}
                name="unit"
                defaultValue={editing.day?.unit ?? editing.item.unit}
              >
                {Object.entries(unitNames).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value || t('Altro')}
                  </option>
                ))}
              </Select>
            </label>
          </div>
          <label>
            {t('Categoria')}
            <input
              name="category"
              aria-label={t('Categoria')}
              list="shopping-categories"
              placeholder={t('Automatica · {0}', {
                '0': foodCategory(editing.item.name, null, catalog),
              })}
              defaultValue={editing.item.category ?? ''}
              maxLength={80}
            />
            <small>
              {t('Lascia vuoto per la scelta automatica o scrivi una categoria personalizzata.')}
            </small>
          </label>
          <datalist id="shopping-categories">
            {catalog.map((c) => (
              <option key={c.name} value={c.name} />
            ))}
          </datalist>
          {editing.day ? (
            <p className="muted small">
              {t('Serve')} {dayNames[editing.day.dayOfWeek - 1]}{' '}
              {t('. Lo spostamento riguarda solo l’acquisto.')}
            </p>
          ) : (
            <DaySelect
              label={t('Giorno di utilizzo')}
              name="needed"
              value={editing.item.dayOfWeek}
            />
          )}
          <DaySelect
            label={t('Giorno d’acquisto')}
            name="purchase"
            value={editing.day?.purchaseDayOfWeek ?? editing.item.purchaseDayOfWeek}
            fallback={t('Come il giorno di utilizzo')}
          />
          <Actions saving={saving} close={() => setEditing(null)} label={t('Salva alimento')} />
        </form>
      </section>
    )
  return (
    <>
      {!canWrite && (
        <p className="notice" role="status">
          {t('Questa sezione è disponibile in sola lettura con i tuoi permessi.')}
        </p>
      )}
      <div className="page-heading">
        <div>
          <span className="eyebrow">{t('IL NECESSARIO, AL MOMENTO GIUSTO')}</span>
          <h1>
            {t('La tua spesa')}
            <span className="heading-dot">.</span>
          </h1>
          <p className="muted">{t('Organizzata per giorno e per reparto.')}</p>
        </div>
        <button
          className="button primary"
          disabled={saving}
          onClick={() => open(() => setCreating(true))}
        >
          <Plus size={19} />
          {t('Nuova lista')}
        </button>
      </div>
      {catalogError && <ErrorNotice message={catalogError} />}
      {!list ? (
        <EmptyState
          icon={<ShoppingBasket size={32} />}
          title={t('La prossima spesa comincia qui')}
          description={t(
            'Crea una lista: potrai scegliere quando acquistare gli alimenti e ritrovarli per categoria.',
          )}
          action={
            <button className="button secondary" onClick={() => open(() => setCreating(true))}>
              {t('Crea la prima lista')}
            </button>
          }
        />
      ) : (
        <div className="shopping-layout">
          <aside className="list-sidebar">
            <span className="eyebrow">{t('LE TUE LISTE')}</span>
            {s.lists.map((x) => (
              <button
                key={x.id}
                className={`list-choice ${list.id === x.id ? 'selected' : ''}`}
                onClick={() => {
                  setSelectedId(x.id)
                  setSelectedDay(-1)
                }}
              >
                <ListChecks size={20} />
                <span>
                  {x.name}
                  <small>
                    {shoppingEntries(s, x.id, catalog).filter((i) => !i.bought).length}{' '}
                    {t('da acquistare')}
                  </small>
                </span>
              </button>
            ))}
          </aside>
          <section className="surface shopping-panel">
            <div className="section-heading">
              <div>
                <h2>{list.name}</h2>
                {list.dietId && (
                  <p className="muted small">{s.diets.find((d) => d.id === list.dietId)?.name}</p>
                )}
                <span className="muted small">
                  {bought} {t('di')} {entries.length} {t('acquisti completati')}
                </span>
              </div>
              <div className="shopping-tools">
                <button
                  className="icon-button"
                  aria-label={t('Promemoria spesa')}
                  onClick={() => open(() => setReminders(true))}
                >
                  <Bell size={18} />
                </button>
                <button
                  className="icon-button"
                  aria-label={t('Rimuovi lista')}
                  onClick={() => open(() => setDeleting(true))}
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
            <div className="progress-track">
              <div style={{ width: `${entries.length ? (bought / entries.length) * 100 : 0}%` }} />
            </div>
            <p className="muted small shopping-explainer">
              {t(
                'I giorni indicano quando fai la spesa. Il giorno di utilizzo resta legato al pasto.',
              )}
            </p>
            <div className="shopping-day-tabs" role="group" aria-label={t('Giorni di acquisto')}>
              {[
                [-1, t('Tutti')],
                [0, t('Senza giorno')],
                ...dayNames.map((name, i) => [i + 1, name]),
              ].map(([value, title]) => (
                <button
                  key={value}
                  className={`button small ${selectedDay === value ? 'primary' : 'secondary'}`}
                  aria-pressed={selectedDay === value}
                  onClick={() => setSelectedDay(Number(value))}
                >
                  {title}
                </button>
              ))}
            </div>
            {shown.length === 0 && (
              <p className="muted shopping-empty">
                {entries.length
                  ? t('Nessun acquisto per questo giorno.')
                  : t('Aggiungi il primo alimento qui sotto.')}
              </p>
            )}
            {days.map((day) => (
              <section
                key={day}
                className="shopping-day-section"
                aria-label={
                  day ? t('Acquisti di {0}', { '0': dayNames[day - 1] }) : t('Senza giorno')
                }
              >
                <h3>
                  {day
                    ? t('Acquisti di {0}', { '0': dayNames[day - 1] })
                    : t('Senza giorno · dispensa')}
                </h3>
                {[
                  ...new Set(
                    shown
                      .filter((row) => (row.purchaseDay ?? 0) === day)
                      .map((row) => row.category),
                  ),
                ]
                  .sort()
                  .map((category) => (
                    <div key={category} className="shopping-category">
                      <h4>{t(category)}</h4>
                      {shown
                        .filter(
                          (row) => (row.purchaseDay ?? 0) === day && row.category === category,
                        )
                        .sort(
                          (a, b) =>
                            Number(a.bought) - Number(b.bought) ||
                            a.item.name.localeCompare(b.item.name, 'it'),
                        )
                        .map((row) => (
                          <div
                            className={`shopping-row ${row.bought ? 'bought' : ''}`}
                            key={row.key}
                          >
                            <button
                              aria-label={`${row.bought ? t('Da acquistare') : t('Acquistato')}: ${row.item.name}${row.day ? `, ${dayNames[row.day.dayOfWeek - 1]}` : ''}`}
                              aria-pressed={row.bought}
                              className="check-button"
                              disabled={saving}
                              onClick={() => {
                                void save(
                                  setShoppingBought(s, row.item.id, !row.bought, row.day?.id),
                                )
                              }}
                            >
                              {row.bought && <Check size={16} />}
                            </button>
                            <span>
                              {row.item.name}
                              <small>
                                {formatQuantity(
                                  row.day?.amount ?? row.item.amount,
                                  row.day?.unit ?? row.item.unit,
                                )}
                                {row.neededDay
                                  ? t(' · Serve {0}', {
                                      '0': dayNames[row.neededDay - 1].toLowerCase(),
                                    })
                                  : ''}
                              </small>
                            </span>
                            <button
                              className="icon-button"
                              aria-label={t('Organizza {0}{1}', {
                                '0': row.item.name,
                                '1': row.day ? `, ${dayNames[row.day.dayOfWeek - 1]}` : '',
                              })}
                              disabled={saving}
                              onClick={() => open(() => setEditing(row))}
                            >
                              <Pencil size={16} />
                            </button>
                            <button
                              className="icon-button"
                              aria-label={t('Rimuovi {0}', { '0': row.item.name })}
                              disabled={saving}
                              onClick={() => {
                                void save({
                                  ...s,
                                  items: s.items.filter((x) => x.id !== row.item.id),
                                  days: s.days.filter((x) => x.shoppingListItemId !== row.item.id),
                                })
                              }}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        ))}
                    </div>
                  ))}
              </section>
            ))}
            <form
              className="shopping-add-planned"
              onSubmit={(e) => {
                void addItem(e)
              }}
            >
              <label>
                {t('Alimento')}
                <input
                  name="name"
                  aria-label={t('Nuovo ingrediente')}
                  placeholder={t('Es. Pomodori')}
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  required
                  maxLength={500}
                />
              </label>
              <div className="form-row">
                <label>
                  {t('Quantità')}
                  <input
                    name="amount"
                    aria-label={t('Quantità')}
                    placeholder={t('Qtà')}
                    maxLength={200}
                  />
                </label>
                <label>
                  {t('Unità')}
                  <Select name="unit" aria-label={t('Unità')}>
                    {Object.entries(unitNames).map(([key, value]) => (
                      <option key={key} value={key}>
                        {value || t('Altro')}
                      </option>
                    ))}
                  </Select>
                </label>
              </div>
              <div className="form-row">
                <DaySelect label={t('Giorno di utilizzo')} name="needed" />
                <DaySelect
                  label={t('Giorno d’acquisto')}
                  name="purchase"
                  fallback={t('Come il giorno di utilizzo')}
                />
              </div>
              <div className="section-heading">
                <span className="muted small">
                  {t('Categoria automatica:')} {foodCategory(newName, null, catalog)}
                </span>
                <button
                  className="button primary"
                  disabled={saving || !newName.trim()}
                  aria-label={t('Aggiungi ingrediente')}
                >
                  {saving ? (
                    <Spinner />
                  ) : (
                    <>
                      <Plus size={18} />
                      {t('Aggiungi')}
                    </>
                  )}
                </button>
              </div>
            </form>
            <p className="muted small">
              {list.remindersEnabled
                ? t('Promemoria Android attivi · verso le {0}', {
                    '0': timeLabel(list.reminderTimeMinutes ?? 1080),
                  })
                : t('Promemoria disattivati')}
            </p>
          </section>
        </div>
      )}
      {creating && (
        <Modal title={t('Una nuova lista')} onClose={() => setCreating(false)}>
          <form
            onSubmit={(e) => {
              void createList(e)
            }}
          >
            {error && <ErrorNotice message={error} />}
            <label>
              {t('Nome della lista')}
              <input
                name="name"
                placeholder={t('La spesa della settimana')}
                required
                maxLength={500}
                autoFocus
              />
            </label>
            <label>
              {t('Piano associato')}
              <Select name="dietId" aria-label={t('Piano associato')}>
                <option value="">{t('Nessun piano')}</option>
                {s.diets.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </Select>
            </label>
            <Actions saving={saving} close={() => setCreating(false)} label={t('Crea lista')} />
          </form>
        </Modal>
      )}
      {reminders && list && (
        <Modal title={t('Promemoria della spesa')} onClose={() => setReminders(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              const data = new FormData(e.currentTarget)
              const [h, m] = String(data.get('time')).split(':').map(Number)
              void save(
                {
                  ...s,
                  lists: s.lists.map((x) =>
                    x.id === list.id
                      ? {
                          ...x,
                          remindersEnabled: data.get('enabled') === 'on',
                          reminderTimeMinutes: h * 60 + m,
                        }
                      : x,
                  ),
                },
                draftVersion.current,
              ).then((ok) => {
                if (ok) setReminders(false)
              })
            }}
          >
            {error && <ErrorNotice message={error} />}
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="enabled"
                defaultChecked={list.remindersEnabled ?? false}
              />{' '}
              {t('Ricordami gli alimenti per domani')}
            </label>
            <label>
              {t('Orario indicativo')}
              <input
                type="time"
                name="time"
                required
                defaultValue={timeLabel(list.reminderTimeMinutes ?? 1080)}
              />
            </label>
            <p className="muted small">
              {t(
                'Ricevi il promemoria su Android, anche ad app chiusa, per gli alimenti che servono il giorno dopo e non risultano acquistati. Consenti le notifiche sul telefono. Gli alimenti senza giorno utilizzano i pasti del piano associato, se presenti.',
              )}
            </p>
            <Actions
              saving={saving}
              close={() => setReminders(false)}
              label={t('Salva promemoria')}
            />
          </form>
        </Modal>
      )}
      {deleting && list && (
        <ConfirmDialog
          title={t('Rimuovere la lista?')}
          description={t('La lista “{0}” e i suoi alimenti verranno rimossi anche da Android.', {
            '0': list.name,
          })}
          actionLabel={t('Rimuovi lista')}
          busy={saving}
          error={error}
          onClose={() => setDeleting(false)}
          onConfirm={() => {
            const ids = new Set(
              s.items.filter((x) => x.shoppingListId === list.id).map((x) => x.id),
            )
            void save(
              {
                ...s,
                lists: s.lists.filter((x) => x.id !== list.id),
                items: s.items.filter((x) => !ids.has(x.id)),
                days: s.days.filter((x) => !ids.has(x.shoppingListItemId)),
              },
              draftVersion.current,
            ).then((ok) => {
              if (ok) setDeleting(false)
            })
          }}
        />
      )}
    </>
  )
}
function DaySelect({
  label,
  name,
  value,
  fallback = t('Non assegnato'),
}: {
  label: string
  name: string
  value?: number | null
  fallback?: string
}) {
  return (
    <label>
      {label}
      <Select name={name} aria-label={label} defaultValue={value ?? ''}>
        <option value="">{fallback}</option>
        {dayNames.map((day, i) => (
          <option key={day} value={i + 1}>
            {day}
          </option>
        ))}
      </Select>
    </label>
  )
}
function Actions({ saving, close, label }: { saving: boolean; close: () => void; label: string }) {
  return (
    <div className="modal-actions">
      <button className="button secondary" type="button" disabled={saving} onClick={close}>
        {t('Annulla')}
      </button>
      <button className="button primary" disabled={saving}>
        {saving ? <Spinner /> : label}
      </button>
    </div>
  )
}
