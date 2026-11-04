import type { Snapshot } from './types'
import { timeLabel } from './workspace'
const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
/** Export every course, including all alternatives, without applying shopping choices. */
export function exportDiet(snapshot: Snapshot, dietId: number) {
  const diet = snapshot.diets.find((d) => d.id === dietId)
  if (!diet) throw new Error('Diet not found')
  return {
    formatVersion: 2,
    uuid: diet.uuid,
    name: diet.name,
    nextMealWindowMinutes: diet.nextMealWindowMinutes,
    disabledNotificationDays: diet.disabledNotificationDays,
    meals: snapshot.meals
      .filter((m) => m.dietId === dietId)
      .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.timeMinutes - b.timeMinutes || a.id - b.id)
      .map((m) => ({
        day: weekdays[m.dayOfWeek - 1],
        type: m.type,
        time: timeLabel(m.timeMinutes),
        description: m.description,
        customTypeLabel: m.customTypeLabel,
        isNotificationEnabled: m.isNotificationEnabled,
        courses: snapshot.courses
          .filter((c) => c.mealId === m.id)
          .sort((a, b) => a.order - b.order || a.id - b.id)
          .map((c) => ({
            name: c.name,
            order: c.order,
            alternativeGroup: c.alternativeGroup ?? null,
            description: c.description ?? '',
            items: snapshot.foods
              .filter((f) => f.courseId === c.id)
              .sort((a, b) => a.order - b.order || a.id - b.id)
              .map((f) => ({ name: f.name, amount: f.amount, unit: f.unit, order: f.order })),
          })),
      })),
  }
}
export function downloadDietJson(snapshot: Snapshot, dietId: number) {
  const data = exportDiet(snapshot, dietId)
  const url = URL.createObjectURL(
    new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' }),
  )
  const link = document.createElement('a')
  link.href = url
  link.download = `${data.name.replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_') || 'dieta'}.json`
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
