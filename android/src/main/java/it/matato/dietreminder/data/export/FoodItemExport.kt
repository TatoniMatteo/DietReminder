package it.matato.dietreminder.data.export

import it.matato.dietreminder.data.model.QuantityUnit
import kotlinx.serialization.Serializable

@Serializable
data class FoodItemExport(
	val name: String,
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
	val order: Int = 0,
) {
	val displayQuantity: String
		get() = unit.format(amount)
}
