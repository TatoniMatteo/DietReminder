package it.matato.dietreminder.domain

import it.matato.dietreminder.data.database.relation.CourseWithItems

/** Shopping-only choices. The plan itself never stores a chosen alternative. */
fun List<CourseWithItems>.coursesForShopping(selectedCourseIds: Set<Long>? = null): List<CourseWithItems> {
    if (selectedCourseIds != null) {
        filter { it.course.alternativeGroup != null }.groupBy { it.course.alternativeGroup }.values.forEach { options ->
            require(options.count { it.course.id in selectedCourseIds } == 1) { "Choose one alternative per group before creating the shopping list" }
        }
    }
    val seen = mutableSetOf<String>()
    return filter { row ->
        val group = row.course.alternativeGroup
        group == null || if (selectedCourseIds != null) row.course.id in selectedCourseIds else seen.add(group)
    }
}
