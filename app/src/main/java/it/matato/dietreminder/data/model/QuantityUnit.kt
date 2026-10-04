package it.matato.dietreminder.data.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R
import kotlinx.serialization.Serializable

@Serializable
enum class QuantityUnit(
	@StringRes val symbolRes: Int,
	@StringRes val singularSymbolRes: Int = symbolRes,
	@StringRes val labelRes: Int,
	val isNoAmountNeeded: Boolean = false,
) {
	GRAMS(R.string.unit_g_symbol, R.string.unit_g_symbol, R.string.unit_g),
	KILOGRAMS(R.string.unit_kg_symbol, R.string.unit_kg_symbol, R.string.unit_kg),
	MILLIGRAMS(R.string.unit_mg_symbol, R.string.unit_mg_symbol, R.string.unit_mg),

	MILLILITERS(R.string.unit_ml_symbol, R.string.unit_ml_symbol, R.string.unit_ml),
	CENTILITERS(R.string.unit_cl_symbol, R.string.unit_cl_symbol, R.string.unit_cl),
	DECILITERS(R.string.unit_dl_symbol, R.string.unit_dl_symbol, R.string.unit_dl),
	LITERS(R.string.unit_l_symbol, R.string.unit_l_symbol, R.string.unit_l),

	PIECES(R.string.unit_pz_symbol, R.string.unit_pz_singular_symbol, R.string.unit_pz),
	SLICES(R.string.unit_slices_symbol, R.string.unit_slices_singular_symbol, R.string.unit_slices),
	CLOVES(R.string.unit_cloves_symbol, R.string.unit_cloves_singular_symbol, R.string.unit_cloves),
	SPOONS(R.string.unit_spoons_symbol, R.string.unit_spoons_singular_symbol, R.string.unit_spoons),
	TEASPOONS(R.string.unit_teaspoons_symbol, R.string.unit_teaspoons_singular_symbol, R.string.unit_teaspoons),
	CUPS(R.string.unit_cups_symbol, R.string.unit_cups_singular_symbol, R.string.unit_cups),
	GLASSES(R.string.unit_glasses_symbol, R.string.unit_glasses_singular_symbol, R.string.unit_glasses),
	PINCHES(R.string.unit_pinches_symbol, R.string.unit_pinches_singular_symbol, R.string.unit_pinches),
	JARS(R.string.unit_jars_symbol, R.string.unit_jars_singular_symbol, R.string.unit_jars),
	CANS(R.string.unit_cans_symbol, R.string.unit_cans_singular_symbol, R.string.unit_cans),
	PACKETS(R.string.unit_packets_symbol, R.string.unit_packets_singular_symbol, R.string.unit_packets),
	LEAVES(R.string.unit_leaves_symbol, R.string.unit_leaves_singular_symbol, R.string.unit_leaves),
	SPRIGS(R.string.unit_sprigs_symbol, R.string.unit_sprigs_singular_symbol, R.string.unit_sprigs),
	DROPS(R.string.unit_drops_symbol, R.string.unit_drops_singular_symbol, R.string.unit_drops),
	PORTIONS(R.string.unit_portions_symbol, R.string.unit_portions_singular_symbol, R.string.unit_portions),

	QB(R.string.unit_qb_symbol, R.string.unit_qb_symbol, R.string.unit_qb, isNoAmountNeeded = true),
	FREE(R.string.unit_free_symbol, R.string.unit_free_symbol, R.string.unit_free, isNoAmountNeeded = true),

	CUSTOM(0, 0, R.string.unit_custom);

	fun getSymbol(context: Context? = null, isSingular: Boolean = false): String {
		val resId = if (isSingular) singularSymbolRes else symbolRes
		if ((context != null) && (resId != 0)) {
			return context.getString(resId)
		}
		return fallbackSymbol(isSingular)
	}

	fun format(amount: String, context: Context? = null): String {
		if (isNoAmountNeeded) {
			return getSymbol(context)
		}
		val cleanAmount = amount.trim()
		if (cleanAmount.isEmpty()) return ""
		val num = cleanAmount.replace(',', '.').toDoubleOrNull()
		val isSingular = (num == 1.0)
		val sym = getSymbol(context, isSingular)
		val effectiveSymbol = sym.ifEmpty { GRAMS.getSymbol(context) }
		return "$cleanAmount $effectiveSymbol".trim()
	}

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
		fun fromSymbol(symbol: String?, context: Context? = null): QuantityUnit {
			if (symbol.isNullOrBlank()) return CUSTOM
			val s = symbol.trim().lowercase()
			return entries.find { unit ->
				unit.name.equals(s, ignoreCase = true) ||
						unit.fallbackSymbol(isSingular = false).equals(s, ignoreCase = true) ||
						unit.fallbackSymbol(isSingular = true).equals(s, ignoreCase = true) ||
						(context != null && (
								(unit.symbolRes != 0 && context.getString(unit.symbolRes)
									.equals(s, ignoreCase = true)) ||
										(unit.singularSymbolRes != 0 && context.getString(unit.singularSymbolRes)
											.equals(s, ignoreCase = true))
								))
			} ?: CUSTOM
		}
	}
}

@Composable
fun QuantityUnit.localizedSymbol(isSingular: Boolean = false): String {
	val resId = if (isSingular) singularSymbolRes else symbolRes
	return if (resId != 0) stringResource(resId) else fallbackSymbol(isSingular)
}
