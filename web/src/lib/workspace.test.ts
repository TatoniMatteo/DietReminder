import { describe, expect, it } from 'vitest'
import { emptySnapshot, nextId, removeDiet, setShoppingBought, formatQuantity } from './workspace'

describe('shared workspace graph', () => {
  it('preserves unrelated meals and detaches shopping lists when deleting a diet', () => {
    const s = emptySnapshot()
    s.diets = [1, 2].map((id) => ({
      id,
      uuid: `uuid-${id}`,
      name: `Plan ${id}`,
      isActive: id === 1,
      nextMealWindowMinutes: 90,
      disabledNotificationDays: '',
    }))
    s.meals = [1, 2].map((id) => ({
      id: id + 10,
      dietId: id,
      dayOfWeek: 1,
      type: 'LUNCH',
      timeMinutes: 720,
      description: '',
      customTypeLabel: null,
      isNotificationEnabled: true,
    }))
    s.courses = [{ id: 20, mealId: 11, name: '', order: 0 }]
    s.foods = [{ id: 30, courseId: 20, name: 'Pane', amount: '50', unit: 'GRAMS', order: 0 }]
    s.lists = [{ id: 40, name: 'Spesa', createdAt: 1, dietId: 1 }]
    const result = removeDiet(s, 1)
    expect(result.meals.map((m) => m.dietId)).toEqual([2])
    expect(result.foods).toEqual([])
    expect(result.courses).toEqual([])
    expect(result.lists[0].dietId).toBeNull()
    expect(s.diets).toHaveLength(2)
  })
  it('allocates IDs above every kind of existing entity', () => {
    const s = emptySnapshot()
    s.days = [
      { id: 900, shoppingListItemId: 1, dayOfWeek: 1, amount: '', unit: 'GRAMS', isBought: false },
    ]
    expect(nextId(s)).toBe(901)
    expect(nextId(emptySnapshot())).toBe(1)
  })
})

describe('daily shopping shared with Android', () => {
  it('updates individual days and derives the parent state without altering other data', () => {
    const s = emptySnapshot()
    s.items = [
      {
        id: 1,
        shoppingListId: 10,
        name: 'Verdure',
        amount: '',
        unit: 'GRAMS',
        isBought: false,
        isCustom: false,
        isFresh: true,
        dayOfWeek: null,
      },
      {
        id: 2,
        shoppingListId: 10,
        name: 'Pane',
        amount: '50',
        unit: 'GRAMS',
        isBought: true,
        isCustom: true,
        isFresh: false,
        dayOfWeek: null,
      },
    ]
    s.days = [1, 2].map((id) => ({
      id,
      shoppingListItemId: 1,
      dayOfWeek: id,
      amount: '100',
      unit: 'GRAMS',
      isBought: false,
    }))
    const partial = setShoppingBought(s, 1, true, 1)
    expect(partial.days.map((day) => day.isBought)).toEqual([true, false])
    expect(partial.items[0].isBought).toBe(false)
    expect(partial.items[1]).toEqual(s.items[1])
    const complete = setShoppingBought(partial, 1, true, 2)
    expect(complete.items[0].isBought).toBe(true)
    expect(setShoppingBought(complete, 1, false).days.every((day) => !day.isBought)).toBe(true)
    expect(s.days.every((day) => !day.isBought)).toBe(true)
  })
})

it('formats quantities like Android without phantom grams or amounts on q.b.', () => {
  expect(formatQuantity('', 'GRAMS')).toBe('')
  expect(formatQuantity('10', 'QB')).toBe('q.b.')
  expect(formatQuantity('10', 'FREE')).toBe('a piacere')
  expect(formatQuantity(' 1,5 ', 'KILOGRAMS')).toBe('1,5 kg')
})
