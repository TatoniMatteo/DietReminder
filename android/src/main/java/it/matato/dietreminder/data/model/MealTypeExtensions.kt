package it.matato.dietreminder.data.model

import it.matato.dietreminder.R

val MealType.resId: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.MORNING_SNACK -> R.string.meal_morning_snack
        MealType.LUNCH -> R.string.meal_lunch
        MealType.AFTERNOON_SNACK -> R.string.meal_afternoon_snack
        MealType.DINNER -> R.string.meal_dinner
        MealType.OTHER -> R.string.meal_other
    }
