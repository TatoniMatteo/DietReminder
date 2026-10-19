import type { FoodCategory, ShoppingItem, ShoppingDay, Snapshot } from './types'

export function normalizeFood(value: string) {
  return value.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')
}
export function foodCategory(
  name: string,
  manual: string | null | undefined,
  catalog: FoodCategory[],
) {
  if (manual?.trim()) return manual.trim()
  const text = normalizeFood(name)
  if (/(^|[^a-z0-9])surgelat[a-z]*(?:$|[^a-z0-9])/.test(text)) return 'Surgelati'
  const matches = catalog.flatMap((category) =>
    category.keywords.flatMap((keyword) => {
      const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const match = new RegExp(`(^|[^a-z0-9])${escaped}($|[^a-z0-9])`).exec(text)
      return match
        ? [
            {
              name: category.name,
              length: keyword.length,
              phrase: keyword.includes(' '),
              index: match.index,
            },
          ]
        : []
    }),
  )
  return (
    matches.sort(
      (a, b) => Number(b.phrase) - Number(a.phrase) || a.index - b.index || b.length - a.length,
    )[0]?.name ?? 'Altro'
  )
}
export interface ShoppingEntry {
  key: string
  item: ShoppingItem
  day?: ShoppingDay
  purchaseDay: number | null
  neededDay: number | null
  bought: boolean
  category: string
}
export function shoppingEntries(
  snapshot: Snapshot,
  listId: number,
  catalog: FoodCategory[],
): ShoppingEntry[] {
  return snapshot.items
    .filter((item) => item.shoppingListId === listId)
    .flatMap((item) => {
      const allocations = snapshot.days.filter((day) => day.shoppingListItemId === item.id)
      const category = foodCategory(item.name, item.category, catalog)
      return allocations.length
        ? allocations.map((day) => ({
            key: `${item.id}:${day.id}`,
            item,
            day,
            purchaseDay: day.purchaseDayOfWeek ?? day.dayOfWeek,
            neededDay: day.dayOfWeek,
            bought: day.isBought,
            category,
          }))
        : [
            {
              key: `${item.id}:0`,
              item,
              purchaseDay: item.purchaseDayOfWeek ?? item.dayOfWeek,
              neededDay: item.dayOfWeek,
              bought: item.isBought,
              category,
            },
          ]
    })
}
export function movePurchase(
  snapshot: Snapshot,
  itemId: number,
  purchaseDay: number | null,
  dayId?: number,
): Snapshot {
  return {
    ...snapshot,
    items: snapshot.items.map((item) =>
      item.id === itemId && dayId === undefined
        ? { ...item, purchaseDayOfWeek: purchaseDay }
        : item,
    ),
    days: snapshot.days.map((day) =>
      day.id === dayId && day.shoppingListItemId === itemId
        ? { ...day, purchaseDayOfWeek: purchaseDay }
        : day,
    ),
  }
}
