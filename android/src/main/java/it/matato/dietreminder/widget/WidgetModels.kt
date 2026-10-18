package it.matato.dietreminder.widget

import it.matato.dietreminder.common.model.FoodCategories
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.domain.NextMeal
import it.matato.dietreminder.domain.nextMeal
import java.time.LocalDateTime

/** A manual selection is local to one widget and expires, so the default remains useful. */
data class MealWidgetSelection(val mealId: Long, val scope: String, val selectedAt: Long, val date: String)
data class MealWidgetModel(val selected: NextMeal?, val today: List<MealWithDetails>, val browsing: Boolean) {
    fun adjacent(direction: Int): MealWithDetails? {
        if (today.isEmpty()) return null
        val index = today.indexOfFirst { it.meal.id == selected?.meal?.meal?.id }
        return today[if (index < 0) { if (direction < 0) today.lastIndex else 0 } else Math.floorMod(index + direction, today.size)]
    }
}
fun mealWidgetModel(meals: List<MealWithDetails>, now: LocalDateTime, window: Int, scope: String, selection: MealWidgetSelection? = null, epochMillis: Long = System.currentTimeMillis()): MealWidgetModel {
    val today = meals.filter { it.meal.dayOfWeek == now.dayOfWeek }.sortedWith(compareBy<MealWithDetails> { it.meal.timeMinutes }.thenBy { it.meal.id })
    val chosen = selection?.takeIf { it.scope == scope && it.date == now.toLocalDate().toString() && epochMillis - it.selectedAt in 0 until 15 * 60_000L }
        ?.let { choice -> today.find { it.meal.id == choice.mealId } }
    val selected = chosen?.let { NextMeal(it, now.toLocalDate().atTime(it.meal.timeMinutes / 60, it.meal.timeMinutes % 60), 0) }
        ?: nextMeal(meals, now, window)
    return MealWidgetModel(selected, today, chosen != null)
}

data class ShoppingWidgetEntry(val itemId: Long, val dayId: Long?, val name: String, val quantity: String, val category: String, val purchaseDay: Int?, val bought: Boolean) {
    val key: String get() = "$itemId:${dayId ?: 0}"
}
fun shoppingWidgetEntries(list: ShoppingListWithItems): List<ShoppingWidgetEntry> = list.items.flatMap { row ->
    val item = row.item
    val category = FoodCategories.classify(item.name, item.category)
    if (row.days.isEmpty()) listOf(ShoppingWidgetEntry(item.id, null, item.name, item.displayQuantity, category, (item.purchaseDayOfWeek ?: item.dayOfWeek)?.value, item.isBought))
    else row.days.map { day -> ShoppingWidgetEntry(item.id, day.id, item.name, day.displayQuantity, category, (day.purchaseDayOfWeek ?: day.dayOfWeek).value, day.isBought) }
}.sortedWith(compareBy<ShoppingWidgetEntry> { it.bought }.thenBy { it.purchaseDay ?: 8 }.thenBy { it.category }.thenBy { it.name }.thenBy { it.key })

fun selectWidgetList(lists: List<ShoppingListWithItems>, selectedId: Long?, activeDietId: Long?): ShoppingListWithItems? =
    lists.find { it.list.id == selectedId }
        ?: lists.firstOrNull { it.list.dietId == activeDietId && it.items.any { row -> if (row.days.isEmpty()) !row.item.isBought else row.days.any { !it.isBought } } }
        ?: lists.firstOrNull { it.items.any { row -> if (row.days.isEmpty()) !row.item.isBought else row.days.any { !it.isBought } } }
        ?: lists.firstOrNull()
