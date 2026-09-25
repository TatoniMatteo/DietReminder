package it.matato.dietreminder.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.model.MealType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
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
            config = database.configDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createAndObserveDiets() = runBlocking {
        assertTrue(repository.all.first().isEmpty())

        val dietId = repository.create("Dieta Mediterranea", 60)
        val allDiets = repository.all.first()

        assertEquals(1, allDiets.size)
        assertEquals("Dieta Mediterranea", allDiets[0].name)
        assertEquals(dietId, allDiets[0].id)
    }

    @Test
    fun activateDiet() = runBlocking {
        val diet1Id = repository.create("Dieta 1", 60)
        val diet2Id = repository.create("Dieta 2", 90)

        repository.activate(diet1Id)
        assertEquals(diet1Id, repository.active.first()?.id)

        repository.activate(diet2Id)
        assertEquals(diet2Id, repository.active.first()?.id)
    }

    @Test
    fun deleteDiet() = runBlocking {
        val dietId = repository.create("Dieta Temporanea", 60)
        assertEquals(1, repository.all.first().size)

        repository.delete(dietId)
        assertTrue(repository.all.first().isEmpty())
    }

    @Test
    fun saveAndGetMeals() = runBlocking {
        val dietId = repository.create("Dieta Test", 60)
        val meal = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.LUNCH,
            timeMinutes = 13 * 60,
            description = "Pasta al Pesto"
        )

        repository.saveMeal(meal, emptyList())

        val meals = repository.getMeals(dietId)
        assertEquals(1, meals.size)
        assertEquals("Pasta al Pesto", meals[0].meal.description)
    }

    @Test
    fun deleteMeal() = runBlocking {
        val dietId = repository.create("Dieta Test", 60)
        val meal = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.DINNER,
            timeMinutes = 20 * 60,
            description = "Insalata"
        )

        repository.saveMeal(meal, emptyList())
        val savedMealId = repository.getMeals(dietId)[0].meal.id

        repository.deleteMeal(savedMealId)
        assertTrue(repository.getMeals(dietId).isEmpty())
    }

    @Test
    fun saveAndObserveConfig() = runBlocking {
        repository.saveConfig(ConfigKey.THEME, "dark")
        val config = repository.observeConfig(ConfigKey.THEME).first()

        assertNotNull(config)
        assertEquals("dark", config?.value)
    }

    @Test
    fun duplicateDiet() = runBlocking {
        val originalId = repository.create("Dieta Base", 60)
        val meal = Meal(
            dietId = originalId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.BREAKFAST,
            timeMinutes = 8 * 60,
            description = "Colazione"
        )
        repository.saveMeal(meal, emptyList())

        val duplicatedId = repository.duplicate(originalId)

        val allDiets = repository.all.first()
        assertEquals(2, allDiets.size)

        val duplicatedMeals = repository.getMeals(duplicatedId)
        assertEquals(1, duplicatedMeals.size)
        assertEquals("Colazione", duplicatedMeals[0].meal.description)
    }
}
