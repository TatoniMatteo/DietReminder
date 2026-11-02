package it.matato.dietreminder.data.export

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.model.MealType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=TestDietApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DietPdfExporterTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    @Test fun includesEveryAlternativeWithOwnIngredientsAndRecipe() {
        val diet = DietExport(name="Dieta", meals=listOf(MealExport(DayOfWeek.MONDAY,MealType.LUNCH,"12:30",courses=listOf(
            CourseExport("Riso",alternativeGroup="primo",description="Bollire il riso",items=listOf(FoodItemExport("Riso","80"))),
            CourseExport("Pasta",alternativeGroup="primo",description="Cuocere la pasta",items=listOf(FoodItemExport("Pasta","100"))),
        ))))
        val text = DietPdfExporter.lines(context,diet).joinToString("\n") { it.text }
        assertTrue(text.contains("Riso")); assertTrue(text.contains("Pasta"))
        assertTrue(text.contains("80 g")); assertTrue(text.contains("100 g"))
        assertTrue(text.contains("Bollire il riso")); assertTrue(text.contains("Cuocere la pasta"))
        assertTrue(text.contains("1 · Riso")); assertTrue(text.contains("2 · Pasta"))
    }
    @Test fun createsPaginatedPdfForLongRecipesAndHandlesEmptyDiet() {
        assertEquals(1,DietPdfExporter.layout(context,DietExport(name="Vuota")).size)
        val diet = DietExport(name="Inverno – città",meals=listOf(MealExport(DayOfWeek.MONDAY,MealType.LUNCH,"12:30",courses=listOf(
            CourseExport("Ricetta lunga",description="Zucca, quantità e preparazione. ".repeat(200)),
            CourseExport("Ultima alternativa",alternativeGroup="pasto",items=listOf(FoodItemExport("Merluzzo","200"))),
        ))))
        val pages = DietPdfExporter.layout(context,diet)
        assertTrue(pages.size>1)
        assertTrue(pages.flatten().all { it.y > 0 && it.y < 792f })
        val text = pages.flatten().joinToString(" ") { it.text }
        assertTrue(text.contains("Merluzzo"))
        assertEquals(200,Regex("preparazione").findAll(text).count())
        assertTrue(text.contains("Ultima alternativa"))
    }
}
