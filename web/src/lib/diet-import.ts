import { validateAlternatives } from './alternatives'
import { t } from './i18n'
import type { Snapshot, MealType, Unit } from './types'
import { mealNames, unitNames, nextId } from './workspace'

const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
type JsonObject = Record<string, unknown>
export interface ImportedDiet {
  uuid: string | null
  name: string
  nextMealWindowMinutes: number
  disabledNotificationDays: string
  meals: {
    day: number
    type: MealType
    time: string
    description: string
    customTypeLabel: string | null
    isNotificationEnabled: boolean
    courses: {
      name: string
      alternativeGroup: string | null
      description: string
      order: number
      items: { name: string; amount: string; unit: Unit; order: number }[]
    }[]
  }[]
}
function invalid(): never {
  throw new Error(t('JSON della dieta non valido. Controlla il formato e i valori.'))
}
function object(value: unknown): JsonObject {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return invalid()
  return value as JsonObject
}
function text(value: unknown, fallback?: string, max = 500): string {
  if (value === undefined && fallback !== undefined) return fallback
  if (typeof value !== 'string' || value.length > max) return invalid()
  return value
}
function array(value: unknown): unknown[] {
  if (value === undefined) return []
  if (!Array.isArray(value)) return invalid()
  return value
}
function integer(value: unknown, fallback: number, min: number, max: number) {
  const n = value === undefined ? fallback : value
  if (typeof n !== 'number' || !Number.isInteger(n) || n < min || n > max) return invalid()
  return n
}
export function parseDietJson(raw: string): ImportedDiet {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    return invalid()
  }
  const d = object(parsed)
  integer(d.formatVersion, 1, 1, 2)
  const name = text(d.name).trim()
  if (!name) return invalid()
  const diet: ImportedDiet = {
    uuid: d.uuid == null ? null : text(d.uuid),
    name,
    nextMealWindowMinutes: integer(d.nextMealWindowMinutes, 60, 1, 1440),
    disabledNotificationDays: text(d.disabledNotificationDays, ''),
    meals: array(d.meals).map((value) => {
      const m = object(value)
      const day = weekdays.indexOf(String(m.day)) + 1
      const type = text(m.type) as MealType
      const time = text(m.time)
      if (!day || !Object.hasOwn(mealNames, type) || !/^(?:[01]?\d|2[0-3]):[0-5]\d$/.test(time))
        return invalid()
      if (m.isNotificationEnabled !== undefined && typeof m.isNotificationEnabled !== 'boolean')
        return invalid()
      return {
        day,
        type,
        time,
        description: text(m.description, '', 10000),
        customTypeLabel: m.customTypeLabel == null ? null : text(m.customTypeLabel),
        isNotificationEnabled: m.isNotificationEnabled !== false,
        courses: array(m.courses).map((value) => {
          const c = object(value)
          return {
            alternativeGroup:
              c.alternativeGroup == null ? null : text(c.alternativeGroup, undefined, 128),
            description: text(c.description, '', 10000),
            name: text(c.name),
            order: integer(c.order, 0, 0, 2147483647),
            items: array(c.items).map((value) => {
              const f = object(value)
              const name = text(f.name).trim(),
                amount = text(f.amount, '', 200).trim()
              const unit = text(f.unit, 'GRAMS') as Unit
              if (!name || !Object.hasOwn(unitNames, unit)) return invalid()
              if (
                amount
                  ? !Number.isFinite(Number(amount.replace(',', '.'))) ||
                    Number(amount.replace(',', '.')) <= 0
                  : !['QB', 'FREE', 'CUSTOM'].includes(unit)
              )
                return invalid()
              return { name, amount, unit, order: integer(f.order, 0, 0, 2147483647) }
            }),
          }
        }),
      }
    }),
  }
  diet.meals.forEach((m) => validateAlternatives(m.courses))
  return diet
}
export function importDiet(snapshot: Snapshot, data: ImportedDiet, overwrite = false): Snapshot {
  const existing = data.uuid ? snapshot.diets.find((d) => d.uuid === data.uuid) : undefined
  if (existing && !overwrite)
    throw new Error(t('Questa dieta esiste già. Conferma la sostituzione.'))
  let id = nextId(snapshot)
  const dietId = existing?.id ?? id++
  const oldMeals = new Set(snapshot.meals.filter((m) => m.dietId === dietId).map((m) => m.id))
  const oldCourses = new Set(
    snapshot.courses.filter((c) => oldMeals.has(c.mealId)).map((c) => c.id),
  )
  const result: Snapshot = {
    ...snapshot,
    diets: [
      ...snapshot.diets.filter((d) => d.id !== dietId),
      {
        id: dietId,
        uuid: data.uuid || crypto.randomUUID(),
        name: data.name,
        nextMealWindowMinutes: data.nextMealWindowMinutes,
        disabledNotificationDays: data.disabledNotificationDays,
        isActive: existing?.isActive ?? snapshot.diets.length === 0,
      },
    ],
    meals: snapshot.meals.filter((m) => !oldMeals.has(m.id)),
    courses: snapshot.courses.filter((c) => !oldCourses.has(c.id)),
    foods: snapshot.foods.filter((f) => !oldCourses.has(f.courseId)),
  }
  for (const m of data.meals) {
    const mealId = id++
    const [h, min] = m.time.split(':').map(Number)
    result.meals.push({
      id: mealId,
      dietId,
      dayOfWeek: m.day,
      type: m.type,
      timeMinutes: h * 60 + min,
      description: m.description,
      customTypeLabel: m.customTypeLabel,
      isNotificationEnabled: m.isNotificationEnabled,
    })
    for (const c of m.courses) {
      const courseId = id++
      result.courses.push({
        id: courseId,
        mealId,
        name: c.name,
        order: c.order,
        alternativeGroup: c.alternativeGroup,
        description: c.description,
      })
      for (const f of c.items) result.foods.push({ ...f, id: id++, courseId })
    }
  }
  return result
}
