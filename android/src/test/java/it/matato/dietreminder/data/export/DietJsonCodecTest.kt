package it.matato.dietreminder.data.export

import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import java.io.File
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DietJsonCodecTest {

	private val codec = DietJsonCodec()

	@Test
	fun encodeAndDecode_preservesDietData() {
		val originalExport = DietExport(
			uuid = "test-uuid-123",
			name = "Dieta Mediterranea",
			nextMealWindowMinutes = 60,
			meals = listOf(
				MealExport(
					day = DayOfWeek.MONDAY,
					type = MealType.BREAKFAST,
					time = "08:00",
					description = "Caffè e fette biscottate",
					courses = listOf(
						CourseExport(
							name = "Primo",
							order = 0,
							items = listOf(
								FoodItemExport(
									name = "Fette biscottate",
									amount = "3",
									unit = QuantityUnit.PIECES,
									order = 0),
							),
						),
					),
				),
			),
		)

		val json = codec.encode(originalExport)
		assertNotNull(json)

		val decoded = codec.decode(json)
		assertEquals(originalExport.uuid, decoded.uuid)
		assertEquals(originalExport.name, decoded.name)
		assertEquals(originalExport.nextMealWindowMinutes, decoded.nextMealWindowMinutes)
		assertEquals(1, decoded.meals.size)
		assertEquals(DayOfWeek.MONDAY, decoded.meals[0].day)
		assertEquals(MealType.BREAKFAST, decoded.meals[0].type)
		assertEquals("08:00", decoded.meals[0].time)
		assertEquals("Caffè e fette biscottate", decoded.meals[0].description)
		assertEquals(1, decoded.meals[0].courses.size)
		assertEquals("Primo", decoded.meals[0].courses[0].name)
	}

	@Test
	fun decode_settembreOttobreFile_succeeds() {
		val file = File("/home/matato/Scaricati/settembre_ottobre_2026.dr")
		if (file.exists()) {
			val json = file.readText()
			val decoded = codec.decode(json)
			assertEquals("Settembre/Ottobre 2026", decoded.name)
			assertEquals("948537de-8878-4b3f-bce1-bf2b52bc1a4a", decoded.uuid)
			assertEquals(35, decoded.meals.size)
		}
	}

	@Test
	fun decode_handlesUnknownKeysGracefully() {
		val json = """
            {
                "uuid": "test-uuid-456",
                "name": "Dieta Test",
                "nextMealWindowMinutes": 90,
                "unknownField": "should be ignored",
                "meals": []
            }
        """.trimIndent()

		val decoded = codec.decode(json)
		assertEquals("test-uuid-456", decoded.uuid)
		assertEquals("Dieta Test", decoded.name)
		assertEquals(90, decoded.nextMealWindowMinutes)
		assertEquals(0, decoded.meals.size)
	}

	@Test(expected = Exception::class)
	fun decode_blankJsonThrowsException() {
		codec.decode("   ")
	}

	@Test(expected = Exception::class)
	fun decode_malformedJsonThrowsException() {
		codec.decode("{ invalid json syntax }")
	}

	@Test(expected = IllegalArgumentException::class)
	fun decode_blankDietName_throwsException() {
		val json = """
            {
                "name": "   ",
                "meals": []
            }
        """.trimIndent()
		codec.decode(json)
	}

	@Test(expected = IllegalArgumentException::class)
	fun decode_invalidMealTimeFormat_throwsException() {
		val json = """
            {
                "name": "Dieta Test",
                "meals": [
                    {
                        "day": "MONDAY",
                        "type": "BREAKFAST",
                        "time": "25:00",
                        "courses": []
                    }
                ]
            }
        """.trimIndent()
		codec.decode(json)
	}

	@Test(expected = IllegalArgumentException::class)
	fun decode_blankFoodItemName_throwsException() {
		val json = """
            {
                "name": "Dieta Test",
                "meals": [
                    {
                        "day": "MONDAY",
                        "type": "BREAKFAST",
                        "time": "08:00",
                        "courses": [
                            {
                                "name": "Primo",
                                "items": [
                                    { "name": "   ", "amount": "100" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
		codec.decode(json)
	}

	@Test(expected = IllegalArgumentException::class)
	fun decode_unitWithoutAmount_throwsException() {
		val json = """
            {
                "name": "Dieta Test",
                "meals": [
                    {
                        "day": "MONDAY",
                        "type": "BREAKFAST",
                        "time": "08:00",
                        "courses": [
                            {
                                "name": "Primo",
                                "items": [
                                    { "name": "Spaghetti", "amount": "  ", "unit": "GRAMS" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
		codec.decode(json)
	}

	@Test(expected = IllegalArgumentException::class)
	fun decode_invalidAmountFormat_throwsException() {
		val json = """
            {
                "name": "Dieta Test",
                "meals": [
                    {
                        "day": "MONDAY",
                        "type": "BREAKFAST",
                        "time": "08:00",
                        "courses": [
                            {
                                "name": "Primo",
                                "items": [
                                    { "name": "Spaghetti", "amount": "abc", "unit": "GRAMS" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
		codec.decode(json)
	}

	@Test
	fun decode_specialUnitWithoutAmount_succeeds() {
		val json = """
            {
                "name": "Dieta Test",
                "meals": [
                    {
                        "day": "MONDAY",
                        "type": "BREAKFAST",
                        "time": "08:00",
                        "courses": [
                            {
                                "name": "Primo",
                                "items": [
                                    { "name": "Sale", "amount": "", "unit": "QB" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

		val decoded = codec.decode(json)
		assertEquals("Dieta Test", decoded.name)
		assertEquals("Sale", decoded.meals[0].courses[0].items[0].name)
		assertEquals(QuantityUnit.QB, decoded.meals[0].courses[0].items[0].unit)
	}
}
