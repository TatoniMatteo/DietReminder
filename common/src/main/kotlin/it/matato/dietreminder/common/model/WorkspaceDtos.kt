package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

/** One account's shared data. Device preferences and notification registries stay local. */
@Serializable
data class WorkspaceSnapshot(
    val schemaVersion: Int = 2,
    val diets: List<CloudDiet> = emptyList(),
    val meals: List<CloudMeal> = emptyList(),
    val courses: List<CloudCourse> = emptyList(),
    val foods: List<CloudFood> = emptyList(),
    val lists: List<CloudShoppingList> = emptyList(),
    val items: List<CloudShoppingItem> = emptyList(),
    val days: List<CloudShoppingDay> = emptyList()
) {
    fun requireValid() {
        require(schemaVersion in 1..2) { "Unsupported workspace schema" }
        require(diets.size + meals.size + courses.size + foods.size + lists.size + items.size + days.size <= 50_000) { "Too many records" }
        if (schemaVersion == 1) require(lists.none { it.remindersEnabled || it.reminderTimeMinutes != 1080 } && items.none { it.category != null || it.purchaseDayOfWeek != null } && days.none { it.purchaseDayOfWeek != null }) { "Shopping planning requires schema version 2" }
        fun ids(values: List<Long>): Set<Long> {
            require(values.all { it in 1..1_000_000_000_000_000L } && values.distinct().size == values.size) { "Invalid or duplicate IDs" }
            return values.toSet()
        }
        fun text(value: String, limit: Int, blank: Boolean = true) {
            require(value.length <= limit && (blank || value.isNotBlank())) { "Invalid text field" }
        }
        val dietIds = ids(diets.map { it.id })
        val mealIds = ids(meals.map { it.id })
        val courseIds = ids(courses.map { it.id })
        ids(foods.map { it.id })
        val listIds = ids(lists.map { it.id })
        val itemIds = ids(items.map { it.id })
        ids(days.map { it.id })
        require(diets.map { it.uuid }.distinct().size == diets.size && diets.count { it.isActive } <= 1) { "Invalid diets" }
        diets.forEach {
            text(it.uuid, 128, false); text(it.name, 500, false)
            require(it.nextMealWindowMinutes in 0..10080) { "Invalid meal window" }
            require(it.disabledNotificationDays.isEmpty() || it.disabledNotificationDays.split(',').all { day -> day in dayNames }) { "Invalid notification days" }
        }
        meals.forEach {
            require(it.dietId in dietIds && it.dayOfWeek in 1..7 && it.timeMinutes in 0..1439) { "Invalid meal" }
            text(it.description, 10_000); it.customTypeLabel?.let { label -> text(label, 500) }
        }
        courses.forEach { require(it.mealId in mealIds) { "Missing meal" }; text(it.name, 500) }
        foods.forEach { require(it.courseId in courseIds) { "Missing course" }; text(it.name, 500, false); text(it.amount, 200) }
        lists.forEach { require(it.dietId == null || it.dietId in dietIds) { "Missing diet" }; text(it.name, 500, false); require(it.createdAt >= 0 && it.reminderTimeMinutes in 0..1439) }
        items.forEach { require(it.shoppingListId in listIds && (it.dayOfWeek == null || it.dayOfWeek in 1..7)) { "Invalid shopping item" }; text(it.name, 500, false); text(it.amount, 200); it.category?.let { category -> text(category, 80, false) }; require(it.purchaseDayOfWeek == null || it.purchaseDayOfWeek in 1..7) }
        days.forEach { require(it.shoppingListItemId in itemIds && it.dayOfWeek in 1..7) { "Invalid shopping day" }; text(it.amount, 200); require(it.purchaseDayOfWeek == null || it.purchaseDayOfWeek in 1..7) }
    }
    companion object {
        private val dayNames = setOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    }
}

@Serializable data class CloudDiet(val id: Long, val uuid: String, val name: String, val nextMealWindowMinutes: Int = 90, val isActive: Boolean = false, val disabledNotificationDays: String = "")
@Serializable data class CloudMeal(val id: Long, val dietId: Long, val dayOfWeek: Int, val type: MealType, val timeMinutes: Int, val description: String = "", val customTypeLabel: String? = null, val isNotificationEnabled: Boolean = true)
@Serializable data class CloudCourse(val id: Long, val mealId: Long, val name: String = "", val order: Int = 0)
@Serializable data class CloudFood(val id: Long, val courseId: Long, val name: String, val amount: String = "", val unit: QuantityUnit = QuantityUnit.GRAMS, val order: Int = 0)
@Serializable data class CloudShoppingList(val id: Long, val name: String, val createdAt: Long, val dietId: Long? = null, val remindersEnabled: Boolean = false, val reminderTimeMinutes: Int = 1080)
@Serializable data class CloudShoppingItem(val id: Long, val shoppingListId: Long, val name: String, val amount: String = "", val unit: QuantityUnit = QuantityUnit.GRAMS, val isBought: Boolean = false, val isCustom: Boolean = false, val isFresh: Boolean = false, val dayOfWeek: Int? = null, val category: String? = null, val purchaseDayOfWeek: Int? = null)
@Serializable data class CloudShoppingDay(val id: Long, val shoppingListItemId: Long, val dayOfWeek: Int, val amount: String = "", val unit: QuantityUnit = QuantityUnit.GRAMS, val isBought: Boolean = false, val purchaseDayOfWeek: Int? = null)
@Serializable data class WorkspaceDto(val version: Long, val snapshot: WorkspaceSnapshot)
@Serializable data class WorkspacePutRequest(val baseVersion: Long, val requestId: String, val snapshot: WorkspaceSnapshot)
@Serializable data class WorkspaceAck(val version: Long, val requestId: String)
