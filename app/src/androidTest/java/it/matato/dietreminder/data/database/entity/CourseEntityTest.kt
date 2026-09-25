package it.matato.dietreminder.data.database.entity

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseEntityTest {
    @Test
    fun testCourseCreation() {
        val course = Course(id = 1L, mealId = 1L, name = "Primo Piatto")
        assertEquals(1L, course.id)
        assertEquals(1L, course.mealId)
        assertEquals("Primo Piatto", course.name)
    }
}
