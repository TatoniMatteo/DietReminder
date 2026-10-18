package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matato.dietreminder.data.model.QuantityUnit

@Entity(
	tableName = "food_items",
	foreignKeys = [
		ForeignKey(
			entity = Course::class,
			parentColumns = ["id"],
			childColumns = ["courseId"],
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [Index("courseId")],
)
data class FoodItem(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val courseId: Long,
	val name: String,
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
	val order: Int = 0,
) {
	val displayQuantity: String
		get() = unit.format(amount)

	val quantities: String
		get() = displayQuantity
}
