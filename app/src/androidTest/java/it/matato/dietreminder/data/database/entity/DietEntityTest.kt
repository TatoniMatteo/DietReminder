package it.matato.dietreminder.data.database.entity

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietEntityTest {
    @Test
    fun testDietCreation() {
        val diet = Diet(id = 1L, name = "Test", nextMealWindowMinutes = 60, isActive = true)
        assertEquals("Test", diet.name)
        assertEquals(60, diet.nextMealWindowMinutes)
    }
}
