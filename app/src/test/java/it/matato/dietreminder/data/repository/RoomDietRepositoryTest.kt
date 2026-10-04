package it.matato.dietreminder.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.aggregate.CompositeDietRepository
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.delegating.DelegatingDietRepository
import it.matato.dietreminder.data.repository.delegating.DelegatingShoppingListRepository
import it.matato.dietreminder.data.repository.delegating.OfflineWriteException
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.data.repository.room.RoomConfigRepository
import it.matato.dietreminder.data.repository.room.RoomDietRepository
import it.matato.dietreminder.data.repository.room.RoomShoppingListRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDietRepositoryTest {

	private lateinit var database: AppDatabase
	private lateinit var repository: CompositeDietRepository

	@Before
	fun setUp() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
			.allowMainThreadQueries()
			.build()

		val dietRepo = RoomDietRepository(
			database = database,
			diets = database.dietDao(),
			meals = database.mealDao(),
			courses = database.courseDao(),
			foodItems = database.foodItemDao(),
		)
		val shoppingRepo = RoomShoppingListRepository(
			database = database,
			shoppingLists = database.shoppingListDao(),
			diets = database.dietDao(),
			meals = database.mealDao(),
		)
		val configRepo = RoomConfigRepository(
			database = database,
			config = database.configDao(),
			diets = database.dietDao(),
			shoppingLists = database.shoppingListDao(),
		)

		repository = CompositeDietRepository(dietRepo, shoppingRepo, configRepo)
	}

	@After
	fun tearDown() {
		database.close()
	}

	@Test
	fun testCreateDiet() = runBlocking {
		val dietId = repository.create("Dieta Test", 60)
		assertTrue(dietId > 0)

		val activeDiet = repository.active.first()
		assertNotNull(activeDiet)
		assertEquals("Dieta Test", activeDiet?.name)
		assertTrue(activeDiet?.isActive == true)
	}

	@Test
	fun testSaveAndGetMeals() = runBlocking {
		val dietId = repository.create("Dieta Test", 60)
		val meal = Meal(
			dietId = dietId,
			dayOfWeek = DayOfWeek.MONDAY,
			type = MealType.LUNCH,
			timeMinutes = 13 * 60,
			description = "Pranzo test",
		)
		repository.saveMeal(meal, emptyList())

		val meals = repository.getMeals(dietId)
		assertTrue(meals.isNotEmpty())
	}

	@Test
	fun testDefaultTimes() = runBlocking {
		repository.saveDefaultTime(MealType.BREAKFAST, 450)
		val times = repository.defaultTimes.first()
		assertEquals(1, times.size)
		assertEquals(MealType.BREAKFAST, times[0].type)
		assertEquals(450, times[0].timeMinutes)
	}

	@Test
	fun offlineDelegatesReadRemoteDataAndKeepsLocalConfigWritable() = runBlocking {
		repository.create("Cached diet", 75)
		repository.createShoppingList("Cached shopping list")
		repository.saveConfig(ConfigKey.THEME, "dark")
		repository.saveDefaultTime(MealType.LUNCH, 780)

		val onlineRepository = FakeDietRepository()
		val offlineDietRepository = DelegatingDietRepository(
			onlineRepository = onlineRepository,
			localCacheRepository = repository,
			isOfflineProvider = { true },
		)
		val offlineShoppingRepository = DelegatingShoppingListRepository(
			onlineRepository = onlineRepository,
			localCacheRepository = repository,
			isOfflineProvider = { true },
		)

		assertEquals(listOf("Cached diet"), offlineDietRepository.all.first().map { it.name })
		assertEquals(
			listOf("Cached shopping list"),
			offlineShoppingRepository.allShoppingLists.first().map { it.list.name },
		)
		assertEquals("dark", repository.observeConfig(ConfigKey.THEME).first()?.value)
		assertEquals(780, repository.defaultTimes.first().single().timeMinutes)

		val cachedDiet = offlineDietRepository.all.first().single()
		val cachedMeal = Meal(
			dietId = cachedDiet.id,
			dayOfWeek = DayOfWeek.MONDAY,
			type = MealType.LUNCH,
			timeMinutes = 12 * 60,
		)
		val shoppingList = offlineShoppingRepository.allShoppingLists.first().single()
		val shoppingItem = ShoppingListItem(
			id = 1,
			shoppingListId = shoppingList.list.id,
			name = "Blocked item",
		)

		assertOfflineWriteBlocked { offlineDietRepository.create("Blocked", 60) }
		assertOfflineWriteBlocked { offlineDietRepository.updateDiet(cachedDiet.copy(name = "Changed")) }
		assertOfflineWriteBlocked { offlineDietRepository.activate(cachedDiet.id) }
		assertOfflineWriteBlocked { offlineDietRepository.delete(cachedDiet.id) }
		assertOfflineWriteBlocked { offlineDietRepository.saveMeal(cachedMeal, emptyList()) }
		assertOfflineWriteBlocked { offlineDietRepository.deleteMeal(1) }
		assertOfflineWriteBlocked { offlineDietRepository.duplicate(cachedDiet.id) }
		assertOfflineWriteBlocked { offlineDietRepository.importJson("{}") }

		assertOfflineWriteBlocked { offlineShoppingRepository.createShoppingList("Blocked") }
		assertOfflineWriteBlocked { offlineShoppingRepository.updateShoppingListName(shoppingList.list.id, "Changed") }
		assertOfflineWriteBlocked { offlineShoppingRepository.deleteShoppingList(shoppingList.list.id) }
		assertOfflineWriteBlocked {
			offlineShoppingRepository.addShoppingListItem(shoppingList.list.id, "Blocked")
		}
		assertOfflineWriteBlocked { offlineShoppingRepository.updateShoppingListItem(shoppingItem) }
		assertOfflineWriteBlocked { offlineShoppingRepository.toggleShoppingListItemBought(1, true) }
		assertOfflineWriteBlocked { offlineShoppingRepository.toggleShoppingListItemDayBought(1, true) }
		assertOfflineWriteBlocked { offlineShoppingRepository.deleteShoppingListItem(1) }
		assertOfflineWriteBlocked {
			offlineShoppingRepository.addDietIngredientsToShoppingList(
				shoppingList.list.id,
				cachedDiet.id,
				listOf(DietImportItemConfig("apple", false)),
			)
		}

		repository.saveDefaultTime(MealType.LUNCH, 13 * 60)
		repository.saveConfig(ConfigKey.THEME, "light")

		assertEquals(listOf("Cached diet"), offlineDietRepository.all.first().map { it.name })
		assertEquals(
			listOf("Cached shopping list"),
			offlineShoppingRepository.allShoppingLists.first().map { it.list.name },
		)
		assertEquals("light", repository.observeConfig(ConfigKey.THEME).first()?.value)
		assertEquals(13 * 60, repository.defaultTimes.first().single().timeMinutes)
	}

	@Test
	fun testDuplicateDiet() = runBlocking {
		val dietId = repository.create("Originale", 30)
		val duplicateId = repository.duplicate(dietId)
		assertTrue(duplicateId > 0)

		val meals = repository.getMeals(duplicateId)
		assertNotNull(meals)
	}

	@Test
	fun testActivateAndDeleteDiet() = runBlocking {
		val id1 = repository.create("Dieta 1", 30)
		val id2 = repository.create("Dieta 2", 30)

		repository.activate(id2)
		assertEquals(id2, repository.active.first()?.id)

		repository.delete(id2)
		assertEquals(id1, repository.active.first()?.id)
	}

	@Test
	fun testShoppingListOperations() = runBlocking {
		val listId = repository.createShoppingList("Spesa settimanale")
		assertTrue(listId > 0)

		val lists = repository.allShoppingLists.first()
		assertEquals(1, lists.size)
		assertEquals("Spesa settimanale", lists[0].list.name)

		repository.addShoppingListItem(listId, "Mela", "2", QuantityUnit.PIECES)
		val listWithItems = repository.observeShoppingList(listId).first()
		assertNotNull(listWithItems)
		assertEquals(1, listWithItems?.items?.size)
		assertEquals("Mela", listWithItems?.items?.get(0)?.item?.name)

		repository.toggleShoppingListItemBought(listWithItems!!.items[0].item.id, true)
		val updatedList = repository.observeShoppingList(listId).first()
		assertTrue(updatedList?.items?.get(0)?.item?.isBought == true)

		repository.deleteShoppingListItem(updatedList!!.items[0].item.id)
		val emptyList = repository.observeShoppingList(listId).first()
		assertTrue(emptyList?.items.isNullOrEmpty())

		repository.deleteShoppingList(listId)
		val noLists = repository.allShoppingLists.first()
		assertTrue(noLists.isEmpty())
	}

	@Test
	fun testResetDatabase() = runBlocking {
		repository.create("Dieta", 30)
		repository.saveDefaultTime(MealType.LUNCH, 780)
		repository.resetDatabase()

		assertTrue(repository.all.first().isEmpty())
		assertTrue(repository.defaultTimes.first().isEmpty())
	}

	private suspend fun assertOfflineWriteBlocked(action: suspend () -> Unit) {
		val exception = runCatching { action() }.exceptionOrNull()
		assertTrue("Expected offline write to be rejected, got $exception", exception is OfflineWriteException)
	}
}
