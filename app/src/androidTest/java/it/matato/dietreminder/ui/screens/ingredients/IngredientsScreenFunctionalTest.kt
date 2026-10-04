package it.matato.dietreminder.ui.screens.ingredients

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientsScreenFunctionalTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun testIngredientsScreen_DisplaysTitleAndEmptyState() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		composeTestRule.setContent {
			DietTheme {
				IngredientsScreen(vm = viewModel)
			}
		}

		ingredientsRobot(composeTestRule) {
			verifyTitle()
		}
	}

	@Test
	fun testIngredientsScreen_DisplaysAndExpandsIngredient() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		var dietId = 0L
		runBlocking {
			dietId = fakeRepository.create("Dieta Test", 60)
			val meal = Meal(
				dietId = dietId,
				dayOfWeek = DayOfWeek.MONDAY,
				type = MealType.LUNCH,
				timeMinutes = 13 * 60,
			)
			val courseWithItems = CourseWithItems(
				course = Course(id = 10, mealId = 1, name = "Primo"),
				items = listOf(
					FoodItem(
						id = 100,
						courseId = 10,
						name = "Spaghetti integrali",
						amount = "100",
						unit = QuantityUnit.GRAMS)
				)
			)
			fakeRepository.saveMeal(meal, listOf(courseWithItems))
		}

		composeTestRule.setContent {
			DietTheme {
				IngredientsScreen(vm = viewModel, dietId = dietId)
			}
		}

		ingredientsRobot(composeTestRule) {
			verifyTitle()
			verifyIngredientDisplayed("Spaghetti integrali")
			clickIngredient("Spaghetti integrali")
			verifyQuantityDisplayed("100 g")
		}
	}
}
