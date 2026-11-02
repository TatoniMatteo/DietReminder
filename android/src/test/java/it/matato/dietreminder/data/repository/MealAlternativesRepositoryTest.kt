package it.matato.dietreminder.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.export.*
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.room.RoomDietRepository
import it.matato.dietreminder.data.repository.room.RoomShoppingListRepository
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.sync.WorkspaceCache
import it.matato.dietreminder.domain.*
import it.matato.dietreminder.widget.widgetMealCourseNames
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=TestDietApplication::class)
class MealAlternativesRepositoryTest {
    @Test fun alternativesSurviveImportSelectionSyncDuplicationExportAndDriveShoppingAndWidget() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = RoomDietRepository(db,db.dietDao(),db.mealDao(),db.courseDao(),db.foodItemDao())
            val codec = DietJsonCodec()
            val data = DietExport(formatVersion=2,name="Piano",meals=listOf(MealExport(DayOfWeek.MONDAY,MealType.LUNCH,"12:00",courses=listOf(
                CourseExport("Riso",alternativeGroup="main",description="Bollire",items=listOf(FoodItemExport("Riso","80"))),
                CourseExport("Pasta",alternativeGroup="main",items=listOf(FoodItemExport("Pasta","100"))),
                CourseExport("Pane",items=listOf(FoodItemExport("Pane","50"))),
            ))))
            repo.importJson(codec.encode(data))
            val first = WorkspaceCache(db).read()
            first.requireValid()
            val dietId = first.diets.single().id
            val meal = repo.getMeals(dietId).single()
            assertEquals(listOf("Riso / Pasta","Pane"),widgetMealCourseNames(meal))
            val selectedIds = setOf(meal.courses[1].course.id)
            assertEquals(setOf("Pasta","Pane"),listOf(meal).toIngredientSummaries(selectedIds).map { it.name }.toSet())
            assertEquals(listOf("Riso / Pasta","Pane"),widgetMealCourseNames(meal))
            val snapshot = WorkspaceCache(db).read()
            WorkspaceCache(db).replace(snapshot)
            assertEquals(snapshot,WorkspaceCache(db).read())
            val duplicated = repo.duplicate(dietId)
            val export = codec.decode(repo.exportJson(duplicated))
            assertEquals(2,export.formatVersion)
            assertEquals("Bollire",export.meals.single().courses.first().description)
            assertEquals("main",export.meals.single().courses.first().alternativeGroup)
            val shopping = RoomShoppingListRepository(db,db.shoppingListDao(),db.dietDao(),db.mealDao())
            val listId = shopping.createShoppingList("Spesa",dietId,emptyList())
            shopping.addDietIngredientsToShoppingList(listId,dietId,listOf(DietImportItemConfig("Pasta",false,emptySet(),selectedIds),DietImportItemConfig("Riso",false,emptySet(),selectedIds)))
            assertEquals(listOf("Pasta"),WorkspaceCache(db).read().items.map { it.name })
            assertEquals("100",WorkspaceCache(db).read().items.single().amount)
        } finally { db.close() }
    }
}
