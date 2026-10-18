package it.matato.dietreminder.domain

import android.content.Context
import it.matato.dietreminder.data.model.QuantityUnit
import java.util.Locale

data class QuantityValue(
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
)

data class AggregatedQuantity(
	val amount: String,
	val unit: QuantityUnit,
	val formatted: String,
)

object QuantityAggregator {

	fun aggregateOccurrences(occurrences: List<IngredientOccurrence>, context: Context? = null): AggregatedQuantity {
		val values = occurrences.map { QuantityValue(amount = it.amount, unit = it.unit) }
		return aggregateValues(values, context)
	}

	fun aggregateValues(values: List<QuantityValue>, context: Context? = null): AggregatedQuantity {
		val validValues =
			values.filter { it.amount.isNotBlank() || it.unit != QuantityUnit.CUSTOM || it.unit.isNoAmountNeeded }
		if (validValues.isEmpty()) return AggregatedQuantity("", QuantityUnit.GRAMS, "")
		if (validValues.size == 1) {
			val single = validValues.first()
			return AggregatedQuantity(single.amount, single.unit, single.unit.format(single.amount, context))
		}

		var sumGrams = 0.0
		var hasGrams = false

		var sumMl = 0.0
		var hasMl = false

		val unitCounts = mutableMapOf<QuantityUnit, Double>()

		for (valItem in validValues) {
			val unitObj = valItem.unit
			if (unitObj.isNoAmountNeeded) {
				return AggregatedQuantity(
					"",
					unitObj,
					unitObj.getSymbol(context ?: return AggregatedQuantity("", unitObj, unitObj.fallbackSymbol(false))))
			}

			val num = valItem.amount.replace(',', '.').toDoubleOrNull()
			if (num != null) {
				when (unitObj) {
					QuantityUnit.GRAMS -> {
						sumGrams += num
						hasGrams = true
					}

					QuantityUnit.KILOGRAMS -> {
						sumGrams += num * 1000.0
						hasGrams = true
					}

					QuantityUnit.MILLIGRAMS -> {
						sumGrams += num / 1000.0
						hasGrams = true
					}

					QuantityUnit.MILLILITERS -> {
						sumMl += num
						hasMl = true
					}

					QuantityUnit.CENTILITERS -> {
						sumMl += num * 10.0
						hasMl = true
					}

					QuantityUnit.DECILITERS -> {
						sumMl += num * 100.0
						hasMl = true
					}

					QuantityUnit.LITERS -> {
						sumMl += num * 1000.0
						hasMl = true
					}

					else -> {
						unitCounts[unitObj] = (unitCounts[unitObj] ?: 0.0) + num
					}
				}
			}
		}

		if (hasGrams) {
			val isKg = sumGrams >= 1000.0
			val amountVal = if (isKg) sumGrams / 1000.0 else sumGrams
			val amountStr =
				if (amountVal % 1.0 == 0.0) "${amountVal.toInt()}" else "%.1f".format(Locale.ROOT, amountVal)
			val unitObj = if (isKg) QuantityUnit.KILOGRAMS else QuantityUnit.GRAMS
			return AggregatedQuantity(amountStr, unitObj, unitObj.format(amountStr, context))
		}

		if (hasMl) {
			val isL = sumMl >= 1000.0
			val amountVal = if (isL) sumMl / 1000.0 else sumMl
			val amountStr =
				if (amountVal % 1.0 == 0.0) "${amountVal.toInt()}" else "%.1f".format(Locale.ROOT, amountVal)
			val unitObj = if (isL) QuantityUnit.LITERS else QuantityUnit.MILLILITERS
			return AggregatedQuantity(amountStr, unitObj, unitObj.format(amountStr, context))
		}

		val firstPair = unitCounts.entries.firstOrNull()
		if (firstPair != null) {
			val amountStr = if (firstPair.value % 1.0 == 0.0) "${firstPair.value.toInt()}" else "%.1f".format(
				Locale.ROOT,
				firstPair.value)
			return AggregatedQuantity(amountStr, firstPair.key, firstPair.key.format(amountStr, context))
		}

		val firstValid = validValues.first()
		return AggregatedQuantity(
			firstValid.amount,
			firstValid.unit,
			firstValid.unit.format(firstValid.amount, context))
	}

	fun sumOccurrences(occurrences: List<IngredientOccurrence>, context: Context? = null): String {
		return aggregateOccurrences(occurrences, context).formatted
	}

	fun sumValues(values: List<QuantityValue>, context: Context? = null): String {
		return aggregateValues(values, context).formatted
	}
}
