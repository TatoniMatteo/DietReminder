package it.matato.dietreminder.data.database.entity

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.QuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodItemEntityTest {
	@Test
	fun testFoodItemCreation() {
		val item = FoodItem(id = 1L, courseId = 1L, name = "Pasta", amount = "100", unit = QuantityUnit.GRAMS)
		assertEquals(1L, item.id)
		assertEquals(1L, item.courseId)
		assertEquals("Pasta", item.name)
		assertEquals("100", item.amount)
		assertEquals(QuantityUnit.GRAMS, item.unit)
		assertEquals("100 g", item.displayQuantity)
	}
}
