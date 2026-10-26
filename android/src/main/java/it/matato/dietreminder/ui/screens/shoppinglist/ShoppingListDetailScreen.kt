package it.matato.dietreminder.ui.screens.shoppinglist


import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppTopBar
import it.matato.dietreminder.ui.components.AppScaffold
import it.matato.dietreminder.ui.components.AppCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CardDefaults
import it.matato.dietreminder.ui.theme.AppMotion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay
import it.matato.dietreminder.data.database.relation.ShoppingListItemWithDays
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.components.QuantityInputRow
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.OfflineAwareFloatingActionButton
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.format.TextStyle

@Composable
fun ShoppingListDetailScreen(
	vm: DietViewModel,
	listId: Long,
	onBack: () -> Unit,
	onNavigateToImportDietFoods: (listId: Long, dietId: Long) -> Unit,
) {
	val listWithItems by vm.observeShoppingList(listId).collectAsState(initial = null)
	val activeDiet by vm.active.collectAsState()

	var showAddCustomDialog by remember { mutableStateOf(false) }

	if (listWithItems == null) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			CircularProgressIndicator()
		}
		return
	}

	ShoppingListDetailContent(
		listWithItems = listWithItems!!,
		hasActiveDiet = activeDiet != null,
		onBack = onBack,
		onToggleItem = { vm.toggleShoppingListItem(it) },
		onToggleDay = { dayId, isBought -> vm.toggleShoppingListItemDay(dayId, isBought) },
		onDeleteItem = { vm.deleteShoppingListItem(it) },
		onUpdateItem = { vm.updateShoppingListItem(it) },
		onAddCustomClick = { showAddCustomDialog = true },
		onImportDietClick = {
			activeDiet?.let { diet ->
				onNavigateToImportDietFoods(listId, diet.id)
			}
		},
		onDeleteList = {
			vm.deleteShoppingList(listId)
			onBack()
		},
        onMovePurchase = vm::moveShoppingPurchase,
        onAllocation = vm::updateShoppingAllocation,
        onReminders = { enabled -> vm.updateShoppingReminders(listId, enabled) },
		onRenameList = { newName ->
			vm.updateShoppingListName(listId, newName)
		},
	)

	if (showAddCustomDialog) {
		EditShoppingListItemDialog(
			title = stringResource(R.string.add_custom_item),
			initialName = "",
			initialAmount = "",
			initialUnit = QuantityUnit.GRAMS,
			onDismiss = { showAddCustomDialog = false },
			onConfirm = { name, amount, unit ->
				showAddCustomDialog = false
				vm.addShoppingListItem(
					listId = listId,
					name = name,
					amount = amount,
					unit = unit,
					isCustom = true,
				)
			},
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailContent(
	listWithItems: ShoppingListWithItems,
	hasActiveDiet: Boolean,
	onBack: () -> Unit,
	onToggleItem: (ShoppingListItem) -> Unit,
	onToggleDay: (dayId: Long, isBought: Boolean) -> Unit,
	onDeleteItem: (Long) -> Unit,
	onUpdateItem: (ShoppingListItem) -> Unit,
	onAddCustomClick: () -> Unit,
	onImportDietClick: () -> Unit,
	onDeleteList: () -> Unit,
	onRenameList: (String) -> Unit,
    onAllocation: (ShoppingListItemDay) -> Unit = {},
    onMovePurchase: (Long, Long?, java.time.DayOfWeek?) -> Unit = { _, _, _ -> },
    onReminders: (Boolean) -> Unit = {},
) {
	val isOffline = LocalOfflineMode.current
	var editingItem by remember { mutableStateOf<ShoppingListItem?>(null) }
	var showMenu by remember { mutableStateOf(false) }
	var showRenameDialog by remember { mutableStateOf(false) }
	var showDeleteConfirmDialog by remember { mutableStateOf(false) }
	var showCompletedDeleteConfirmDialog by remember { mutableStateOf(false) }
	var fabExpanded by remember { mutableStateOf(false) }
    val purchases = remember(listWithItems) { shoppingEntries(listWithItems) }
    val totalCount = purchases.size
    val boughtCount = purchases.count { it.bought }
    val progress = if (totalCount > 0) boughtCount.toFloat() / totalCount else 0f

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
		label = "progress_anim",
	)

	AppScaffold(
		topBar = {
			Column {
				AppTopBar(
					title = {
						Text(
							text = listWithItems.list.name,
							fontWeight = FontWeight.Bold,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
						)
					},
					navigationIcon = {
						FilledTonalIconButton(onClick = onBack) {
							Icon(
								imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
								contentDescription = stringResource(R.string.back),
							)
						}
					},
					actions = {
						Box {
							IconButton(onClick = { showMenu = true }, enabled = !isOffline) {
								Icon(
									imageVector = Icons.Rounded.MoreVert,
									contentDescription = stringResource(R.string.more_actions),
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
									enabled = !isOffline,
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
									enabled = !isOffline,
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
					},
				)

				if (totalCount > 0) {
					LinearProgressIndicator(
						progress = { animatedProgress },
						modifier = Modifier
							.fillMaxWidth()
                            .padding(horizontal = AppSpace.screen)
							.height(6.dp),
					)
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = AppSpace.screen, vertical = 6.dp),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = pluralStringResource(
								R.plurals.items_purchased_count,
								boughtCount,
								boughtCount,
								totalCount,
							),
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.primary,
							fontWeight = FontWeight.Bold,
						)
						Text(
							text = "${(animatedProgress * 100).toInt()}%",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							fontWeight = FontWeight.Bold,
						)
					}
				}
			}
		},
		floatingActionButton = {
			Box(contentAlignment = Alignment.BottomEnd) {
				OfflineAwareFloatingActionButton(
					onClick = { fabExpanded = !fabExpanded },
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary,
				) {
					Icon(
						imageVector = Icons.Rounded.Add,
						contentDescription = stringResource(R.string.add_item),
					)
				}

				DropdownMenu(
					expanded = fabExpanded,
					onDismissRequest = { fabExpanded = false },
				) {
					DropdownMenuItem(
						text = { Text(stringResource(R.string.add_custom_item)) },
						onClick = {
							fabExpanded = false
							onAddCustomClick()
						},
						enabled = !isOffline,
						leadingIcon = {
							Icon(Icons.Rounded.Edit, contentDescription = null)
						},
					)
					if (hasActiveDiet) {
						DropdownMenuItem(
							text = { Text(stringResource(R.string.import_from_diet)) },
							onClick = {
								fabExpanded = false
								onImportDietClick()
							},
							enabled = !isOffline,
							leadingIcon = {
								Icon(Icons.Rounded.Restaurant, contentDescription = null)
							},
						)
					}
				}
			}
		},
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
		) {
            ShoppingReminderControls(listWithItems.list, onReminders)
            ShoppingDayPlanner(listWithItems, onToggleItem, onToggleDay, { editingItem = it }, onUpdateItem, onMovePurchase, onDeleteItem, Modifier.weight(1f), onCompletedDelete = { showCompletedDeleteConfirmDialog = true }, onAllocation = onAllocation)
        }
    }

	if (editingItem != null) {
		val current = editingItem!!
		EditShoppingListItemDialog(
			title = stringResource(R.string.edit_item),
            showQuantity = listWithItems.items.find { it.item.id == current.id }?.days.isNullOrEmpty(),
			initialName = current.name,
			initialAmount = current.amount,
			initialUnit = current.unit,
			onDismiss = { editingItem = null },
			onConfirm = { newName, newAmount, newUnit ->
				editingItem = null
				onUpdateItem(
					current.copy(
						name = newName,
						amount = newAmount,
						unit = newUnit,
					),
				)
			},
		)
	}

	if (showRenameDialog) {
		RenameShoppingListDialog(
			currentName = listWithItems.list.name,
			onDismiss = { showRenameDialog = false },
			onConfirm = { newName ->
				showRenameDialog = false
				onRenameList(newName)
			},
		)
	}

	if (showDeleteConfirmDialog || showCompletedDeleteConfirmDialog) {
		AlertDialog(
			onDismissRequest = {
				showDeleteConfirmDialog = false
				showCompletedDeleteConfirmDialog = false
			},
			title = { Text(stringResource(R.string.delete)) },
			text = { Text(if (showCompletedDeleteConfirmDialog) stringResource(R.string.delete_completed_list_confirm) else listWithItems.list.name) },
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteConfirmDialog = false
						showCompletedDeleteConfirmDialog = false
						onDeleteList()
					},
					enabled = !isOffline,
				) {
					Text(
						stringResource(R.string.delete),
						color = MaterialTheme.colorScheme.error,
					)
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showDeleteConfirmDialog = false
						showCompletedDeleteConfirmDialog = false
					},
				) {
					Text(stringResource(R.string.cancel))
				}
			},
		)
	}
}

