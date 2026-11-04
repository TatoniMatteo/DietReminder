import type { Course } from './types'
import { t } from './i18n'
export function validateAlternatives(courses: Pick<Course, 'alternativeGroup'>[]) {
  if (
    courses.some(
      (c) =>
        c.alternativeGroup != null &&
        (!c.alternativeGroup.trim() || c.alternativeGroup.length > 128),
    )
  )
    throw new Error(t('Gruppo di alternative non valido.'))
}
export function alternativeKey(course: Course) {
  return `${course.mealId}:${course.alternativeGroup}`
}
/** Temporary shopping choices; no choice is stored on the diet. */
export function shoppingCourses(courses: Course[], selectedCourseIds?: number[]): Course[] {
  if (selectedCourseIds) {
    const keys = [...new Set(courses.filter((c) => c.alternativeGroup).map(alternativeKey))]
    if (
      keys.some(
        (key) =>
          courses.filter((c) => alternativeKey(c) === key && selectedCourseIds.includes(c.id))
            .length !== 1,
      )
    )
      throw new Error(t('Scegli una alternativa per ogni pasto prima di creare la spesa.'))
  }
  const groups = new Set<string>()
  return courses.filter((c) => {
    if (!c.alternativeGroup) return true
    const key = alternativeKey(c)
    if (selectedCourseIds) return selectedCourseIds.includes(c.id)
    if (groups.has(key)) return false
    groups.add(key)
    return true
  })
}

export function courseSummary(courses: Course[]): string {
  const seen = new Set<string>()
  return courses
    .flatMap((c) => {
      if (!c.alternativeGroup) return c.name ? [c.name] : []
      const key = alternativeKey(c)
      if (seen.has(key)) return []
      seen.add(key)
      return [
        courses
          .filter((option) => alternativeKey(option) === key)
          .map((option) => option.name)
          .filter(Boolean)
          .join(' / '),
      ]
    })
    .join(' · ')
}
