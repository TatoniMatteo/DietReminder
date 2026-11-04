import { shoppingCourses } from './alternatives'
import type { Snapshot, Unit } from './types'
import { nextId } from './workspace'
export interface Occurrence {
  day: number
  amount: string
  unit: Unit
}
export interface DietIngredient {
  name: string
  occurrences: Occurrence[]
}
export interface IngredientChoice {
  name: string
  fresh: boolean
  days: number[]
}
export function dietIngredients(
  s: Snapshot,
  dietId: number,
  selectedCourseIds?: number[],
): DietIngredient[] {
  const meals = new Map(s.meals.filter((m) => m.dietId === dietId).map((m) => [m.id, m]))
  const courses = new Map(
    shoppingCourses(
      s.courses.filter((c) => meals.has(c.mealId)),
      selectedCourseIds,
    ).map((c) => [c.id, c]),
  )
  const groups = new Map<string, DietIngredient>()
  for (const f of s.foods) {
    const c = courses.get(f.courseId)
    if (!c || !f.name.trim()) continue
    const name = f.name.trim(),
      key = name.toLowerCase()
    const group = groups.get(key) ?? {
      name: name[0].toUpperCase() + name.slice(1),
      occurrences: [],
    }
    group.occurrences.push({ day: meals.get(c.mealId)!.dayOfWeek, amount: f.amount, unit: f.unit })
    groups.set(key, group)
  }
  return [...groups.values()].sort((a, b) => a.name.localeCompare(b.name))
}
export function freshByDefault(name: string) {
  return /salmone|pesce|carne|petto|mozzarella|fresco|freschi|ricotta|latte/i.test(name)
}
const mass: Partial<Record<Unit, number>> = { GRAMS: 1, KILOGRAMS: 1000, MILLIGRAMS: 0.001 }
const volume: Partial<Record<Unit, number>> = {
  MILLILITERS: 1,
  CENTILITERS: 10,
  DECILITERS: 100,
  LITERS: 1000,
}
// Keep incompatible quantities separate so no ingredient quantity is silently lost.
export function aggregateQuantities(values: Occurrence[]): { amount: string; unit: Unit }[] {
  const groups = new Map<string, { amount: string; unit: Unit; sum?: number }>()
  for (const v of values) {
    const n = v.amount.trim() ? Number(v.amount.replace(',', '.')) : NaN
    if (!Number.isFinite(n) || ['QB', 'FREE', 'CUSTOM'].includes(v.unit)) {
      const key = `${v.unit}:${v.amount}`
      if (!groups.has(key)) groups.set(key, { amount: v.amount, unit: v.unit })
      continue
    }
    const factor = mass[v.unit] ?? volume[v.unit] ?? 1
    const key = mass[v.unit] ? 'mass' : volume[v.unit] ? 'volume' : v.unit
    const unit = mass[v.unit] ? 'GRAMS' : volume[v.unit] ? 'MILLILITERS' : v.unit
    const prev = groups.get(key)
    groups.set(key, { amount: '', unit, sum: (prev?.sum ?? 0) + n * factor })
  }
  return [...groups.values()].map((v) => {
    if (v.sum === undefined) return { amount: v.amount, unit: v.unit }
    const large = v.sum >= 1000 && ['GRAMS', 'MILLILITERS'].includes(v.unit)
    return {
      amount: String(Number((v.sum / (large ? 1000 : 1)).toFixed(6))),
      unit: large ? (v.unit === 'GRAMS' ? 'KILOGRAMS' : 'LITERS') : v.unit,
    }
  })
}
export function addDietShopping(
  s: Snapshot,
  listId: number,
  dietId: number,
  choices: IngredientChoice[],
  selectedCourseIds?: number[],
): Snapshot {
  let id = nextId(s)
  const result = { ...s, items: [...s.items], days: [...s.days] }
  for (const ingredient of dietIngredients(s, dietId, selectedCourseIds)) {
    const choice = choices.find((c) => c.name === ingredient.name)
    if (!choice) continue
    const occurrences = ingredient.occurrences.filter(
      (o) => !choice.fresh || choice.days.includes(o.day),
    )
    const days = choice.fresh ? [...new Set(occurrences.map((o) => o.day))] : [null]
    // Split incompatible units into distinct items, including fresh daily allocations.
    const quantities = aggregateQuantities(occurrences)
    if (!choice.fresh) {
      for (const q of quantities)
        result.items.push({
          id: id++,
          shoppingListId: listId,
          name: ingredient.name,
          ...q,
          isBought: false,
          isCustom: false,
          isFresh: false,
          dayOfWeek: null,
        })
    } else {
      const byUnit = new Map<Unit, { day: number; amount: string; unit: Unit }[]>()
      for (const day of days)
        for (const q of aggregateQuantities(occurrences.filter((o) => o.day === day))) {
          const rows = byUnit.get(q.unit) ?? []
          rows.push({ ...q, day: day! })
          byUnit.set(q.unit, rows)
        }
      for (const rows of byUnit.values()) {
        const itemId = id++
        result.items.push({
          id: itemId,
          shoppingListId: listId,
          name: ingredient.name,
          amount: '',
          unit: rows[0].unit,
          isBought: false,
          isCustom: false,
          isFresh: true,
          dayOfWeek: null,
        })
        for (const row of rows)
          result.days.push({
            id: id++,
            shoppingListItemId: itemId,
            dayOfWeek: row.day,
            amount: row.amount,
            unit: row.unit,
            isBought: false,
          })
      }
    }
  }
  return result
}
