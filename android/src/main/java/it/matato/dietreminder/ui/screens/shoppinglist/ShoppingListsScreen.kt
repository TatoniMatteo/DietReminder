package it.matato.dietreminder.ui.screens.shoppinglist

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.OfflineAwareFloatingActionButton
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ShoppingListsScreen(
	vm: DietViewModel,
	onBack: (() -> Unit)? = null,
	onSelectList: (Long) -> Unit,
	onNavigateToImportDietFoods: (listId: Long, dietId: Long) -> Unit,
	onNavigateToIngredients: () -> Unit,
) {
	val shoppingLists by vm.shoppingLists.collectAsState()
	val activeDiet by vm.active.collectAsState()

	var showCreateDialog by remember { mutableStateOf(false) }

	ShoppingListsContent(
		shoppingLists = shoppingLists,
		onBack = onBack,
		onSelectList = onSelectList,
		onIngredientsClick = onNavigateToIngredients,
		onCreateClick = { showCreateDialog = true },
		onDeleteList = { vm.deleteShoppingList(it) },
		onRenameList = { id, newName -> vm.updateShoppingListName(id, newName) },
	)

	if (showCreateDialog) {
		val defaultName = stringResource(
			R.string.default_shopping_list_name,
			LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
		)

		NewShoppingListDialog(
			defaultName = defaultName,
			hasActiveDiet = activeDiet != null,
			onDismiss = { showCreateDialog = false },
			onConfirm = { name, importFromDiet ->
				showCreateDialog = false
				val activeDietId = activeDiet?.id
				vm.createShoppingList(
					name = name,
					dietId = activeDietId,
					onCreated = { listId ->
						if (importFromDiet && activeDietId != null) {
							onNavigateToImportDietFoods(listId, activeDietId)
						} else {
							onSelectList(listId)
						}
					},
				)
			},
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsContent(
	shoppingLists: List<ShoppingListWithItems>,
	onBack: (() -> Unit)? = null,
	onSelectList: (Long) -> Unit,
	onIngredientsClick: () -> Unit,
	onCreateClick: () -> Unit,
	onDeleteList: (Long) -> Unit,
	onRenameList: (Long, String) -> Unit,
) {
	val isOffline = LocalOfflineMode.current
	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = stringResource(R.string.shopping_lists),
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
				actions = {
					IconButton(onClick = onIngredientsClick) {
						Icon(
							imageVector = Icons.Rounded.Kitchen,
							contentDescription = stringResource(R.string.ingredients_title),
						)
					}
				},
			)
		},
		floatingActionButton = {
			OfflineAwareFloatingActionButton(
				onClick = onCreateClick,
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary,
			) {
				Icon(
					imageVector = Icons.Rounded.Add,
					contentDescription = stringResource(R.string.new_shopping_list),
				)
			}
		},
	) { innerPadding ->
		if (shoppingLists.isEmpty()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentAlignment = Alignment.Center,
			) {
				EmptyShoppingListsState()
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
					bottom = 88.dp,
				),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				items(
					items = shoppingLists,
					key = { it.list.id },
				) { listWithItems ->
					ShoppingListCard(
						listWithItems = listWithItems,
						onClick = { onSelectList(listWithItems.list.id) },
						onDelete = { onDeleteList(listWithItems.list.id) },
						onRename = { newName -> onRenameList(listWithItems.list.id, newName) },
					)
				}
			}
		}
	}
}

@Composable
private fun ShoppingListCard(
	listWithItems: ShoppingListWithItems,
	onClick: () -> Unit,
	onDelete: () -> Unit,
	onRename: (String) -> Unit,
) {
	val totalItems = listWithItems.items.size
	val boughtItems = listWithItems.items.count { it.isAllDaysBought }
	val progress = if (totalItems > 0) boughtItems.toFloat() / totalItems.toFloat() else 0f

	val createdDateFormatted = remember(listWithItems.list.createdAt) {
		val date = Instant.ofEpochMilli(listWithItems.list.createdAt)
			.atZone(ZoneId.systemDefault())
			.toLocalDate()
		date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
	}

	var showMenu by remember { mutableStateOf(false) }
	var showRenameDialog by remember { mutableStateOf(false) }
	var showDeleteConfirmDialog by remember { mutableStateOf(false) }

	OutlinedCard(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		shape = MaterialTheme.shapes.large,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = listWithItems.list.name,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
					Spacer(modifier = Modifier.height(2.dp))
					Text(
						text = createdDateFormatted,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}

				Box {
					IconButton(onClick = { showMenu = true }, enabled = !LocalOfflineMode.current) {
						Icon(
							imageVector = Icons.Rounded.MoreVert,
							contentDescription = null,
						)
					}

					DropdownMenu(
						expanded = showMenu,
						onDismissRequest = { showMenu = false },
					) {
						DropdownMenuItem(
							text = { Text(stringResource(R.string.rename_list)) },
							onClick = {
								showMenu = false
								showRenameDialog = true
							},
							enabled = !LocalOfflineMode.current,
							leadingIcon = {
								Icon(Icons.Rounded.Edit, contentDescription = null)
							},
						)
						DropdownMenuItem(
							text = { Text(stringResource(R.string.delete)) },
							onClick = {
								showMenu = false
								showDeleteConfirmDialog = true
							},
							enabled = !LocalOfflineMode.current,
							leadingIcon = {
								Icon(
									Icons.Rounded.Delete,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.error,
								)
							},
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = pluralStringResource(
						R.plurals.items_purchased_count,
						boughtItems,
						boughtItems,
						totalItems,
					),
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary,
				)
			}

			Spacer(modifier = Modifier.height(6.dp))

			LinearProgressIndicator(
				progress = { progress },
				modifier = Modifier.fillMaxWidth(),
			)
		}
	}

	if (showRenameDialog) {
		RenameShoppingListDialog(
			currentName = listWithItems.list.name,
			onDismiss = { showRenameDialog = false },
			onConfirm = { newName ->
				showRenameDialog = false
				onRename(newName)
			},
		)
	}

	if (showDeleteConfirmDialog) {
		AlertDialog(
			onDismissRequest = { showDeleteConfirmDialog = false },
			title = { Text(stringResource(R.string.delete)) },
			text = { Text(listWithItems.list.name) },
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteConfirmDialog = false
						onDelete()
					},
				) {
					Text(
						stringResource(R.string.delete),
						color = MaterialTheme.colorScheme.error,
					)
				}
			},
			dismissButton = {
				TextButton(onClick = { showDeleteConfirmDialog = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
		)
	}
}

