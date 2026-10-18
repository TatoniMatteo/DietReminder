package it.matato.dietreminder.data.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R

val QuantityUnit.symbolRes: Int
	get() = when (this) {
		QuantityUnit.GRAMS -> R.string.unit_g_symbol
		QuantityUnit.KILOGRAMS -> R.string.unit_kg_symbol
		QuantityUnit.MILLIGRAMS -> R.string.unit_mg_symbol
		QuantityUnit.MILLILITERS -> R.string.unit_ml_symbol
		QuantityUnit.CENTILITERS -> R.string.unit_cl_symbol
		QuantityUnit.DECILITERS -> R.string.unit_dl_symbol
		QuantityUnit.LITERS -> R.string.unit_l_symbol
		QuantityUnit.PIECES -> R.string.unit_pz_symbol
		QuantityUnit.SLICES -> R.string.unit_slices_symbol
		QuantityUnit.CLOVES -> R.string.unit_cloves_symbol
		QuantityUnit.SPOONS -> R.string.unit_spoons_symbol
		QuantityUnit.TEASPOONS -> R.string.unit_teaspoons_symbol
		QuantityUnit.CUPS -> R.string.unit_cups_symbol
		QuantityUnit.GLASSES -> R.string.unit_glasses_symbol
		QuantityUnit.PINCHES -> R.string.unit_pinches_symbol
		QuantityUnit.JARS -> R.string.unit_jars_symbol
		QuantityUnit.CANS -> R.string.unit_cans_symbol
		QuantityUnit.PACKETS -> R.string.unit_packets_symbol
		QuantityUnit.LEAVES -> R.string.unit_leaves_symbol
		QuantityUnit.SPRIGS -> R.string.unit_sprigs_symbol
		QuantityUnit.DROPS -> R.string.unit_drops_symbol
		QuantityUnit.PORTIONS -> R.string.unit_portions_symbol
		QuantityUnit.QB -> R.string.unit_qb_symbol
		QuantityUnit.FREE -> R.string.unit_free_symbol
		QuantityUnit.CUSTOM -> 0
	}

val QuantityUnit.singularSymbolRes: Int
	get() = when (this) {
		QuantityUnit.GRAMS -> R.string.unit_g_symbol
		QuantityUnit.KILOGRAMS -> R.string.unit_kg_symbol
		QuantityUnit.MILLIGRAMS -> R.string.unit_mg_symbol
		QuantityUnit.MILLILITERS -> R.string.unit_ml_symbol
		QuantityUnit.CENTILITERS -> R.string.unit_cl_symbol
		QuantityUnit.DECILITERS -> R.string.unit_dl_symbol
		QuantityUnit.LITERS -> R.string.unit_l_symbol
		QuantityUnit.PIECES -> R.string.unit_pz_singular_symbol
		QuantityUnit.SLICES -> R.string.unit_slices_singular_symbol
		QuantityUnit.CLOVES -> R.string.unit_cloves_singular_symbol
		QuantityUnit.SPOONS -> R.string.unit_spoons_singular_symbol
		QuantityUnit.TEASPOONS -> R.string.unit_teaspoons_singular_symbol
		QuantityUnit.CUPS -> R.string.unit_cups_singular_symbol
		QuantityUnit.GLASSES -> R.string.unit_glasses_singular_symbol
		QuantityUnit.PINCHES -> R.string.unit_pinches_singular_symbol
		QuantityUnit.JARS -> R.string.unit_jars_singular_symbol
		QuantityUnit.CANS -> R.string.unit_cans_singular_symbol
		QuantityUnit.PACKETS -> R.string.unit_packets_singular_symbol
		QuantityUnit.LEAVES -> R.string.unit_leaves_singular_symbol
		QuantityUnit.SPRIGS -> R.string.unit_sprigs_singular_symbol
		QuantityUnit.DROPS -> R.string.unit_drops_singular_symbol
		QuantityUnit.PORTIONS -> R.string.unit_portions_singular_symbol
		QuantityUnit.QB -> R.string.unit_qb_symbol
		QuantityUnit.FREE -> R.string.unit_free_symbol
		QuantityUnit.CUSTOM -> 0
	}

