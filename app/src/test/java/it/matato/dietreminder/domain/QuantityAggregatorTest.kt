package it.matato.dietreminder.domain

import it.matato.dietreminder.data.model.QuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityAggregatorTest {

	@Test
	fun sumValuesInGrams() {
		val values = List(7) { QuantityValue(amount = "45", unit = QuantityUnit.GRAMS) }
		val result = QuantityAggregator.sumValues(values)
		assertEquals("315 g", result)
	}

	@Test
	fun sumValuesKilogramsAndGrams() {
		val values = listOf(
			QuantityValue(amount = "500", unit = QuantityUnit.GRAMS),
			QuantityValue(amount = "1", unit = QuantityUnit.KILOGRAMS),
		)
		val result = QuantityAggregator.sumValues(values)
		assertEquals("1.5 kg", result)
	}

	@Test
	fun sumValuesMilliliters() {
		val values = listOf(
			QuantityValue(amount = "200", unit = QuantityUnit.MILLILITERS),
			QuantityValue(amount = "300", unit = QuantityUnit.MILLILITERS),
		)
		val result = QuantityAggregator.sumValues(values)
		assertEquals("500 ml", result)
	}

	@Test
	fun sumValuesWithCustomUnits() {
		val values = listOf(
			QuantityValue(amount = "2", unit = QuantityUnit.SPOONS),
			QuantityValue(amount = "1", unit = QuantityUnit.SPOONS),
		)
		val result = QuantityAggregator.sumValues(values)
		assertEquals("3 cucchiai", result)
	}

	@Test
	fun sumValuesSpecialUnits() {
		val values = listOf(
			QuantityValue(amount = "", unit = QuantityUnit.QB),
			QuantityValue(amount = "", unit = QuantityUnit.QB),
		)
		val result = QuantityAggregator.sumValues(values)
		assertEquals("q.b.", result)
	}
}
