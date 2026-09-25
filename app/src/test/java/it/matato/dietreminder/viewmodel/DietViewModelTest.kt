package it.matato.dietreminder.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.DietJsonCodec
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.FakeDietRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class DietViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeDietRepository
    private lateinit var viewModel: DietViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        repository = FakeDietRepository()
        viewModel = DietViewModel(context, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createDiet_addsDietToRepository() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }

        assertTrue(viewModel.diets.value.isEmpty())

        viewModel.create("Dieta Ipocalorica", 60)
        advanceUntilIdle()

        assertEquals(1, viewModel.diets.value.size)
        assertEquals("Dieta Ipocalorica", viewModel.diets.value[0].name)
        assertEquals(60, viewModel.diets.value[0].nextMealWindowMinutes)
        assertTrue(viewModel.diets.value[0].isActive)
    }

    @Test
    fun activateDiet_changesActiveDiet() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }
        backgroundScope.launch { viewModel.active.collect {} }

        viewModel.create("Dieta 1", 60)
        viewModel.create("Dieta 2", 90)
        advanceUntilIdle()

        val diet1Id = viewModel.diets.value[0].id
        val diet2Id = viewModel.diets.value[1].id

        assertEquals(diet1Id, viewModel.active.value?.id)

        viewModel.activate(diet2Id)
        advanceUntilIdle()

        assertEquals(diet2Id, viewModel.active.value?.id)
    }

    @Test
    fun deleteDiet_removesDietFromRepository() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }

        viewModel.create("Dieta da Eliminare", 60)
        advanceUntilIdle()

        assertEquals(1, viewModel.diets.value.size)

        val dietId = viewModel.diets.value[0].id
        viewModel.deleteDiet(dietId)
        advanceUntilIdle()

        assertTrue(viewModel.diets.value.isEmpty())
    }

    @Test
    fun duplicateDiet_createsCopyOfDietAndMeals() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }

        viewModel.create("Dieta Originale", 45)
        advanceUntilIdle()

        val originalId = viewModel.diets.value[0].id

        val meal = Meal(
            dietId = originalId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.LUNCH,
            timeMinutes = 13 * 60,
            description = "Pranzo test"
        )
        viewModel.saveMeal(meal, emptyList())
        advanceUntilIdle()

        viewModel.duplicate(originalId)
        advanceUntilIdle()

        assertEquals(2, viewModel.diets.value.size)
        val duplicated = viewModel.diets.value.find { it.name == "Dieta Originale copia" }
        assertNotNull(duplicated)

        val duplicatedMeals = repository.getMeals(duplicated!!.id)
        assertEquals(1, duplicatedMeals.size)
        assertEquals("Pranzo test", duplicatedMeals[0].meal.description)
    }

    @Test
    fun saveMealAndGetMeal_worksCorrectly() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }
        backgroundScope.launch { viewModel.active.collect {} }
        backgroundScope.launch { viewModel.meals.collect {} }

        viewModel.create("Dieta Test", 60)
        advanceUntilIdle()

        val dietId = viewModel.diets.value[0].id

        val meal = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.TUESDAY,
            type = MealType.BREAKFAST,
            timeMinutes = 8 * 60,
            description = "Colazione Sana"
        )

        viewModel.saveMeal(meal, emptyList())
        advanceUntilIdle()

        val mealsForDiet = repository.getMeals(dietId)
        assertEquals(1, mealsForDiet.size)
        assertEquals("Colazione Sana", mealsForDiet[0].meal.description)

        val fetchedMeal = viewModel.getMeal(mealsForDiet[0].meal.id)
        assertNotNull(fetchedMeal)
        assertEquals("Colazione Sana", fetchedMeal?.meal?.description)
    }

    @Test
    fun deleteMeal_removesMealFromDiet() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }

        viewModel.create("Dieta Test", 60)
        advanceUntilIdle()

        val dietId = viewModel.diets.value[0].id

        val meal = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.WEDNESDAY,
            type = MealType.DINNER,
            timeMinutes = 20 * 60,
            description = "Cena Leggera"
        )
        viewModel.saveMeal(meal, emptyList())
        advanceUntilIdle()

        val mealId = repository.getMeals(dietId)[0].meal.id
        viewModel.deleteMeal(mealId)
        advanceUntilIdle()

        assertTrue(repository.getMeals(dietId).isEmpty())
    }

    @Test
    fun updateSettings_updatesViewModelState() = runTest {
        backgroundScope.launch { viewModel.theme.collect {} }
        backgroundScope.launch { viewModel.useDynamicColors.collect {} }
        backgroundScope.launch { viewModel.seedColor.collect {} }
        backgroundScope.launch { viewModel.language.collect {} }
        backgroundScope.launch { viewModel.isDeveloperMode.collect {} }
        backgroundScope.launch { viewModel.mealRemindersEnabled.collect {} }
        backgroundScope.launch { viewModel.hydrationEnabled.collect {} }
        backgroundScope.launch { viewModel.hydrationInterval.collect {} }
        backgroundScope.launch { viewModel.hydrationRanges.collect {} }

        viewModel.setTheme("dark")
        advanceUntilIdle()
        assertEquals("dark", viewModel.theme.value)

        viewModel.setUseDynamicColors(false)
        advanceUntilIdle()
        assertFalse(viewModel.useDynamicColors.value)

        viewModel.setSeedColor("0xFF00FF00")
        advanceUntilIdle()
        assertEquals("0xFF00FF00", viewModel.seedColor.value)

        viewModel.setLanguage("en")
        advanceUntilIdle()
        assertEquals("en", viewModel.language.value)

        viewModel.setDeveloperMode(true)
        advanceUntilIdle()
        assertTrue(viewModel.isDeveloperMode.value)

        viewModel.setMealRemindersEnabled(false)
        advanceUntilIdle()
        assertFalse(viewModel.mealRemindersEnabled.value)

        viewModel.setHydrationEnabled(false)
        advanceUntilIdle()
        assertFalse(viewModel.hydrationEnabled.value)

        viewModel.setHydrationInterval(90)
        advanceUntilIdle()
        assertEquals(90, viewModel.hydrationInterval.value)

        val customRanges = listOf(HydrationRange(startMinutes = 9 * 60, endMinutes = 21 * 60))
        viewModel.setHydrationRanges(customRanges)
        advanceUntilIdle()
        assertEquals(customRanges, viewModel.hydrationRanges.value)
    }

    @Test
    fun checkImportConflict_detectsValidAndConflict() = runTest {
        backgroundScope.launch { viewModel.diets.collect {} }

        val codec = DietJsonCodec()
        val dietExport = DietExport(
            uuid = "unique-uuid-1",
            name = "Dieta Importata",
            nextMealWindowMinutes = 60,
            meals = emptyList()
        )
        val json = codec.encode(dietExport)

        val result1 = viewModel.checkImportConflict(json)
        assertEquals(ImportCheckResult.Valid, result1)

        viewModel.importDiet(json)
        advanceUntilIdle()

        val result2 = viewModel.checkImportConflict(json)
        assertEquals(ImportCheckResult.Conflict, result2)
    }
}