val QuantityUnit.labelRes: Int
	get() = when (this) {
		QuantityUnit.GRAMS -> R.string.unit_g
		QuantityUnit.KILOGRAMS -> R.string.unit_kg
		QuantityUnit.MILLIGRAMS -> R.string.unit_mg
		QuantityUnit.MILLILITERS -> R.string.unit_ml
		QuantityUnit.CENTILITERS -> R.string.unit_cl
		QuantityUnit.DECILITERS -> R.string.unit_dl
		QuantityUnit.LITERS -> R.string.unit_l
		QuantityUnit.PIECES -> R.string.unit_pz
		QuantityUnit.SLICES -> R.string.unit_slices
		QuantityUnit.CLOVES -> R.string.unit_cloves
		QuantityUnit.SPOONS -> R.string.unit_spoons
		QuantityUnit.TEASPOONS -> R.string.unit_teaspoons
		QuantityUnit.CUPS -> R.string.unit_cups
		QuantityUnit.GLASSES -> R.string.unit_glasses
		QuantityUnit.PINCHES -> R.string.unit_pinches
		QuantityUnit.JARS -> R.string.unit_jars
		QuantityUnit.CANS -> R.string.unit_cans
		QuantityUnit.PACKETS -> R.string.unit_packets
		QuantityUnit.LEAVES -> R.string.unit_leaves
		QuantityUnit.SPRIGS -> R.string.unit_sprigs
		QuantityUnit.DROPS -> R.string.unit_drops
		QuantityUnit.PORTIONS -> R.string.unit_portions
		QuantityUnit.QB -> R.string.unit_qb
		QuantityUnit.FREE -> R.string.unit_free
		QuantityUnit.CUSTOM -> R.string.unit_custom
	}

fun QuantityUnit.getSymbol(context: Context? = null, isSingular: Boolean = false): String {
	val resId = if (isSingular) singularSymbolRes else symbolRes
	if ((context != null) && (resId != 0)) {
		return context.getString(resId)
	}
	return fallbackSymbol(isSingular)
}

fun QuantityUnit.format(amount: String, context: Context? = null): String {
	if (isNoAmountNeeded) {
		return getSymbol(context)
	}
	val cleanAmount = amount.trim()
	if (cleanAmount.isEmpty()) return ""
	val num = cleanAmount.replace(',', '.').toDoubleOrNull()
	val isSingular = (num == 1.0)
	val sym = getSymbol(context, isSingular)
	val effectiveSymbol = sym.ifEmpty { QuantityUnit.GRAMS.getSymbol(context) }
	return "$cleanAmount $effectiveSymbol".trim()
}

@Composable
fun QuantityUnit.localizedSymbol(isSingular: Boolean = false): String {
	val resId = if (isSingular) singularSymbolRes else symbolRes
	return if (resId != 0) stringResource(resId) else fallbackSymbol(isSingular)
}

fun it.matato.dietreminder.common.model.QuantityUnit.Companion.fromSymbol(symbol: String?, context: Context?): QuantityUnit {
	if (symbol.isNullOrBlank()) return QuantityUnit.CUSTOM
	val s = symbol.trim().lowercase()
	return QuantityUnit.entries.find { unit ->
		unit.name.equals(s, ignoreCase = true) ||
				unit.fallbackSymbol(isSingular = false).equals(s, ignoreCase = true) ||
				unit.fallbackSymbol(isSingular = true).equals(s, ignoreCase = true) ||
				(context != null && (
						(unit.symbolRes != 0 && context.getString(unit.symbolRes)
							.equals(s, ignoreCase = true)) ||
								(unit.singularSymbolRes != 0 && context.getString(unit.singularSymbolRes)
									.equals(s, ignoreCase = true))
						))
	} ?: QuantityUnit.CUSTOM
}
