package it.matato.dietreminder.data.export

import android.content.Context
import androidx.annotation.StringRes
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.QuantityUnit
import kotlinx.serialization.json.Json

sealed class DietValidationException(
	@StringRes val messageRes: Int,
	val formatArgs: Array<out Any> = emptyArray(),
) : IllegalArgumentException() {
	class BlankJson : DietValidationException(R.string.import_error_blank_json)
	class BlankDietName : DietValidationException(R.string.import_error_blank_diet_name)
	class InvalidMealTime(time: String) :
		DietValidationException(R.string.import_error_invalid_meal_time, arrayOf(time))

	class BlankFoodName(courseName: String) :
		DietValidationException(R.string.import_error_blank_food_name, arrayOf(courseName))

	class InvalidAmount(amount: String, itemName: String) :
		DietValidationException(R.string.import_error_invalid_amount, arrayOf(amount, itemName))

	class UnitRequiresAmount(unitName: String, itemName: String) :
		DietValidationException(R.string.import_error_unit_requires_amount, arrayOf(unitName, itemName))

	class MalformedJson : DietValidationException(R.string.import_error_malformed_json)
}

fun Throwable.getLocalizedImportError(context: Context): String {
	return when (this) {
		is DietValidationException -> context.getString(messageRes, *formatArgs)
		is kotlinx.serialization.SerializationException -> context.getString(R.string.import_error_malformed_json)
		is IllegalArgumentException -> localizedMessage ?: context.getString(R.string.import_error_malformed_json)
		else -> localizedMessage ?: context.getString(R.string.import_error_malformed_json)
	}
}

class DietJsonCodec {

	private val json = Json {
		ignoreUnknownKeys = true
		coerceInputValues = true
		isLenient = false
	}

	fun encode(diet: DietExport): String =
		json.encodeToString(diet)

	fun decode(jsonString: String): DietExport {
		val trimmed = jsonString.trim()
		if (trimmed.isBlank()) {
			throw DietValidationException.BlankJson()
		}
		val export = try {
			this.json.decodeFromString<DietExport>(trimmed)
		} catch (e: kotlinx.serialization.SerializationException) {
			throw DietValidationException.MalformedJson()
		} catch (e: Exception) {
			throw DietValidationException.MalformedJson()
		}
		validate(export)
		return export
	}

	private fun validate(export: DietExport) {
		if (export.name.isBlank()) {
			throw DietValidationException.BlankDietName()
		}
		if ((export.nextMealWindowMinutes < 1) || (export.nextMealWindowMinutes > 1440)) {
			throw DietValidationException.BlankDietName()
		}
		export.meals.forEach { meal ->
			if (!meal.time.matches(TIME_REGEX)) {
				throw DietValidationException.InvalidMealTime(meal.time)
			}
			meal.courses.forEach { course ->
				course.items.forEach { item ->
					if (item.name.isBlank()) {
						throw DietValidationException.BlankFoodName(course.name.ifBlank { "Unassigned" })
					}

					val amountClean = item.amount.trim()
					val isAmountPresent = amountClean.isNotBlank()
					val unit = item.unit

					if (isAmountPresent) {
						val num = amountClean.replace(',', '.').toDoubleOrNull()
						if (num == null || num <= 0) {
							throw DietValidationException.InvalidAmount(item.amount, item.name)
						}
					} else {
						if (!unit.isNoAmountNeeded && unit != QuantityUnit.CUSTOM) {
							throw DietValidationException.UnitRequiresAmount(unit.name, item.name)
						}
					}
				}
			}
		}
	}

	companion object {
		private val TIME_REGEX = Regex("""^(?:[01]?\d|2[0-3]):[0-5]\d$""")
	}
}
