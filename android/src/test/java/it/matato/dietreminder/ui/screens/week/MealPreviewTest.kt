package it.matato.dietreminder.ui.screens.week

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class MealPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun collapsedMealShowsCoursesAndExpandedMealKeepsDescriptionAndIngredients() {
        val meal = MealWithDetails(Meal(1, 1, DayOfWeek.MONDAY, MealType.LUNCH, 780, description = "Note pranzo"), listOf(
            CourseWithItems(Course(1, 1, "Pasta al pesto"), listOf(FoodItem(1, 1, "Basilico"))),
            CourseWithItems(Course(2, 1, "Insalata mista"), emptyList()),
        ))
        compose.setContent { DietTheme { MealTimelineItem(meal) } }
        compose.onNodeWithText("Pasta al pesto, Insalata mista").assertIsDisplayed()
        compose.onNodeWithText("Basilico").assertDoesNotExist()
        compose.onNodeWithText("Note pranzo").assertDoesNotExist()
        compose.onNodeWithTag("weekMeal_1").performClick()
        compose.onNodeWithText("Note pranzo").assertIsDisplayed()
        compose.onNodeWithText("Basilico").assertIsDisplayed()
    }
}
