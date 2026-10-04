package it.matato.dietreminder.ui.screens.ingredients

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.domain.IngredientOccurrence
import it.matato.dietreminder.domain.IngredientSummary
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun IngredientsScreen(
	vm: DietViewModel,
	dietId: Long? = null,
	onBack: (() -> Unit)? = null,
) {
	val activeDiet by vm.active.collectAsState()
	val targetDietId = dietId ?: activeDiet?.id ?: 0L

	var ingredients by remember { mutableStateOf<List<IngredientSummary>>(emptyList()) }
	var isLoading by remember { mutableStateOf(true) }

	LaunchedEffect(targetDietId) {
		ingredients = if (targetDietId > 0L) {
			vm.getDietIngredients(targetDietId)
		} else {
			emptyList()
		}
		isLoading = false
	}

	IngredientsContent(
		isLoading = isLoading,
		ingredients = ingredients,
		onBack = onBack,
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsContent(
	isLoading: Boolean,
	ingredients: List<IngredientSummary>,
	onBack: (() -> Unit)? = null,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = stringResource(R.string.ingredients_title),
						fontWeight = FontWeight.Bold,
					)
				},
				navigationIcon = {
					if (onBack != null) {
						IconButton(onClick = onBack) {
							Icon(
								imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
								contentDescription = stringResource(R.string.back),
							)
						}
					}
				},
			)
		},
	) { innerPadding ->
		if (isLoading) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentAlignment = Alignment.Center,
			) {
				CircularProgressIndicator()
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
					bottom = 32.dp,
				),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				if (ingredients.isEmpty()) {
					item {
						EmptyIngredientsState()
					}
				} else {
					items(
						items = ingredients,
						key = { it.name },
					) { ingredient ->
						ExpandableIngredientCard(ingredient = ingredient)
					}
				}
			}
		}
	}
}

@Composable
private fun ExpandableIngredientCard(ingredient: IngredientSummary) {
	var expanded by remember { mutableStateOf(false) }

	OutlinedCard(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { expanded = !expanded },
		shape = MaterialTheme.shapes.large,
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 14.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = ingredient.name,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.weight(1f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)

				Spacer(modifier = Modifier.width(8.dp))

				Box(
					modifier = Modifier
						.clip(MaterialTheme.shapes.small)
						.background(MaterialTheme.colorScheme.primaryContainer)
						.padding(horizontal = 8.dp, vertical = 4.dp),
				) {
					Text(
						text = stringResource(R.string.meals_count, ingredient.totalOccurrences),
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
					)
				}

				Spacer(modifier = Modifier.width(6.dp))

				Icon(
					imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			AnimatedVisibility(visible = expanded) {
				Column(
					modifier = Modifier.fillMaxWidth(),
				) {
					HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

					ingredient.occurrences.forEachIndexed { index, occurrence ->
						IngredientOccurrenceRow(occurrence = occurrence)

						if (index < ingredient.occurrences.lastIndex) {
							HorizontalDivider(
								modifier = Modifier.padding(horizontal = 16.dp),
								color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
							)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun IngredientOccurrenceRow(occurrence: IngredientOccurrence) {
	val dayName = occurrence.dayOfWeek.getDisplayName(
		TextStyle.FULL,
		LocalLocale.current.platformLocale,
	).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

	val mealName = occurrence.mealCustomLabel.takeIf { !it.isNullOrBlank() }
		?: stringResource(occurrence.mealType.resId)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.clip(MaterialTheme.shapes.small)
				.background(MaterialTheme.colorScheme.surfaceVariant)
				.padding(horizontal = 8.dp, vertical = 4.dp),
		) {
			Text(
				text = dayName,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
			)
		}

		Spacer(modifier = Modifier.width(12.dp))

		Column(
			modifier = Modifier.weight(1f),
		) {
			Text(
				text = mealName,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurface,
			)

			if (occurrence.courseName.isNotBlank()) {
				Text(
					text = occurrence.courseName,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}

		Spacer(modifier = Modifier.width(8.dp))

		Text(
			text = occurrence.quantity.ifBlank { "—" },
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Bold,
			color = if (occurrence.quantity.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
		)
	}
}

@Composable
private fun EmptyIngredientsState() {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 48.dp),
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
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun IngredientsContentPreview() {
	DietTheme {
		IngredientsContent(
			isLoading = false,
			ingredients = listOf(
				IngredientSummary(
					name = "Spaghetti",
					occurrences = listOf(
						IngredientOccurrence(
							dayOfWeek = DayOfWeek.MONDAY,
							mealType = MealType.LUNCH,
							courseName = "Primo",
							amount = "100",
							unit = QuantityUnit.GRAMS,
						),
						IngredientOccurrence(
							dayOfWeek = DayOfWeek.THURSDAY,
							mealType = MealType.DINNER,
							courseName = "Primo",
							amount = "120",
							unit = QuantityUnit.GRAMS,
						),
					),
				),
				IngredientSummary(
					name = "Petto di pollo",
					occurrences = listOf(
						IngredientOccurrence(
							dayOfWeek = DayOfWeek.TUESDAY,
							mealType = MealType.DINNER,
							courseName = "Secondo",
							amount = "150",
							unit = QuantityUnit.GRAMS,
						),
					),
				),
			),
			onBack = {},
		)
	}
}
