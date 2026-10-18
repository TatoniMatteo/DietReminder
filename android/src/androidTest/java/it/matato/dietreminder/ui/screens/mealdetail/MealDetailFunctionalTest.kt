package it.matato.dietreminder.ui.screens.mealdetail

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealDetailFunctionalTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun testMealDetail_CreateAndSaveNewMeal() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		var backClicked = false
		var dietId = 0L

		runBlocking {
			dietId = fakeRepository.create("Dieta Test", 60)
		}

		composeTestRule.setContent {
			DietTheme {
				MealDetailScreen(
					vm = viewModel,
					mealId = 0L,
					dietId = dietId,
					dayOfWeek = DayOfWeek.MONDAY,
					onBack = { backClicked = true },
				)
			}
		}

		mealDetailRobot(composeTestRule) {
			clickSave()
			assertTrue(backClicked)
		}

		runBlocking {
			val meals = fakeRepository.getMeals(dietId)
			assertEquals(1, meals.size)
		}
	}

	@Test
	fun testMealDetail_AddCourseAndFoodItem() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		var backClicked = false
		var dietId = 0L

		runBlocking {
			dietId = fakeRepository.create("Dieta Test", 60)
		}

		composeTestRule.setContent {
			DietTheme {
				MealDetailScreen(
					vm = viewModel,
					mealId = 0L,
					dietId = dietId,
					dayOfWeek = DayOfWeek.MONDAY,
					onBack = { backClicked = true },
				)
			}
		}

		mealDetailRobot(composeTestRule) {
			clickAddCourse()
			clickAddFoodItem()
			typeFoodItemName("Spaghetti")
			typeQuantityAmount("100")
			clickSave()
			assertTrue(backClicked)
		}

		runBlocking {
			val meals = fakeRepository.getMeals(dietId)
			assertEquals(1, meals.size)
			val firstMeal = meals[0]
			assertEquals(1, firstMeal.courses.size)
			val firstCourse = firstMeal.courses[0]
			assertEquals(1, firstCourse.items.size)
			assertEquals("Spaghetti", firstCourse.items[0].name)
			assertEquals("100", firstCourse.items[0].amount)
		}
	}
    @Test
    fun saveRemainsVisibleWhenEditingTheBottomOfALongMeal() {
        var saved = false
        composeTestRule.setContent {
            DietTheme {
                MealDetailContent(
                    isLoading = false, isNew = false, type = MealType.LUNCH, timeMinutes = 780,
                    customLabel = "", description = "Note", isNotificationEnabled = false, isNotificationSwitchEnabled = true,
                    courses = listOf(CourseWithItems(Course(id = 1, mealId = 1, name = "Piatto unico"),
                        (1..5).map { FoodItem(id = it.toLong(), courseId = 1, name = "Ingrediente $it", amount = "80") })),
                    onBack = {}, onSave = { saved = true }, onDelete = {}, onTimeClick = {}, onTypeChange = {},
                    onCustomLabelChange = {}, onDescriptionChange = {}, onNotificationEnabledChange = {},
                    onAddCourse = {}, onUpdateCourse = { _, _ -> }, onDeleteCourse = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("meal_detail_list")
            .performScrollToNode(hasText("Ingrediente 5"))
        composeTestRule.onNodeWithText("Ingrediente 5").assertIsDisplayed()
        val save = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.save)
        composeTestRule.onNodeWithText(save).assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertTrue(saved) }
    }

}
