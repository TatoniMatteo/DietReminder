package it.matato.dietreminder.data.model

import it.matato.dietreminder.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MealType(val resId: Int) {

    @SerialName("BREAKFAST")
    BREAKFAST(R.string.meal_breakfast),

    @SerialName("MORNING_SNACK")
    MORNING_SNACK(R.string.meal_morning_snack),

    @SerialName("LUNCH")
    LUNCH(R.string.meal_lunch),

    @SerialName("AFTERNOON_SNACK")
    AFTERNOON_SNACK(R.string.meal_afternoon_snack),

    @SerialName("DINNER")
    DINNER(R.string.meal_dinner),

    @SerialName("OTHER")
    OTHER(R.string.meal_other)
}