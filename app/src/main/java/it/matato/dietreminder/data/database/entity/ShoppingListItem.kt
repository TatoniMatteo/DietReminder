package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matato.dietreminder.data.model.QuantityUnit
import java.time.DayOfWeek

@Entity(
	tableName = "shopping_list_item",
	foreignKeys = [
		ForeignKey(
			entity = ShoppingList::class,
			parentColumns = ["id"],
			childColumns = ["shoppingListId"],
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [Index("shoppingListId")],
)
data class ShoppingListItem(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val shoppingListId: Long,
	val name: String,
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
	val isBought: Boolean = false,
	val isCustom: Boolean = false,
	val isFresh: Boolean = false,
	val dayOfWeek: DayOfWeek? = null,
) {
	val displayQuantity: String
		get() = unit.format(amount)

	val quantity: String
		get() = displayQuantity
}
