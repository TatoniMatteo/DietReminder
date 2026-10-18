import { t } from '../../lib/i18n'
import { useRef, useState, type FormEvent } from 'react'
import { Plus, Trash2, ArrowUp, ArrowDown } from 'lucide-react'
import { ErrorNotice, Spinner } from '../../components/ui'
import { useWorkspace } from '../../app/WorkspaceContext'
import { mealNames, nextId, timeLabel, unitNames } from '../../lib/workspace'
import type { Food, Meal, MealType, Unit, Course } from '../../lib/types'

export function MealEditor({
  meal,
  dietId,
  baseVersion,
  day,
  onClose,
}: {
  meal?: Meal
  baseVersion: number
  dietId: number
  day: number
  onClose: () => void
}) {
  const { workspace, save, saving, error: saveError } = useWorkspace()
  const snapshot = workspace!.snapshot
  const id = useRef(meal?.id ?? nextId(snapshot)).current
  const counter = useRef(Math.max(nextId(snapshot), id + 1))
  const [type, setType] = useState<MealType>(meal?.type ?? 'BREAKFAST')
  const [time, setTime] = useState(timeLabel(meal?.timeMinutes ?? 480))
  const [description, setDescription] = useState(meal?.description ?? '')
  const [customLabel, setCustomLabel] = useState(meal?.customTypeLabel ?? '')
  const [courses, setCourses] = useState(
    snapshot.courses.filter((x) => x.mealId === id).sort((a, b) => a.order - b.order),
  )
  const [foods, setFoods] = useState<Food[]>(
    snapshot.foods.filter((x) => courses.some((c) => c.id === x.courseId)),
  )
  const [error, setError] = useState('')
  const [notifications, setNotifications] = useState(meal?.isNotificationEnabled ?? true)
  function addCourse() {
    setCourses([...courses, { id: counter.current++, mealId: id, name: '', order: courses.length }])
  }
  function moveCourse(index: number, delta: number) {
    const reordered = [...courses]
    ;[reordered[index], reordered[index + delta]] = [reordered[index + delta], reordered[index]]
    setCourses(reordered.map((c, order) => ({ ...c, order })))
  }
  function addFood(target?: Course) {
    let course = target ?? courses[0]
    if (!course) {
      course = { id: counter.current++, mealId: id, name: '', order: 0 }
      setCourses([course])
    }
    setFoods([
      ...foods,
      {
        id: counter.current++,
        courseId: course.id,
        name: '',
        amount: '',
        unit: 'GRAMS',
        order:
          Math.max(-1, ...foods.filter((f) => f.courseId === course.id).map((f) => f.order)) + 1,
      },
    ])
  }
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (foods.some((x) => !x.name.trim())) {
      setError(t('Inserisci il nome di ogni ingrediente.'))
      return
    }
    const oldCourses = new Set(snapshot.courses.filter((x) => x.mealId === id).map((x) => x.id))
    const [h, m] = time.split(':').map(Number)
    const updated: Meal = {
      id,
      dietId,
      dayOfWeek: meal?.dayOfWeek ?? day,
      type,
      timeMinutes: h * 60 + m,
      description,
      customTypeLabel: type === 'OTHER' ? customLabel : null,
      isNotificationEnabled: notifications,
    }
    if (
      await save(
        {
          ...snapshot,
          meals: [...snapshot.meals.filter((x) => x.id !== id), updated],
          courses: [
            ...snapshot.courses.filter((x) => x.mealId !== id),
            ...courses.map((c, order) => ({ ...c, name: c.name.trim(), order })),
          ],
          foods: [...snapshot.foods.filter((x) => !oldCourses.has(x.courseId)), ...foods],
        },
        baseVersion,
      )
    )
      onClose()
  }
  return (
    <section className="dedicated-editor" aria-label={t('Editor pasto')}>
      <div className="page-heading">
        <div>
          <button type="button" className="text-button" onClick={onClose} disabled={saving}>
            {t('Torna alla settimana')}
          </button>
          <h1>{meal ? t('Un po’ di cura per questo pasto') : t('Aggiungi un pasto')}</h1>
        </div>
      </div>
      <form
        onSubmit={(e) => {
          void submit(e)
        }}
      >
        {(error || saveError) && <ErrorNotice message={error || saveError} />}
        <div className="form-row">
          <label>
            {t('Pasto')}
            <select
              aria-label={t('Pasto')}
              value={type}
              onChange={(e) => setType(e.target.value as MealType)}
            >
              {Object.entries(mealNames).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t('Orario')}
            <input type="time" value={time} onChange={(e) => setTime(e.target.value)} required />
          </label>
        </div>
        {type === 'OTHER' && (
          <label>
            {t('Nome del pasto')}
            <input
              value={customLabel}
              onChange={(e) => setCustomLabel(e.target.value)}
              maxLength={500}
            />
          </label>
        )}
        <label>
          {t('Una nota per te')}
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t('Come ti va di prepararlo?')}
            maxLength={10000}
            rows={2}
          />
        </label>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={notifications}
            onChange={(e) => setNotifications(e.target.checked)}
          />{' '}
          {t('Promemoria per questo pasto')}
        </label>
        <div className="section-heading">
          <h3>{t('Portate e ingredienti')}</h3>
          <button type="button" className="text-button" onClick={addCourse}>
            <Plus size={17} />
            {t('Aggiungi portata')}
          </button>
        </div>
        <p className="muted small">
          {t('Ogni pasto contiene portate; ogni portata ha i propri ingredienti.')}
        </p>
        {courses.length === 0 && (
          <button type="button" className="text-button" onClick={() => addFood()}>
            <Plus size={17} />
            {t('Aggiungi')}
          </button>
        )}
        {courses.map((course, courseIndex) => (
          <section
            className="course-editor"
            key={course.id}
            aria-label={t('Portata {0}', { '0': courseIndex + 1 })}
          >
            <div className="course-heading">
              <label>
                {t('Nome della portata')} {courseIndex + 1}
                <input
                  value={course.name}
                  placeholder={t('Es. Pasta al pomodoro')}
                  maxLength={500}
                  onChange={(e) =>
                    setCourses(
                      courses.map((c) => (c.id === course.id ? { ...c, name: e.target.value } : c)),
                    )
                  }
                />
              </label>
              <button
                type="button"
                className="icon-button"
                aria-label={t('Sposta su portata {0}', { '0': courseIndex + 1 })}
                disabled={courseIndex === 0}
                onClick={() => moveCourse(courseIndex, -1)}
              >
                <ArrowUp size={17} />
              </button>
              <button
                type="button"
                className="icon-button"
                aria-label={t('Sposta giù portata {0}', { '0': courseIndex + 1 })}
                disabled={courseIndex === courses.length - 1}
                onClick={() => moveCourse(courseIndex, 1)}
              >
                <ArrowDown size={17} />
              </button>
              <button
                type="button"
                className="icon-button"
                aria-label={t('Rimuovi portata {0}', { '0': courseIndex + 1 })}
                onClick={() => {
                  setCourses(courses.filter((c) => c.id !== course.id))
                  setFoods(foods.filter((f) => f.courseId !== course.id))
                }}
              >
                <Trash2 size={17} />
              </button>
            </div>
            {foods
              .filter((f) => f.courseId === course.id)
              .sort((a, b) => a.order - b.order)
              .map((food) => {
                const index = foods.indexOf(food)
                return (
                  <div className="ingredient-editor" key={food.id}>
                    <input
                      aria-label={t('Ingrediente {0}', { '0': index + 1 })}
                      placeholder={t('Ingrediente')}
                      value={food.name}
                      maxLength={500}
                      required
                      onChange={(e) =>
                        setFoods(
                          foods.map((x) => (x.id === food.id ? { ...x, name: e.target.value } : x)),
                        )
                      }
                    />
                    <input
                      aria-label={t('Quantità {0}', { '0': index + 1 })}
                      placeholder={t('Quantità')}
                      value={food.amount}
                      maxLength={200}
                      onChange={(e) =>
                        setFoods(
                          foods.map((x) =>
                            x.id === food.id ? { ...x, amount: e.target.value } : x,
                          ),
                        )
                      }
                    />
                    <select
                      aria-label={t('Unità {0}', { '0': index + 1 })}
                      value={food.unit}
                      onChange={(e) =>
                        setFoods(
                          foods.map((x) =>
                            x.id === food.id ? { ...x, unit: e.target.value as Unit } : x,
                          ),
                        )
                      }
                    >
                      {Object.entries(unitNames).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label || t('Personalizzata')}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="icon-button"
                      aria-label={t('Rimuovi ingrediente {0}', { '0': index + 1 })}
                      onClick={() => setFoods(foods.filter((x) => x.id !== food.id))}
                    >
                      <Trash2 size={17} />
                    </button>
                  </div>
                )
              })}
            <button type="button" className="text-button" onClick={() => addFood(course)}>
              <Plus size={17} />
              {t('Aggiungi ingrediente alla portata')} {courseIndex + 1}
            </button>
          </section>
        ))}
        <div className="modal-actions">
          <button type="button" className="button secondary" disabled={saving} onClick={onClose}>
            {t('Annulla')}
          </button>
          <button className="button primary" disabled={saving}>
            {saving ? <Spinner /> : t('Salva pasto')}
          </button>
        </div>
      </form>
    </section>
  )
}