@Composable
internal fun CompletedListCard(
	onDeleteClick: () -> Unit,
) {
	val scale by animateFloatAsState(
		targetValue = 1f,
		animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
		label = "completion_scale",
	)

	AppCard(
		modifier = Modifier
			.fillMaxWidth()
			.scale(scale)
			.padding(vertical = 8.dp),
		shape = MaterialTheme.shapes.large,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(AppSpace.card),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(AppSpace.item),
		) {
			Icon(
				imageVector = Icons.Rounded.CheckCircle,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(56.dp),
			)

			Text(
				text = stringResource(R.string.all_items_completed_title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurface,
			)

			Text(
				text = stringResource(R.string.all_items_completed_desc),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			Button(
				onClick = onDeleteClick,
				enabled = !LocalOfflineMode.current,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.errorContainer,
					contentColor = MaterialTheme.colorScheme.onErrorContainer,
				),
			) {
				Icon(Icons.Rounded.Delete, contentDescription = null)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = stringResource(R.string.delete_completed_list),
					fontWeight = FontWeight.Bold,
				)
			}
		}
	}
}

@Composable
private fun EditShoppingListItemDialog(
	title: String,
	initialName: String,
    showQuantity: Boolean = true,
	initialAmount: String = "",
	initialUnit: QuantityUnit = QuantityUnit.GRAMS,
	onDismiss: () -> Unit,
	onConfirm: (name: String, amount: String, unit: QuantityUnit) -> Unit,
) {
	var name by remember { mutableStateOf(initialName) }
	var amount by remember { mutableStateOf(initialAmount) }
	var selectedUnit by remember { mutableStateOf(initialUnit) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(AppSpace.item),
			) {
				OutlinedTextField(
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
					value = name,
					onValueChange = { name = it },
					label = { Text(stringResource(R.string.item_name)) },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
				)

                if (showQuantity) QuantityInputRow(
					amount = amount,
					selectedUnit = selectedUnit,
					onAmountChange = { amount = it },
					onUnitChange = { selectedUnit = it },
				)
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					if (name.isNotBlank()) {
						val cleanAmount = if (selectedUnit.isNoAmountNeeded) "" else amount.trim()
						onConfirm(name.trim(), cleanAmount, selectedUnit)
					}
				},
				enabled = name.isNotBlank(),
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
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
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
				enabled = name.isNotBlank(),
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

@Preview(showBackground = true)
@Composable
private fun ShoppingListDetailContentPreview() {
	DietTheme {
		ShoppingListDetailContent(
			listWithItems = ShoppingListWithItems(
				list = it.matato.dietreminder.data.database.entity.ShoppingList(
					id = 1,
					name = "Lista della spesa 26/09/2026"),
				items = listOf(
					ShoppingListItemWithDays(
						item = ShoppingListItem(
							id = 1,
							shoppingListId = 1,
							name = "Biscotti",
							amount = "315",
							unit = QuantityUnit.GRAMS,
							isBought = false),
						days = emptyList(),
					),
					ShoppingListItemWithDays(
						item = ShoppingListItem(
							id = 2,
							shoppingListId = 1,
							name = "Salmone",
							isFresh = true,
							isBought = false),
						days = listOf(
							ShoppingListItemDay(
								id = 10,
								shoppingListItemId = 2,
								dayOfWeek = java.time.DayOfWeek.MONDAY,
								amount = "150",
								unit = QuantityUnit.GRAMS,
								isBought = true),
							ShoppingListItemDay(
								id = 11,
								shoppingListItemId = 2,
								dayOfWeek = java.time.DayOfWeek.THURSDAY,
								amount = "150",
								unit = QuantityUnit.GRAMS,
								isBought = false),
						),
					),
				),
			),
			hasActiveDiet = true,
			onBack = {},
			onToggleItem = {},
			onToggleDay = { _, _ -> },
			onDeleteItem = {},
			onUpdateItem = {},
			onAddCustomClick = {},
			onImportDietClick = {},
			onDeleteList = {},
			onRenameList = {},
		)
	}
}
