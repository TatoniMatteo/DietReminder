package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.domain.IngredientOccurrence
import it.matato.dietreminder.domain.IngredientSummary
import it.matato.dietreminder.domain.QuantityAggregator
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun ImportDietFoodsScreen(
	vm: DietViewModel,
	listId: Long,
	dietId: Long,
	onBack: () -> Unit,
) {
	var ingredients by remember { mutableStateOf<List<IngredientSummary>>(emptyList()) }
	var isLoading by remember { mutableStateOf(true) }

	LaunchedEffect(dietId) {
		ingredients = vm.getDietIngredients(dietId)
		isLoading = false
	}

	if (isLoading) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			CircularProgressIndicator()
		}
		return
	}

	ImportDietFoodsContent(
		ingredients = ingredients,
		onBack = onBack,
		onConfirmImport = { configs ->
			vm.importDietIngredientsToShoppingList(listId, dietId, configs) {
				onBack()
			}
		},
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDietFoodsContent(
	ingredients: List<IngredientSummary>,
	onBack: () -> Unit,
	onConfirmImport: (List<DietImportItemConfig>) -> Unit,
) {
	val isOffline = LocalOfflineMode.current
	val selectedState = remember(ingredients) {
		mutableStateMapOf<String, Boolean>().apply {
			ingredients.forEach { put(it.name, true) }
		}
	}

	val freshState = remember(ingredients) {
		mutableStateMapOf<String, Boolean>().apply {
			ingredients.forEach { ingredient ->
				val nameLower = ingredient.name.lowercase()
				val isFreshDefault = nameLower.contains("salmone") ||
						nameLower.contains("pesce") ||
						nameLower.contains("carne") ||
						nameLower.contains("petto") ||
						nameLower.contains("mozzarella") ||
						nameLower.contains("fresco") ||
						nameLower.contains("freschi") ||
						nameLower.contains("ricotta") ||
						nameLower.contains("latte")
				put(ingredient.name, isFreshDefault)
			}
		}
	}

	val selectedDaysState = remember(ingredients) {
		mutableStateMapOf<String, MutableSet<DayOfWeek>>().apply {
			ingredients.forEach { ingredient ->
				val allDays = ingredient.occurrences.map { it.dayOfWeek }.toMutableSet()
				put(ingredient.name, allDays)
			}
		}
	}

	val selectedCount = selectedState.values.count { it }

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = stringResource(R.string.import_diet_foods_title),
						fontWeight = FontWeight.Bold,
					)
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
			)
		},
		bottomBar = {
			if (ingredients.isNotEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(MaterialTheme.colorScheme.surface)
						.padding(16.dp),
				) {
					Button(
						onClick = {
							val configs = ingredients.filter { selectedState[it.name] == true }.map {
								DietImportItemConfig(
									ingredientName = it.name,
									isFresh = freshState[it.name] ?: false,
									selectedDays = selectedDaysState[it.name] ?: emptySet(),
								)
							}
							onConfirmImport(configs)
						},
						enabled = selectedCount > 0 && !isOffline,
						modifier = Modifier.fillMaxWidth(),
					) {
						Icon(Icons.Rounded.Check, contentDescription = null)
						Spacer(modifier = Modifier.width(8.dp))
						Text(
							text = pluralStringResource(
								R.plurals.import_selected_count,
								selectedCount,
								selectedCount,
							),
							fontWeight = FontWeight.Bold,
						)
					}
				}
			}
		},
	) { innerPadding ->
		if (ingredients.isEmpty()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentAlignment = Alignment.Center,
			) {
				EmptyDietFoodsState()
			}
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentPadding = PaddingValues(
					start = 16.dp,
					end = 16.dp,
					top = 12.dp,
					bottom = 80.dp,
				),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				item {
					ImportInfoCard()
				}

				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.End,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Row {
							TextButton(
								onClick = {
									ingredients.forEach { selectedState[it.name] = true }
								},
							) {
								Text(stringResource(R.string.select_all))
							}
							Spacer(modifier = Modifier.width(4.dp))
							TextButton(
								onClick = {
									ingredients.forEach { selectedState[it.name] = false }
								},
							) {
								Text(stringResource(R.string.deselect_all))
							}
						}
					}
				}

				items(
					items = ingredients,
					key = { it.name },
				) { ingredient ->
					val isSelected = selectedState[ingredient.name] ?: true
					val isFresh = freshState[ingredient.name] ?: false
					val daysSet = selectedDaysState[ingredient.name] ?: mutableSetOf()

					IngredientImportCard(
						ingredient = ingredient,
						isSelected = isSelected,
						isFresh = isFresh,
						selectedDays = daysSet,
						onToggleSelected = {
							selectedState[ingredient.name] = !isSelected
						},
						onToggleFresh = { fresh ->
							freshState[ingredient.name] = fresh
						},
						onToggleDay = { day ->
							if (daysSet.contains(day)) {
								daysSet.remove(day)
							} else {
								daysSet.add(day)
							}
						},
					)
				}
			}
		}
	}
}

