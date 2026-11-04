package it.matato.dietreminder.common.model

import org.junit.Assert.*
import org.junit.Test

class MealAlternativesTest {
    private fun snapshot(courses: List<CloudCourse>, version: Int = 3) = WorkspaceSnapshot(
        schemaVersion = version, diets = listOf(CloudDiet(1, "diet", "Piano")),
        meals = listOf(CloudMeal(1, 1, 1, MealType.LUNCH, 720)), courses = courses,
    )
    @Test fun alternativesArePossibilitiesWithNoPersistedChoice() {
        snapshot(listOf(CloudCourse(1,1,"Pane"), CloudCourse(2,1,"Riso",alternativeGroup="main"), CloudCourse(3,1,"Pasta",alternativeGroup="main"))).requireValid()
        snapshot(listOf(CloudCourse(1,1,"Legacy")), 2).requireValid()
    }
    @Test fun rejectInvalidGroupsAndLegacyWritesContainingAlternatives() {
        assertThrows(IllegalArgumentException::class.java) { snapshot(listOf(CloudCourse(1,1,alternativeGroup=" "))).requireValid() }
        assertThrows(IllegalArgumentException::class.java) { snapshot(listOf(CloudCourse(1,1,alternativeGroup="a")), 2).requireValid() }
    }
}