@Composable
private fun NewShoppingListDialog(
	defaultName: String,
	hasActiveDiet: Boolean,
	onDismiss: () -> Unit,
	onConfirm: (name: String, importFromDiet: Boolean) -> Unit,
) {
	var name by remember { mutableStateOf(defaultName) }
	var importFromDiet by remember { mutableStateOf(hasActiveDiet) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.new_shopping_list)) },
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text(stringResource(R.string.list_name)) },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
				)

				if (hasActiveDiet) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier
							.fillMaxWidth()
							.clickable { importFromDiet = !importFromDiet },
					) {
						Checkbox(
							checked = importFromDiet,
							onCheckedChange = { importFromDiet = it },
						)
						Spacer(modifier = Modifier.width(8.dp))
						Text(
							text = stringResource(R.string.import_diet_items_option),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					if (name.isNotBlank()) {
						onConfirm(name.trim(), importFromDiet)
					}
				},
				enabled = name.isNotBlank() && !LocalOfflineMode.current,
			) {
				Text(stringResource(R.string.create))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.cancel))
			}
		},
	)
}

@Composable
private fun RenameShoppingListDialog(
	currentName: String,
	onDismiss: () -> Unit,
	onConfirm: (newName: String) -> Unit,
) {
	var name by remember { mutableStateOf(currentName) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.rename_list)) },
		text = {
			OutlinedTextField(
				value = name,
				onValueChange = { name = it },
				label = { Text(stringResource(R.string.list_name)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
		},
		confirmButton = {
			TextButton(
				onClick = {
					if (name.isNotBlank()) {
						onConfirm(name.trim())
					}
				},
				enabled = name.isNotBlank() && !LocalOfflineMode.current,
			) {
				Text(stringResource(R.string.save))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.cancel))
			}
		},
	)
}

@Composable
private fun EmptyShoppingListsState() {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 32.dp, vertical = 48.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		IconContainer(
			icon = Icons.Rounded.ShoppingCart,
			size = 64.dp,
			iconSize = 32.dp,
		)

		Text(
			text = stringResource(R.string.no_shopping_lists),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
		)

		Text(
			text = stringResource(R.string.no_shopping_lists_desc),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListsContentPreview() {
	DietTheme {
		ShoppingListsContent(
			shoppingLists = listOf(
				ShoppingListWithItems(
					list = it.matato.dietreminder.data.database.entity.ShoppingList(
						id = 1,
						name = "Lista della spesa 26/09/2026"),
					items = listOf(
						it.matato.dietreminder.data.database.relation.ShoppingListItemWithDays(
							item = it.matato.dietreminder.data.database.entity.ShoppingListItem(
								id = 1,
								shoppingListId = 1,
								name = "Biscotti",
								amount = "315",
								unit = it.matato.dietreminder.data.model.QuantityUnit.GRAMS,
								isBought = true),
						),
						it.matato.dietreminder.data.database.relation.ShoppingListItemWithDays(
							item = it.matato.dietreminder.data.database.entity.ShoppingListItem(
								id = 2,
								shoppingListId = 1,
								name = "Salmone",
								amount = "150",
								unit = it.matato.dietreminder.data.model.QuantityUnit.GRAMS,
								isBought = false),
						),
					),
				),
			),
			onSelectList = {},
			onIngredientsClick = {},
			onCreateClick = {},
			onDeleteList = {},
			onRenameList = { _, _ -> },
		)
	}
}
