package it.matato.dietreminder.widget

import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import kotlinx.coroutines.flow.first

/** Uses a pinned repository and rechecks permissions after the read suspension. */
internal suspend fun applyWidgetPurchase(repository: ShoppingListRepository, listId: Long, itemId: Long, dayId: Long?, bought: Boolean, isCurrent: () -> Boolean, writesBlocked: () -> Boolean): Boolean {
    if (!isCurrent() || writesBlocked()) return false
    val entry = repository.observeShoppingList(listId).first()?.let(::shoppingWidgetEntries)?.find { it.itemId == itemId && it.dayId == dayId } ?: return false
    if (!isCurrent() || writesBlocked()) return false
    if (entry.bought == bought) return true
    if (dayId == null) repository.toggleShoppingListItemBought(itemId, bought) else repository.toggleShoppingListItemDayBought(dayId, bought)
    return true
}
