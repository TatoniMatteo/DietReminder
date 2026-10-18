import { t, localizedArray } from './i18n'
import type { Snapshot, Unit, MealType } from './types'
export const emptySnapshot = (): Snapshot => ({
  schemaVersion: 2,
  diets: [],
  meals: [],
  courses: [],
  foods: [],
  lists: [],
  items: [],
  days: [],
})
export function nextId(snapshot: Snapshot): number {
  return (
    Math.max(
      0,
      ...[
        ...snapshot.diets,
        ...snapshot.meals,
        ...snapshot.courses,
        ...snapshot.foods,
        ...snapshot.lists,
        ...snapshot.items,
        ...snapshot.days,
      ].map((x) => x.id),
    ) + 1
  )
}
export function removeDiet(snapshot: Snapshot, id: number): Snapshot {
  const meals = new Set(snapshot.meals.filter((x) => x.dietId === id).map((x) => x.id))
  const courses = new Set(snapshot.courses.filter((x) => meals.has(x.mealId)).map((x) => x.id))
  const remaining = snapshot.diets.filter((x) => x.id !== id)
  const diets =
    snapshot.diets.find((x) => x.id === id)?.isActive &&
    remaining.length &&
    !remaining.some((x) => x.isActive)
      ? remaining.map((x, i) => ({ ...x, isActive: i === 0 }))
      : remaining
  return {
    ...snapshot,
    diets,
    meals: snapshot.meals.filter((x) => !meals.has(x.id)),
    courses: snapshot.courses.filter((x) => !courses.has(x.id)),
    foods: snapshot.foods.filter((x) => !courses.has(x.courseId)),
    lists: snapshot.lists.map((x) => (x.dietId === id ? { ...x, dietId: null } : x)),
  }
}
export const mealNames: Record<MealType, string> = {
  get BREAKFAST() {
    return t('Colazione')
  },
  get MORNING_SNACK() {
    return t('Spuntino mattutino')
  },
  get LUNCH() {
    return t('Pranzo')
  },
  get AFTERNOON_SNACK() {
    return t('Merenda')
  },
  get DINNER() {
    return t('Cena')
  },
  get OTHER() {
    return t('Altro')
  },
}
export const unitNames: Record<Unit, string> = {
  GRAMS: 'g',
  KILOGRAMS: 'kg',
  MILLILITERS: 'ml',
  LITERS: 'l',
  get PIECES() {
    return t('pz')
  },
  get SPOONS() {
    return t('cucchiai')
  },
  get TEASPOONS() {
    return t('cucchiaini')
  },
  get CUPS() {
    return t('tazze')
  },
  MILLIGRAMS: 'mg',
  CENTILITERS: 'cl',
  DECILITERS: 'dl',
  get SLICES() {
    return t('fette')
  },
  get CLOVES() {
    return t('spicchi')
  },
  get GLASSES() {
    return t('bicchieri')
  },
  get PINCHES() {
    return t('pizzichi')
  },
  get JARS() {
    return t('vasetti')
  },
  get CANS() {
    return t('lattine')
  },
  get PACKETS() {
    return t('buste')
  },
  get LEAVES() {
    return t('foglie')
  },
  get SPRIGS() {
    return t('rametti')
  },
  get DROPS() {
    return t('gocce')
  },
  get PORTIONS() {
    return t('porzioni')
  },
  get QB() {
    return t('q.b.')
  },
  get FREE() {
    return t('a piacere')
  },
  CUSTOM: '',
}
export const dayNames = localizedArray([
  'Lunedì',
  'Martedì',
  'Mercoledì',
  'Giovedì',
  'Venerdì',
  'Sabato',
  'Domenica',
])
export const timeLabel = (minutes: number) =>
  `${Math.floor(minutes / 60)
    .toString()
    .padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}`

/** A fresh ingredient is bought only when all its daily purchases are complete. */
export function setShoppingBought(
  snapshot: Snapshot,
  itemId: number,
  bought: boolean,
  dayId?: number,
): Snapshot {
  const days = snapshot.days.map((day) =>
    day.shoppingListItemId === itemId && (dayId === undefined || day.id === dayId)
      ? { ...day, isBought: bought }
      : day,
  )
  const allocations = days.filter((day) => day.shoppingListItemId === itemId)
  return {
    ...snapshot,
    days,
    items: snapshot.items.map((item) =>
      item.id === itemId
        ? {
            ...item,
            isBought: allocations.length ? allocations.every((day) => day.isBought) : bought,
          }
        : item,
    ),
  }
}

/** Mirrors Android QuantityUnit.format: amount-free units stand alone. */
export function formatQuantity(amount: string, unit: Unit): string {
  if (unit === 'QB' || unit === 'FREE') return unitNames[unit]
  const value = amount.trim()
  return value ? `${value} ${unitNames[unit] || 'g'}` : ''
}