@Composable
private fun ImportInfoCard() {
	OutlinedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.medium,
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalAlignment = Alignment.Top,
		) {
			Box(
				modifier = Modifier
					.clip(MaterialTheme.shapes.small)
					.background(MaterialTheme.colorScheme.primaryContainer)
					.padding(8.dp),
			) {
				Icon(
					imageVector = Icons.Rounded.Info,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onPrimaryContainer,
				)
			}

			Spacer(modifier = Modifier.width(12.dp))

			Column(
				modifier = Modifier.weight(1f),
			) {
				Text(
					text = stringResource(R.string.import_info_title),
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(R.string.import_info_desc),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

@Composable
private fun IngredientImportCard(
	ingredient: IngredientSummary,
	isSelected: Boolean,
	isFresh: Boolean,
	selectedDays: Set<DayOfWeek>,
	onToggleSelected: () -> Unit,
	onToggleFresh: (Boolean) -> Unit,
	onToggleDay: (DayOfWeek) -> Unit,
) {
	val totalQuantity = remember(ingredient) {
		QuantityAggregator.sumOccurrences(ingredient.occurrences)
	}

	OutlinedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.medium,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable(onClick = onToggleSelected),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Checkbox(
					checked = isSelected,
					onCheckedChange = { onToggleSelected() },
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = ingredient.name,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.weight(1f),
				)
			}

			if (isSelected) {
				Spacer(modifier = Modifier.height(8.dp))
				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
				Spacer(modifier = Modifier.height(8.dp))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						text = stringResource(R.string.item_type_label),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)

					FilterChip(
						selected = !isFresh,
						onClick = { onToggleFresh(false) },
						label = { Text(stringResource(R.string.type_standard_weekly)) },
					)

					FilterChip(
						selected = isFresh,
						onClick = { onToggleFresh(true) },
						label = { Text(stringResource(R.string.type_fresh_daily)) },
					)
				}

				Spacer(modifier = Modifier.height(6.dp))

				if (!isFresh) {
					Text(
						text = stringResource(R.string.weekly_total_preview, totalQuantity.ifBlank { "—" }),
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.SemiBold,
						color = MaterialTheme.colorScheme.primary,
					)
				} else {
					Column(
						verticalArrangement = Arrangement.spacedBy(2.dp),
					) {
						Text(
							text = stringResource(R.string.daily_breakdown_title),
							style = MaterialTheme.typography.labelSmall,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.primary,
						)
						ingredient.occurrences.groupBy { it.dayOfWeek }.forEach { (day, occurrences) ->
							val dayName = day.getDisplayName(
								TextStyle.FULL,
								LocalLocale.current.platformLocale,
							).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

							val dayQty = QuantityAggregator.sumOccurrences(occurrences)
							val isDayChecked = selectedDays.contains(day)

							Row(
								modifier = Modifier
									.fillMaxWidth()
									.clickable { onToggleDay(day) }
									.padding(vertical = 2.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Checkbox(
									checked = isDayChecked,
									onCheckedChange = { onToggleDay(day) },
								)
								Spacer(modifier = Modifier.width(4.dp))
								Text(
									text = "$dayName: $dayQty",
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = if (isDayChecked) FontWeight.Bold else FontWeight.Normal,
									color = if (isDayChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun EmptyDietFoodsState() {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 32.dp, vertical = 48.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		IconContainer(
			icon = Icons.Rounded.Restaurant,
			size = 64.dp,
			iconSize = 32.dp,
		)

		Text(
			text = stringResource(R.string.no_ingredients_in_diet),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun ImportDietFoodsContentPreview() {
	DietTheme {
		ImportDietFoodsContent(
			ingredients = listOf(
				IngredientSummary(
					name = "Biscotti",
					occurrences = listOf(
						IngredientOccurrence(
							dayOfWeek = DayOfWeek.MONDAY,
							mealType = MealType.BREAKFAST,
							amount = "45",
							unit = QuantityUnit.GRAMS,
						),
					),
				),
				IngredientSummary(
					name = "Salmone",
					occurrences = listOf(
						IngredientOccurrence(
							dayOfWeek = DayOfWeek.THURSDAY,
							mealType = MealType.LUNCH,
							amount = "150",
							unit = QuantityUnit.GRAMS,
						),
					),
				),
			),
			onBack = {},
			onConfirmImport = {},
		)
	}
}
