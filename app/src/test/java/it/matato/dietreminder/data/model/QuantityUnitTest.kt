package it.matato.dietreminder.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityUnitTest {

	@Test
	fun format_emptyAmountReturnsEmptyStringWhenNotSpecial() {
		val result = QuantityUnit.GRAMS.format("")
		assertEquals("", result)
	}

	@Test
	fun format_nonEmptyAmountFormatsWithUnitSymbol() {
		val result = QuantityUnit.GRAMS.format("100")
		assertEquals("100 g", result)
	}

	@Test
	fun format_customUnitWithAmountDefaultsToGrams() {
		val result = QuantityUnit.CUSTOM.format("100")
		assertEquals("100 g", result)
	}

	@Test
	fun format_specialUnitReturnsSymbolWithoutAmount() {
		val resultQb = QuantityUnit.QB.format("")
		assertEquals("q.b.", resultQb)

		val resultAPiacere = QuantityUnit.FREE.format("")
		assertEquals("a piacere", resultAPiacere)
	}

	@Test
	fun format_singularAndPluralSymbols() {
		assertEquals("1 cucchiaio", QuantityUnit.SPOONS.format("1"))
		assertEquals("2 cucchiai", QuantityUnit.SPOONS.format("2"))

		assertEquals("1 spicchio", QuantityUnit.CLOVES.format("1"))
		assertEquals("2 spicchi", QuantityUnit.CLOVES.format("2"))

		assertEquals("1 vasetto", QuantityUnit.JARS.format("1"))
		assertEquals("3 vasetti", QuantityUnit.JARS.format("3"))
	}
}
