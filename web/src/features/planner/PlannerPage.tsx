import { Select } from '../../components/Select'
import { usePermission } from '../../lib/access'
import { t, locale } from '../../lib/i18n'
import { useState, useRef } from 'react'
import {
  Plus,
  Utensils,
  Sun,
  Sunrise,
  Moon,
  Cookie,
  ChevronDown,
  CalendarDays,
  Pencil,
  Trash2,
  ArrowUpRight,
  Leaf,
  ShoppingBasket,
} from 'lucide-react'
import { useWorkspace } from '../../app/WorkspaceContext'
import { EmptyState, ConfirmDialog } from '../../components/ui'
import { dayNames, mealNames, timeLabel, formatQuantity } from '../../lib/workspace'
import type { Meal } from '../../lib/types'
import { MealEditor } from './MealEditor'
const mealIcons = {
  BREAKFAST: Sunrise,
  MORNING_SNACK: Cookie,
  LUNCH: Sun,
  AFTERNOON_SNACK: Cookie,
  DINNER: Moon,
  OTHER: Utensils,
}
export function PlannerPage({ navigate }: { navigate: (route: string) => void }) {
  const { workspace, save, saving: savingRemote, error } = useWorkspace()
  const canWrite = usePermission('DIETS_WRITE')
  const saving = savingRemote || !canWrite
  const s = workspace!.snapshot
  const draftVersion = useRef(workspace!.version)
  function startEdit(value: Meal | 'new') {
    if (!canWrite) return
    draftVersion.current = workspace!.version
    setEditing(value)
  }
  function startDelete(value: Meal) {
    if (!canWrite) return
    draftVersion.current = workspace!.version
    setDeleting(value)
  }
  const [day, setDay] = useState(((new Date().getDay() + 6) % 7) + 1)
  const [dietId, setDietId] = useState<number | null>(
    () => Number(new URLSearchParams(location.hash.split('?')[1]).get('diet')) || null,
  )
  const [editing, setEditing] = useState<Meal | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Meal | null>(null)
  const diet = s.diets.find((x) => x.id === dietId) ?? s.diets.find((x) => x.isActive) ?? s.diets[0]
  const meals = s.meals
    .filter((x) => x.dietId === diet?.id && x.dayOfWeek === day)
    .sort((a, b) => a.timeMinutes - b.timeMinutes)
  const monday = new Date()
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7))
  async function deleteMeal() {
    const removed = deleting!
    const courses = new Set(s.courses.filter((c) => c.mealId === removed.id).map((c) => c.id))
    if (
      await save(
        {
          ...s,
          meals: s.meals.filter((x) => x.id !== removed.id),
          courses: s.courses.filter((x) => !courses.has(x.id)),
          foods: s.foods.filter((x) => !courses.has(x.courseId)),
        },
        draftVersion.current,
      )
    )
      setDeleting(null)
  }
  if (editing && diet)
    return (
      <MealEditor
        meal={editing === 'new' ? undefined : editing}
        dietId={diet.id}
        baseVersion={draftVersion.current}
        day={day}
        onClose={() => setEditing(null)}
      />
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
          <span className="eyebrow">{t('UN GIORNO ALLA VOLTA')}</span>
          <h1>
            {t('La tua settimana')}
            <span className="heading-dot">.</span>
          </h1>
          <p className="muted">{t('Un po’ di organizzazione, più spazio per te.')}</p>
        </div>
        <button
          className="button primary"
          disabled={!diet || saving}
          onClick={() => startEdit('new')}
        >
          <Plus size={19} />
          {t('Aggiungi pasto')}
        </button>
      </div>
      <div className="planner-layout">
        <section>
          <div className="week-card surface">
            <div className="section-heading">
              <span className="subheading">
                <CalendarDays size={19} />
                {monday.toLocaleDateString(locale(), { month: 'long', year: 'numeric' })}
              </span>
              <button
                className="text-button"
                onClick={() => setDay(((new Date().getDay() + 6) % 7) + 1)}
              >
                {t('Oggi')}
              </button>
            </div>
            <div className="week-days">
              {dayNames.map((name, i) => {
                const date = new Date(monday)
                date.setDate(monday.getDate() + i)
                return (
                  <button
                    aria-label={name}
                    aria-pressed={day === i + 1}
                    className={day === i + 1 ? 'selected' : ''}
                    key={name}
                    onClick={() => setDay(i + 1)}
                  >
                    <span>{name.slice(0, 3)}</span>
                    <strong>{date.getDate()}</strong>
                    <i
                      className={
                        s.meals.some((m) => m.dietId === diet?.id && m.dayOfWeek === i + 1)
                          ? 'has-meals'
                          : ''
                      }
                    />
                  </button>
                )
              })}
            </div>
          </div>
          <div className="section-heading day-heading">
            <div>
              <h2>{dayNames[day - 1]}</h2>
              <span className="muted small">
                {meals.length} {t('pasti pianificati')}
              </span>
            </div>
            {diet && (
              <Select
                aria-label={t('Piano alimentare')}
                className="plan-select"
                value={diet.id}
                onValueChange={(value) => setDietId(Number(value))}
              >
                {s.diets.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </Select>
            )}
          </div>
          {!diet ? (
            <EmptyState
              icon={<Leaf size={30} />}
              title={t('Una settimana tutta da immaginare')}
              description={t(
                'Crea il tuo primo piano alimentare. Poi aggiungi i pasti, con i tuoi tempi.',
              )}
              action={
                <button className="button primary" onClick={() => navigate('diets')}>
                  {t('Crea il primo piano')}
                  <ArrowUpRight size={17} />
                </button>
              }
            />
          ) : meals.length === 0 ? (
            <EmptyState
              icon={<Utensils size={30} />}
              title={t('Qui c’è spazio per un buon pasto')}
              description={t(
                'Aggiungi la colazione, un pranzo o una piccola pausa. Comincia da quello che preferisci.',
              )}
              action={
                <button className="button secondary" onClick={() => startEdit('new')}>
                  <Plus size={17} />
                  {t('Aggiungi un pasto')}
                </button>
              }
            />
          ) : (
            <div className="meal-list">
              {meals.map((meal) => {
                const Icon = mealIcons[meal.type]
                const courses = s.courses
                  .filter((c) => c.mealId === meal.id)
                  .sort((a, b) => a.order - b.order)
                const foods = s.foods.filter((f) => courses.some((c) => c.id === f.courseId))
                return (
                  <details className={`meal-card surface ${meal.type.toLowerCase()}`} key={meal.id}>
                    <summary>
                      <span className="meal-icon">
                        <Icon size={22} />
                      </span>
                      <span className="meal-title">
                        <strong>{meal.customTypeLabel || mealNames[meal.type]}</strong>
                        <small>
                          {meal.description ||
                            (foods.length
                              ? t('{0} ingredienti, una buona idea', { '0': foods.length })
                              : t('Il tuo momento di pausa'))}
                        </small>
                      </span>
                      <span className="meal-time">{timeLabel(meal.timeMinutes)}</span>
                      <ChevronDown size={18} className="chevron" />
                    </summary>
                    <div className="meal-content">
                      {courses.map((course) => (
                        <div key={course.id}>
                          {course.name && <h4>{course.name}</h4>}
                          {s.foods
                            .filter((x) => x.courseId === course.id)
                            .sort((a, b) => a.order - b.order)
                            .map((food) => (
                              <div className="food-row" key={food.id}>
                                <span>{food.name}</span>
                                <span className="muted">
                                  {formatQuantity(food.amount, food.unit)}
                                </span>
                              </div>
                            ))}
                        </div>
                      ))}
                      {!foods.length && (
                        <p className="muted small">{t('Nessun ingrediente aggiunto.')}</p>
                      )}
                      <div className="meal-actions">
                        <button
                          className="text-button"
                          disabled={saving}
                          onClick={() => startEdit(meal)}
                        >
                          <Pencil size={16} />
                          {t('Modifica')}
                        </button>
                        <button
                          className="text-button danger-text"
                          disabled={saving}
                          onClick={() => startDelete(meal)}
                        >
                          <Trash2 size={16} />
                          {t('Rimuovi')}
                        </button>
                      </div>
                    </div>
                  </details>
                )
              })}
            </div>
          )}
        </section>
        <aside className="planner-aside">
          <div className="surface aside-summary">
            <span className="eyebrow">{t('IL TUO PIANO')}</span>
            <h3>{diet?.name ?? t('Il prossimo passo è tuo')}</h3>
            <div className="summary-row">
              <span>{t('Pasti nella settimana')}</span>
              <strong>{s.meals.filter((x) => x.dietId === diet?.id).length}</strong>
            </div>
            <div className="summary-row">
              <span>{t('Giorni organizzati')}</span>
              <strong>
                {new Set(s.meals.filter((x) => x.dietId === diet?.id).map((x) => x.dayOfWeek)).size}{' '}
                / 7
              </strong>
            </div>
            <button className="text-button full" onClick={() => navigate('diets')}>
              {t('Gestisci i tuoi piani')}
              <ArrowUpRight size={16} />
            </button>
          </div>
          <button className="shopping-shortcut" onClick={() => navigate('shopping')}>
            <ShoppingBasket size={23} />
            <span>
              {t('La spesa, con calma')}
              <small>
                {s.items.filter((x) => !x.isBought).length} {t('ingredienti da acquistare')}
              </small>
            </span>
            <ArrowUpRight size={18} />
          </button>
        </aside>
      </div>
      {deleting && (
        <ConfirmDialog
          title={t('Rimuovere questo pasto?')}
          description={t(
            'Verranno rimossi anche i suoi ingredienti. La modifica verrà sincronizzata con l’app Android.',
          )}
          actionLabel={t('Rimuovi pasto')}
          busy={saving}
          error={error}
          onClose={() => setDeleting(null)}
          onConfirm={() => {
            void deleteMeal()
          }}
        />
      )}
    </>
  )
}
