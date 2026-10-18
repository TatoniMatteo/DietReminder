package it.matato.dietreminder.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay

data class ShoppingListItemWithDays(
	@Embedded val item: ShoppingListItem,
	@Relation(
		parentColumn = "id",
		entityColumn = "shoppingListItemId",
	)
	val days: List<ShoppingListItemDay> = emptyList(),
) {
	val isAllDaysBought: Boolean
		get() = if (item.isFresh && days.isNotEmpty()) days.all { it.isBought } else item.isBought

	val boughtDaysCount: Int
		get() = days.count { it.isBought }
}
