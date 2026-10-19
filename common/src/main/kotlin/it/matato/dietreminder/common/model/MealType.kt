package it.matato.dietreminder.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MealType {

	@SerialName("BREAKFAST")
	BREAKFAST,

	@SerialName("MORNING_SNACK")
	MORNING_SNACK,

	@SerialName("LUNCH")
	LUNCH,

	@SerialName("AFTERNOON_SNACK")
	AFTERNOON_SNACK,

	@SerialName("DINNER")
	DINNER,

	@SerialName("OTHER")
	OTHER
}
