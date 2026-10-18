import { describe, expect, it } from 'vitest'
import { emptySnapshot } from './workspace'
import { foodCategory, movePurchase, shoppingEntries } from './shopping'
const catalog = [
  { name: 'Frutta e verdura', keywords: ['mele', 'insalata'] },
  { name: 'Latte, uova e formaggi', keywords: ['latte'] },
  { name: 'Bevande', keywords: ['latte di soia'] },
]
describe('shopping purchase planning', () => {
  it('keeps use day, quantity and purchase state when moving a single allocation', () => {
    const snapshot = emptySnapshot()
    snapshot.items = [
      {
        id: 1,
        shoppingListId: 2,
        name: 'Insalata',
        amount: '',
        unit: 'GRAMS',
        isBought: false,
        isCustom: false,
        isFresh: true,
        dayOfWeek: null,
        category: 'Orto',
      },
    ]
    snapshot.days = [
      { id: 3, shoppingListItemId: 1, dayOfWeek: 1, amount: '150', unit: 'GRAMS', isBought: true },
      { id: 4, shoppingListItemId: 1, dayOfWeek: 2, amount: '200', unit: 'GRAMS', isBought: false },
    ]
    const moved = movePurchase(snapshot, 1, 7, 3)
    expect(moved.days[0]).toEqual({ ...snapshot.days[0], purchaseDayOfWeek: 7 })
    expect(moved.days[1]).toEqual(snapshot.days[1])
    expect(snapshot.days[0].purchaseDayOfWeek).toBeUndefined()
    expect(
      shoppingEntries(moved, 2, catalog).map((row) => [
        row.purchaseDay,
        row.neededDay,
        row.bought,
        row.category,
      ]),
    ).toEqual([
      [7, 1, true, 'Orto'],
      [2, 2, false, 'Orto'],
    ])
    expect(movePurchase(moved, 1, null, 3).days[0].dayOfWeek).toBe(1)
  })
  it('classifies automatically without matching inside unrelated words and supports manual overrides', () => {
    expect(foodCategory('  MELE BIO ', null, catalog)).toBe('Frutta e verdura')
    expect(foodCategory('cioccolatte', null, catalog)).toBe('Altro')
    expect(foodCategory('latte di soia', null, catalog)).toBe('Bevande')
    expect(foodCategory('Mele', 'Mercato', catalog)).toBe('Mercato')
  })
})
