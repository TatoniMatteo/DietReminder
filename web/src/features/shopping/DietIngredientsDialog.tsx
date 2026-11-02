import { alternativeKey } from '../../lib/alternatives'
import { dayNames } from '../../lib/workspace'
import { useState, useRef } from 'react'
import { Modal, ErrorNotice, Spinner } from '../../components/ui'
import { useWorkspace } from '../../app/WorkspaceContext'
import {
  dietIngredients,
  freshByDefault,
  aggregateQuantities,
  addDietShopping,
} from '../../lib/diet-shopping'
import { formatQuantity } from '../../lib/workspace'
import { t } from '../../lib/i18n'
import type { Snapshot } from '../../lib/types'

function IngredientSelection({
  snapshot,
  dietId,
  listId,
  version,
  onClose,
  onSaved,
  selectedCourseIds,
}: {
  selectedCourseIds?: number[]
  snapshot: Snapshot
  dietId: number
  listId: number
  version: number
  onClose: () => void
  onSaved: () => void
}) {
  const { save, saving, error } = useWorkspace()
  const base = useRef(snapshot).current
  const ingredients = useRef(dietIngredients(base, dietId, selectedCourseIds)).current
  const [choices, setChoices] = useState(() =>
    ingredients.map((i) => ({
      name: i.name,
      selected: true,
      fresh: freshByDefault(i.name),
      days: [...new Set(i.occurrences.map((o) => o.day))],
    })),
  )
  const selected = choices.filter((c) => c.selected && (!c.fresh || c.days.length > 0))
  return (
    <Modal
      title={t('Ingredienti dalla dieta')}
      onClose={() => {
        if (!saving) onClose()
      }}
    >
      <p className="muted">
        {t(
          'Scegli gli ingredienti. Per i freschi puoi selezionare i giorni di utilizzo; gli altri vengono sommati per la settimana.',
        )}
      </p>
      {error && <ErrorNotice message={error} />}
      {ingredients.length === 0 && <p>{t('Questa dieta non contiene ingredienti.')}</p>}
      <div className="diet-import-ingredients">
        {ingredients.map((i, index) => {
          const c = choices[index]
          const patch = (value: Partial<typeof c>) =>
            setChoices((prev) => prev.map((c, n) => (n === index ? { ...c, ...value } : c)))
          return (
            <fieldset key={i.name} disabled={saving} className="import-ingredient">
              <legend>{i.name}</legend>
              <label className="import-choice">
                <input
                  type="checkbox"
                  checked={c.selected}
                  onChange={(e) => patch({ selected: e.target.checked })}
                />
                {t('Includi ingrediente')}
              </label>
              <p className="muted small">
                {aggregateQuantities(i.occurrences)
                  .map((q) => formatQuantity(q.amount, q.unit))
                  .join(' + ')}
              </p>
              <label className="import-choice">
                <input
                  type="checkbox"
                  checked={c.fresh}
                  disabled={!c.selected}
                  onChange={(e) => patch({ fresh: e.target.checked })}
                />
                {t('Fresco · acquisti per giorno')}
              </label>
              {c.fresh && (
                <div className="import-days">
                  {[...new Set(i.occurrences.map((o) => o.day))].sort().map((day) => (
                    <label className="import-choice" key={day}>
                      <input
                        type="checkbox"
                        disabled={!c.selected}
                        checked={c.days.includes(day)}
                        onChange={(e) =>
                          patch({
                            days: e.target.checked
                              ? [...c.days, day]
                              : c.days.filter((d) => d !== day),
                          })
                        }
                      />
                      {dayNames[day - 1]}
                    </label>
                  ))}
                </div>
              )}
            </fieldset>
          )
        })}
      </div>
      <div className="modal-actions">
        <button className="button secondary" disabled={saving} onClick={onClose}>
          {t('Annulla')}
        </button>
        <button
          className="button primary"
          disabled={saving || !selected.length}
          onClick={() => {
            void save(
              addDietShopping(base, listId, dietId, selected, selectedCourseIds),
              version,
            ).then((ok) => {
              if (ok) onSaved()
            })
          }}
        >
          {saving ? <Spinner /> : t('Importa ingredienti')}
        </button>
      </div>
    </Modal>
  )
}

export function DietIngredientsDialog(props: {
  snapshot: Snapshot
  dietId: number
  listId: number
  version: number
  onClose: () => void
  onSaved: () => void
}) {
  const base = useRef(props.snapshot).current
  const meals = base.meals.filter((m) => m.dietId === props.dietId)
  const courses = base.courses.filter(
    (c) => c.alternativeGroup && meals.some((m) => m.id === c.mealId),
  )
  const keys = [...new Set(courses.map(alternativeKey))]
  const [selected, setSelected] = useState(() =>
    keys.map((key) => courses.find((c) => alternativeKey(c) === key)!.id),
  )
  const [ready, setReady] = useState(keys.length === 0)
  if (ready) return <IngredientSelection {...props} selectedCourseIds={selected} />
  return (
    <Modal title={t('Alternative per questa spesa')} onClose={props.onClose}>
      <p className="muted">
        {t('Scegli cosa acquistare questa volta. Le scelte non modificano il piano alimentare.')}
      </p>
      {keys.map((key, index) => {
        const options = courses.filter((c) => alternativeKey(c) === key)
        const meal = meals.find((m) => m.id === options[0].mealId)!
        return (
          <fieldset className="import-ingredient" key={key}>
            <legend>{dayNames[meal.dayOfWeek - 1]}</legend>
            {options.map((c) => (
              <label className="import-choice" key={c.id}>
                <input
                  type="radio"
                  name={key}
                  checked={selected[index] === c.id}
                  onChange={() =>
                    setSelected((prev) => prev.map((id, i) => (i === index ? c.id : id)))
                  }
                />
                {c.name}
              </label>
            ))}
          </fieldset>
        )
      })}
      <div className="modal-actions">
        <button className="button secondary" onClick={props.onClose}>
          {t('Annulla')}
        </button>
        <button className="button primary" onClick={() => setReady(true)}>
          {t('Continua')}
        </button>
      </div>
    </Modal>
  )
}
