package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuantityUnit(
	val isNoAmountNeeded: Boolean = false,
) {
	GRAMS(false),
	KILOGRAMS(false),
	MILLIGRAMS(false),

	MILLILITERS(false),
	CENTILITERS(false),
	DECILITERS(false),
	LITERS(false),

	PIECES(false),
	SLICES(false),
	CLOVES(false),
	SPOONS(false),
	TEASPOONS(false),
	CUPS(false),
	GLASSES(false),
	PINCHES(false),
	JARS(false),
	CANS(false),
	PACKETS(false),
	LEAVES(false),
	SPRIGS(false),
	DROPS(false),
	PORTIONS(false),

	QB(true),
	FREE(true),

	CUSTOM(false);

	fun fallbackSymbol(isSingular: Boolean = false): String {
		return when (this) {
			GRAMS -> "g"
			KILOGRAMS -> "kg"
			MILLIGRAMS -> "mg"
			MILLILITERS -> "ml"
			CENTILITERS -> "cl"
			DECILITERS -> "dl"
			LITERS -> "l"
			PIECES -> "pz"
			SLICES -> if (isSingular) "fetta" else "fette"
			CLOVES -> if (isSingular) "spicchio" else "spicchi"
			SPOONS -> if (isSingular) "cucchiaio" else "cucchiai"
			TEASPOONS -> if (isSingular) "cucchiaino" else "cucchiaini"
			CUPS -> if (isSingular) "tazza" else "tazze"
			GLASSES -> if (isSingular) "bicchiere" else "bicchieri"
			PINCHES -> if (isSingular) "pizzico" else "pizzichi"
			JARS -> if (isSingular) "vasetto" else "vasetti"
			CANS -> if (isSingular) "lattina" else "lattine"
			PACKETS -> if (isSingular) "busta" else "buste"
			LEAVES -> if (isSingular) "foglia" else "foglie"
			SPRIGS -> if (isSingular) "rametto" else "rametti"
			DROPS -> if (isSingular) "goccia" else "gocce"
			PORTIONS -> if (isSingular) "porzione" else "porzioni"
			QB -> "q.b."
			FREE -> "a piacere"
			CUSTOM -> ""
		}
	}

	companion object {
		fun fromSymbol(symbol: String?): QuantityUnit {
			if (symbol.isNullOrBlank()) return CUSTOM
			val s = symbol.trim().lowercase()
			return entries.find { unit ->
				unit.name.equals(s, ignoreCase = true) ||
						unit.fallbackSymbol(isSingular = false).equals(s, ignoreCase = true) ||
						unit.fallbackSymbol(isSingular = true).equals(s, ignoreCase = true)
			} ?: CUSTOM
		}
	}
}
