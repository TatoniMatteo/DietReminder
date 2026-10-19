package it.matato.dietreminder.common.model

/** Missing quantities for fresh products refer to their daily allocations, not the parent row. */
object ShoppingPlanning {
    fun pendingTomorrow(snapshot: WorkspaceSnapshot, listId: Long, tomorrow: Int): List<String> {
        val list = snapshot.lists.find { it.id == listId } ?: return emptyList()
        val meals = snapshot.meals.filter { it.dietId == list.dietId && it.dayOfWeek == tomorrow }.map { it.id }.toSet()
        val courses = snapshot.courses.filter { it.mealId in meals }.map { it.id }.toSet()
        val neededNames = snapshot.foods.filter { it.courseId in courses }.map { FoodCategories.normalize(it.name) }.toSet()
        return snapshot.items.filter { it.shoppingListId == listId }.filter { item ->
            val days = snapshot.days.filter { it.shoppingListItemId == item.id }
            if (days.isNotEmpty()) days.any { it.dayOfWeek == tomorrow && !it.isBought }
            else !item.isBought && (item.dayOfWeek == tomorrow || (item.dayOfWeek == null && FoodCategories.normalize(item.name) in neededNames))
        }.map { it.name }.distinct()
    }
}
