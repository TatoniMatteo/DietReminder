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
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodItemRow(
    item: FoodItem,
    suggestions: List<String> = emptyList(),
    onUpdate: (FoodItem) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredSuggestions = remember(item.name, suggestions) {
        if (item.name.isBlank()) emptyList()
        else suggestions.filter {
            it.contains(item.name.trim(), ignoreCase = true) && !it.equals(item.name.trim(), ignoreCase = true)
        }.take(5)
    }

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
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = item.quantities,
                onValueChange = { value ->
                    onUpdate(
                        item.copy(
                            quantities = value,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text(stringResource(R.string.quantity))
                },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )

            IconButton(
                onClick = onDelete,
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
                quantities = "1 unit",
            ),
            suggestions = listOf("Apple Pie", "Apple Juice"),
            onUpdate = {},
            onDelete = {},
        )
    }
}
