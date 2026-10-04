package it.matato.dietreminder.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDietRepositoryTest {

	private lateinit var database: AppDatabase
	private lateinit var repository: RoomDietRepository

	@Before
	fun setUp() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
			.allowMainThreadQueries()
			.build()

		repository = RoomDietRepository(
			database = database,
			diets = database.dietDao(),
			meals = database.mealDao(),
			courses = database.courseDao(),
			foodItems = database.foodItemDao(),
			config = database.configDao(),
			shoppingLists = database.shoppingListDao(),
		)
	}

	@After
	fun tearDown() {
		database.close()
	}

	@Test
	fun createAndObserveDiet() = runBlocking {
		val dietId = repository.create("Dieta Mediterranea", 60)
		assertTrue(dietId > 0)

		val active = repository.active.first()
		assertNotNull(active)
		assertEquals("Dieta Mediterranea", active?.name)
		assertTrue(active?.isActive == true)
	}

	@Test
	fun saveAndGetMealWithDetails() = runBlocking {
		val dietId = repository.create("Dieta Test", 60)

		val meal = Meal(
			dietId = dietId,
			type = MealType.BREAKFAST,
			timeMinutes = 8 * 60,
			dayOfWeek = DayOfWeek.MONDAY,
			description = "Colazione proteica",
		)

		val course = CourseWithItems(
			course = Course(mealId = 0, name = "Primo", order = 0),
			items = listOf(
				FoodItem(courseId = 0, name = "Fette biscottate", amount = "3", unit = QuantityUnit.PIECES, order = 0),
			),
		)

		repository.saveMeal(meal, listOf(course))

		val mealsList = repository.getMeals(dietId)
		assertEquals(1, mealsList.size)

		val savedMeal = mealsList[0]
		assertEquals(MealType.BREAKFAST, savedMeal.meal.type)
		assertEquals(1, savedMeal.courses.size)
		assertEquals("Primo", savedMeal.courses[0].course.name)
		assertEquals(1, savedMeal.courses[0].items.size)
		assertEquals("Fette biscottate", savedMeal.courses[0].items[0].name)
	}

	@Test
	fun saveAndObserveDefaultTimes() = runBlocking {
		repository.saveDefaultTime(MealType.LUNCH, 13 * 60)

		val times = repository.defaultTimes.first()
		assertEquals(1, times.size)
		assertEquals(MealType.LUNCH, times[0].type)
		assertEquals(13 * 60, times[0].timeMinutes)
	}

	@Test
	fun duplicateDiet() = runBlocking {
		val dietId = repository.create("Dieta Originale", 60)
		val meal = Meal(
			dietId = dietId,
			type = MealType.LUNCH,
			timeMinutes = 13 * 60,
			dayOfWeek = DayOfWeek.MONDAY,
			description = "Pranzo",
		)
		repository.saveMeal(meal, emptyList())

		val duplicatedId = repository.duplicate(dietId)
		assertTrue(duplicatedId > dietId)

		val duplicatedMeals = repository.getMeals(duplicatedId)
		assertEquals(1, duplicatedMeals.size)
		assertEquals(MealType.LUNCH, duplicatedMeals[0].meal.type)
	}

	@Test
	fun deleteDiet() = runBlocking {
		val dietId1 = repository.create("Dieta 1", 60)
		val dietId2 = repository.create("Dieta 2", 60)

		repository.activate(dietId1)
		val deleted = repository.delete(dietId1)

		assertEquals(1, deleted)
		val active = repository.active.first()
		assertEquals(dietId2, active?.id)
	}

	@Test
	fun createAndObserveShoppingList() = runBlocking {
		val listId = repository.createShoppingList("Lista Settimanale")
		assertTrue(listId > 0)

		val lists = repository.allShoppingLists.first()
		assertEquals(1, lists.size)
		assertEquals("Lista Settimanale", lists[0].list.name)
		assertEquals(listId, lists[0].list.id)
	}

	@Test
	fun addAndToggleShoppingListItems() = runBlocking {
		val listId = repository.createShoppingList("Lista Spesa")
		val itemId = repository.addShoppingListItem(listId, "Mela", "1", unit = QuantityUnit.KILOGRAMS, isCustom = true)

		var listWithItems = repository.observeShoppingList(listId).first()
		assertNotNull(listWithItems)
		assertEquals(1, listWithItems?.items?.size)
		assertEquals("Mela", listWithItems?.items?.get(0)?.item?.name)
		assertEquals(false, listWithItems?.items?.get(0)?.item?.isBought)

		repository.toggleShoppingListItemBought(itemId, true)
		listWithItems = repository.observeShoppingList(listId).first()
		assertEquals(true, listWithItems?.items?.get(0)?.item?.isBought)

		repository.deleteShoppingListItem(itemId)
		listWithItems = repository.observeShoppingList(listId).first()
		assertTrue(listWithItems?.items?.isEmpty() == true)
	}

	@Test
	fun deleteShoppingList() = runBlocking {
		val listId = repository.createShoppingList("Lista Da Eliminare")
		repository.deleteShoppingList(listId)

		val listWithItems = repository.observeShoppingList(listId).first()
		assertNull(listWithItems)
	}

	@Test
	fun importDietIngredientsFreshVsStandard() = runBlocking {
		val dietId = repository.create("Dieta Test", 60)
		repository.activate(dietId)

		// Monday Lunch: Salmone 150g, Biscotti 45g
		val mealMon = Meal(dietId = dietId, dayOfWeek = DayOfWeek.MONDAY, type = MealType.LUNCH, timeMinutes = 13 * 60)
		val coursesMon = listOf(
			CourseWithItems(
				course = Course(mealId = 0, name = "Primo"),
				items = listOf(
					FoodItem(courseId = 0, name = "Salmone", amount = "150", unit = QuantityUnit.GRAMS),
					FoodItem(courseId = 0, name = "Biscotti", amount = "45", unit = QuantityUnit.GRAMS),
				),
			),
		)
		repository.saveMeal(mealMon, coursesMon)

		// Thursday Lunch: Salmone 150g, Biscotti 45g
		val mealThu =
			Meal(dietId = dietId, dayOfWeek = DayOfWeek.THURSDAY, type = MealType.LUNCH, timeMinutes = 13 * 60)
		val coursesThu = listOf(
			CourseWithItems(
				course = Course(mealId = 0, name = "Primo"),
				items = listOf(
					FoodItem(courseId = 0, name = "Salmone", amount = "150", unit = QuantityUnit.GRAMS),
					FoodItem(courseId = 0, name = "Biscotti", amount = "45", unit = QuantityUnit.GRAMS),
				),
			),
		)
		repository.saveMeal(mealThu, coursesThu)

		val listId = repository.createShoppingList("Spesa Settimana")

		val configs = listOf(
			DietImportItemConfig(ingredientName = "Biscotti", isFresh = false),
			DietImportItemConfig(
				ingredientName = "Salmone",
				isFresh = true,
				selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
		)

		repository.addDietIngredientsToShoppingList(listId, dietId, configs)

		val listWithItems = repository.observeShoppingList(listId).first()
		assertNotNull(listWithItems)

		val items = listWithItems?.items ?: emptyList()
		// Biscotti: 1 item aggregated (90 g)
		val biscottiItems = items.filter { it.item.name == "Biscotti" }
		assertEquals(1, biscottiItems.size)
		assertEquals("90 g", biscottiItems[0].item.displayQuantity)
		assertEquals(false, biscottiItems[0].item.isFresh)

		// Salmone: 1 fresh item with 2 days (Monday: 150 g, Thursday: 150 g)
		val salmoneItems = items.filter { it.item.name == "Salmone" }
		assertEquals(1, salmoneItems.size)
		assertTrue(salmoneItems[0].item.isFresh)
		assertEquals(2, salmoneItems[0].days.size)
		assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), salmoneItems[0].days.map { it.dayOfWeek }.toSet())
	}
}
