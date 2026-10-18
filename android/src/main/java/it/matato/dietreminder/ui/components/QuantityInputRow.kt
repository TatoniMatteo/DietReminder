package it.matato.dietreminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.model.localizedSymbol
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityInputRow(
	amount: String,
	selectedUnit: QuantityUnit,
	onAmountChange: (String) -> Unit,
	onUnitChange: (QuantityUnit) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = !LocalOfflineMode.current,
) {
	var expanded by remember { mutableStateOf(false) }

	Row(
		modifier = modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		OutlinedTextField(
			value = if (selectedUnit.isNoAmountNeeded) selectedUnit.localizedSymbol() else amount,
			onValueChange = { newAmount ->
				if (!selectedUnit.isNoAmountNeeded) {
					onAmountChange(newAmount)
					if (newAmount.isNotBlank() && selectedUnit == QuantityUnit.CUSTOM) {
						onUnitChange(QuantityUnit.GRAMS)
					}
				}
			},
			enabled = enabled && !selectedUnit.isNoAmountNeeded,
			modifier = Modifier.weight(1f),
			label = { Text(stringResource(R.string.quantity)) },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			shape = MaterialTheme.shapes.medium,
			singleLine = true,
		)

		ExposedDropdownMenuBox(
			expanded = expanded,
			onExpandedChange = { if (enabled) expanded = it },
			modifier = Modifier.weight(1f),
		) {
			OutlinedTextField(
				value = if (selectedUnit == QuantityUnit.CUSTOM) stringResource(R.string.unit_custom) else if (selectedUnit.isNoAmountNeeded) selectedUnit.localizedSymbol() else "${selectedUnit.localizedSymbol()} (${
					stringResource(
						selectedUnit.labelRes)
				})",
				onValueChange = {},
				readOnly = true,
				enabled = enabled,
				trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
				modifier = Modifier
					.fillMaxWidth()
					.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = enabled),
				label = { Text(stringResource(R.string.unit_label)) },
				shape = MaterialTheme.shapes.medium,
				singleLine = true,
			)

			ExposedDropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false },
			) {
				QuantityUnit.entries.forEach { unit ->
					DropdownMenuItem(
						text = {
							Text(
								if (unit == QuantityUnit.CUSTOM) stringResource(unit.labelRes)
								else if (unit.isNoAmountNeeded) unit.localizedSymbol()
								else "${unit.localizedSymbol()} (${stringResource(unit.labelRes)})",
							)
						},
						onClick = {
							onUnitChange(unit)
							expanded = false
						},
						enabled = enabled,
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun QuantityInputRowPreview() {
	DietTheme {
		QuantityInputRow(
			amount = "100",
			selectedUnit = QuantityUnit.GRAMS,
			onAmountChange = {},
			onUnitChange = {},
		)
	}
}
