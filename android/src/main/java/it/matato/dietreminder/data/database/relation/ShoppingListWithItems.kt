package it.matato.dietreminder.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem

data class ShoppingListWithItems(
	@Embedded val list: ShoppingList,
	@Relation(
		entity = ShoppingListItem::class,
		parentColumn = "id",
		entityColumn = "shoppingListId",
	)
	val items: List<ShoppingListItemWithDays> = emptyList(),
)
