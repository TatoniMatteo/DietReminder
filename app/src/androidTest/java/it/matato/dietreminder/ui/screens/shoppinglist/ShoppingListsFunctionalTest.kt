package it.matato.dietreminder.ui.screens.shoppinglist

import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListsFunctionalTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun testShoppingLists_CreateNewList() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		composeTestRule.setContent {
			DietTheme {
				ShoppingListsScreen(
					vm = viewModel,
					onSelectList = {},
					onNavigateToImportDietFoods = { _, _ -> },
					onNavigateToIngredients = {},
				)
			}
		}

		shoppingListsRobot(composeTestRule) {
			verifyTitle()
			clickCreateNewList()
			confirmCreateList()
		}

		runBlocking {
			val lists = fakeRepository.allShoppingLists
			assertNotNull(lists)
		}
	}

	@Test
	fun testShoppingListDetail_AddManualItemAndToggleBought() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		var listId = 0L
		runBlocking {
			listId = fakeRepository.createShoppingList("Spesa Test")
		}

		composeTestRule.setContent {
			DietTheme {
				ShoppingListDetailScreen(
					vm = viewModel,
					listId = listId,
					onBack = {},
					onNavigateToImportDietFoods = { _, _ -> },
				)
			}
		}

		shoppingListsRobot(composeTestRule) {
			clickAddItemFab()
			clickAddManualItem()
			typeItemName("Pane")
			clickSaveDialog()
			verifyItemDisplayed("Pane")

			toggleItemBought("Pane")
			verifyCompletionCardDisplayed()
		}

		runBlocking {
			val listWithItems = fakeRepository.getMeals(listId)
			assertNotNull(listWithItems)
		}
	}

	@Test
	fun testImportDietFoods_DisplaysIngredientsAndImportsToShoppingList() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		var listId = 0L
		var dietId = 0L

		runBlocking {
			dietId = fakeRepository.create("Dieta Test", 60)
			fakeRepository.activate(dietId)
			val meal = Meal(
				dietId = dietId,
				dayOfWeek = DayOfWeek.MONDAY,
				type = MealType.LUNCH,
				timeMinutes = 13 * 60,
			)
			val courseWithItems = CourseWithItems(
				course = Course(id = 10, mealId = 1, name = "Primo"),
				items = listOf(
					FoodItem(id = 100, courseId = 10, name = "Salmone", amount = "150", unit = QuantityUnit.GRAMS),
				),
			)
			fakeRepository.saveMeal(meal, listOf(courseWithItems))
			listId = fakeRepository.createShoppingList("Lista Spesa")
		}

		var backClicked = false

		composeTestRule.setContent {
			DietTheme {
				ImportDietFoodsScreen(
					vm = viewModel,
					listId = listId,
					dietId = dietId,
					onBack = { backClicked = true },
				)
			}
		}

		composeTestRule.onNode(hasText("Salmone")).assertExists()
		composeTestRule.onNode(hasText("Importa 1 alimenti") or hasText("Import 1 foods")).performClick()

		assertTrue(backClicked)
	}
}
