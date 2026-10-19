package it.matato.dietreminder.data.sync

import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.*
import java.time.DayOfWeek

/** Caller holds a Room transaction for coherent reads and atomic replacement. */
class WorkspaceCache(private val db: AppDatabase) {
    suspend fun read(): WorkspaceSnapshot {
        val diets = db.dietDao().getAllForSync()
        val dietIds = diets.map { it.id }.toSet()
        return WorkspaceSnapshot(
            diets = diets.map { CloudDiet(it.id, it.uuid, it.name, it.nextMealWindowMinutes, it.isActive, it.disabledNotificationDays) },
            meals = db.mealDao().getAllForSync().map { CloudMeal(it.id, it.dietId, it.dayOfWeek.value, it.type, it.timeMinutes, it.description, it.customTypeLabel, it.isNotificationEnabled) },
            courses = db.courseDao().getAllForSync().map { CloudCourse(it.id, it.mealId, it.name, it.order) },
            foods = db.foodItemDao().getAllForSync().map { CloudFood(it.id, it.courseId, it.name, it.amount, it.unit, it.order) },
            lists = db.shoppingListDao().listsForSync().map { CloudShoppingList(it.id, it.name, it.createdAt, it.dietId?.takeIf(dietIds::contains), it.remindersEnabled, it.reminderTimeMinutes) },
            items = db.shoppingListDao().itemsForSync().map { CloudShoppingItem(it.id, it.shoppingListId, it.name, it.amount, it.unit, it.isBought, it.isCustom, it.isFresh, it.dayOfWeek?.value, it.category, it.purchaseDayOfWeek?.value) },
            days = db.shoppingListDao().daysForSync().map { CloudShoppingDay(it.id, it.shoppingListItemId, it.dayOfWeek.value, it.amount, it.unit, it.isBought, it.purchaseDayOfWeek?.value) }
        )
    }
    suspend fun replace(snapshot: WorkspaceSnapshot) {
        snapshot.requireValid()
        db.shoppingListDao().deleteAllLists()
        db.dietDao().deleteAll()
        snapshot.diets.forEach { db.dietDao().insert(Diet(it.id, it.uuid, it.name, it.nextMealWindowMinutes, it.isActive, it.disabledNotificationDays)) }
        snapshot.meals.forEach { db.mealDao().insert(Meal(it.id, it.dietId, DayOfWeek.of(it.dayOfWeek), it.type, it.timeMinutes, it.description, it.customTypeLabel, it.isNotificationEnabled)) }
        snapshot.courses.forEach { db.courseDao().insert(Course(it.id, it.mealId, it.name, it.order)) }
        snapshot.foods.forEach { db.foodItemDao().insert(FoodItem(it.id, it.courseId, it.name, it.amount, it.unit, it.order)) }
        snapshot.lists.forEach { db.shoppingListDao().insertList(ShoppingList(it.id, it.name, it.createdAt, it.dietId, it.remindersEnabled, it.reminderTimeMinutes)) }
        snapshot.items.forEach { db.shoppingListDao().insertItem(ShoppingListItem(it.id, it.shoppingListId, it.name, it.amount, it.unit, it.isBought, it.isCustom, it.isFresh, it.dayOfWeek?.let(DayOfWeek::of), it.category, it.purchaseDayOfWeek?.let(DayOfWeek::of))) }
        db.shoppingListDao().insertItemDays(snapshot.days.map { ShoppingListItemDay(it.id, it.shoppingListItemId, DayOfWeek.of(it.dayOfWeek), it.amount, it.unit, it.isBought, it.purchaseDayOfWeek?.let(DayOfWeek::of)) })
    }
}
