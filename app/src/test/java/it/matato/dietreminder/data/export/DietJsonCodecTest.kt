package it.matato.dietreminder.data.export

import it.matato.dietreminder.data.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.DayOfWeek

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
                                FoodItemExport(name = "Fette biscottate", quantities = "3", order = 0)
                            )
                        )
                    )
                )
            )
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
}
