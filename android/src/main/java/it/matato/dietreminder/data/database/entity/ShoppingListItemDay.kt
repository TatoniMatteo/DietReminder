package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matato.dietreminder.data.model.QuantityUnit
import java.time.DayOfWeek

@Entity(
	tableName = "shopping_list_item_day",
	foreignKeys = [
		ForeignKey(
			entity = ShoppingListItem::class,
			parentColumns = ["id"],
			childColumns = ["shoppingListItemId"],
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [Index("shoppingListItemId")],
)
data class ShoppingListItemDay(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val shoppingListItemId: Long,
	val dayOfWeek: DayOfWeek,
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
	val isBought: Boolean = false,
) {
	val displayQuantity: String
		get() = unit.format(amount)
}
