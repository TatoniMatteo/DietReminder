package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.ui.components.QuantityInputRow
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodItemRow(
	item: FoodItem,
	suggestions: List<String> = emptyList(),
	onUpdate: (FoodItem) -> Unit,
	onDelete: () -> Unit,
) {
	val isOffline = LocalOfflineMode.current
	var expanded by remember { mutableStateOf(false) }

	val filteredSuggestions = remember(item.name, suggestions) {
		if (item.name.isBlank()) emptyList()
		else suggestions.filter {
			it.contains(item.name.trim(), ignoreCase = true) && !it.equals(item.name.trim(), ignoreCase = true)
		}.take(5)
	}

	val parts = remember(item.quantities, item.amount, item.unit) {
		if (item.amount.isNotBlank() || item.unit != QuantityUnit.CUSTOM) {
			Pair(item.amount, item.unit)
		} else {
			val trimmed = item.quantities.trim()
			val spaceIndex = trimmed.lastIndexOf(' ')
			if (spaceIndex > 0) {
				val num = trimmed.substring(0, spaceIndex).trim()
				val unitStr = trimmed.substring(spaceIndex + 1).trim()
				Pair(num, QuantityUnit.fromSymbol(unitStr))
			} else {
				Pair(trimmed, QuantityUnit.CUSTOM)
			}
		}
	}

	var currentAmount by remember(item.id, parts.first) { mutableStateOf(parts.first) }
	var currentUnit by remember(item.id, parts.second) { mutableStateOf(parts.second) }

	Column(
		modifier = Modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		ExposedDropdownMenuBox(
			expanded = expanded && filteredSuggestions.isNotEmpty(),
			onExpandedChange = { expanded = it },
			modifier = Modifier.fillMaxWidth(),
		) {
			OutlinedTextField(
				value = item.name,
				onValueChange = {
					onUpdate(item.copy(name = it))
					expanded = true
				},
				modifier = Modifier
					.fillMaxWidth()
					.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
				label = {
					Text(stringResource(R.string.food_item))
				},
				shape = MaterialTheme.shapes.medium,
				singleLine = true,
				enabled = !isOffline,
			)

			if (filteredSuggestions.isNotEmpty()) {
				ExposedDropdownMenu(
					expanded = expanded,
					onDismissRequest = { expanded = false },
				) {
					filteredSuggestions.forEach { suggestion ->
						DropdownMenuItem(
							text = { Text(suggestion) },
							onClick = {
								onUpdate(item.copy(name = suggestion))
								expanded = false
							},
							enabled = !isOffline,
						)
					}
				}
			}
		}

		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
		) {
			QuantityInputRow(
				amount = currentAmount,
				selectedUnit = currentUnit,
				onAmountChange = { newAmount ->
					currentAmount = newAmount
					onUpdate(
						item.copy(
							amount = if (currentUnit.isNoAmountNeeded) "" else newAmount,
							unit = currentUnit,
						),
					)
				},
				onUnitChange = { newUnit ->
					currentUnit = newUnit
					val newAmount = if (newUnit.isNoAmountNeeded) "" else currentAmount
					onUpdate(
						item.copy(
							amount = newAmount,
							unit = newUnit,
						),
					)
				},
				modifier = Modifier.weight(1f),
				enabled = !isOffline,
			)

			IconButton(
				onClick = onDelete,
				enabled = !isOffline,
			) {
				Icon(
					imageVector = Icons.Rounded.RemoveCircleOutline,
					contentDescription = stringResource(R.string.delete),
					tint = MaterialTheme.colorScheme.error,
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun FoodItemRowPreview() {
	DietTheme {
		FoodItemRow(
			item = FoodItem(
				id = 1,
				courseId = 1,
				name = "Apple",
				amount = "1",
				unit = QuantityUnit.PIECES,
			),
			suggestions = listOf("Apple Pie", "Apple Juice"),
			onUpdate = {},
			onDelete = {},
		)
	}
}
