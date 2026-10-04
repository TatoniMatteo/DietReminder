package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
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
) {
	var editingItem by remember { mutableStateOf<ShoppingListItem?>(null) }
	var showMenu by remember { mutableStateOf(false) }
	var showRenameDialog by remember { mutableStateOf(false) }
	var showDeleteConfirmDialog by remember { mutableStateOf(false) }
	var showCompletedDeleteConfirmDialog by remember { mutableStateOf(false) }
	var fabExpanded by remember { mutableStateOf(false) }
	var purchasedExpanded by remember { mutableStateOf(false) }

	val pendingItems = remember(listWithItems.items) {
		listWithItems.items.filter { !it.isAllDaysBought }
	}
	val boughtItems = remember(listWithItems.items) {
		listWithItems.items.filter { it.isAllDaysBought }
	}

	val totalCount = listWithItems.items.size
	val boughtCount = boughtItems.size
	val progress = if (totalCount > 0) boughtCount.toFloat() / totalCount.toFloat() else 0f
	val isAllCompleted = totalCount > 0 && boughtCount == totalCount

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
		label = "progress_anim",
	)

	val purchasedChevronRotation by animateFloatAsState(
		targetValue = if (purchasedExpanded) 180f else 0f,
		animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
		label = "purchased_chevron_rotation",
	)

	Scaffold(
		topBar = {
			Column {
				TopAppBar(
					title = {
						Text(
							text = listWithItems.list.name,
							fontWeight = FontWeight.Bold,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
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
					actions = {
						Box {
							IconButton(onClick = { showMenu = true }) {
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
                            .height(6.dp),
					)
					Row(
						modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(
								R.string.items_purchased_count,
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
				FloatingActionButton(
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
			if (listWithItems.items.isEmpty()) {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					EmptyShoppingListItemsState()
				}
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(
						start = 16.dp,
						end = 16.dp,
						top = 12.dp,
						bottom = 88.dp,
					),
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					// Completion Card Animation
					if (isAllCompleted) {
						item {
							CompletedListCard(
								onDeleteClick = { showCompletedDeleteConfirmDialog = true },
							)
						}
					}

					// Section 1: Da acquistare (To Buy)
					if (pendingItems.isNotEmpty()) {
						item {
							SectionHeader(
								title = stringResource(R.string.to_buy),
								count = pendingItems.size,
							)
						}

						items(
							items = pendingItems,
							key = { it.item.id },
						) { itemWithDays ->
							Box(modifier = Modifier.animateItem()) {
								if (itemWithDays.item.isFresh && itemWithDays.days.isNotEmpty()) {
									FreshShoppingListItemCard(
										itemWithDays = itemWithDays,
										onToggleItem = { onToggleItem(itemWithDays.item) },
										onToggleDay = onToggleDay,
										onEdit = { editingItem = itemWithDays.item },
										onDelete = { onDeleteItem(itemWithDays.item.id) },
									)
								} else {
									ShoppingListItemRow(
										item = itemWithDays.item,
										onToggle = { onToggleItem(itemWithDays.item) },
										onEdit = { editingItem = itemWithDays.item },
										onDelete = { onDeleteItem(itemWithDays.item.id) },
									)
								}
							}
						}
					}

					// Section 2: Acquistati (Purchased - Collapsible, default collapsed)
					if (boughtItems.isNotEmpty()) {
						item {
							Spacer(modifier = Modifier.height(8.dp))
							Row(
								modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { purchasedExpanded = !purchasedExpanded }
                                    .padding(vertical = 4.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								SectionHeader(
									title = stringResource(R.string.purchased),
									count = boughtItems.size,
								)
								Spacer(modifier = Modifier.weight(1f))
								Icon(
									imageVector = Icons.Rounded.KeyboardArrowDown,
									contentDescription = null,
									modifier = Modifier.graphicsLayer { rotationZ = purchasedChevronRotation },
									tint = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							}
						}

						if (purchasedExpanded) {
							items(
								items = boughtItems,
								key = { it.item.id },
							) { itemWithDays ->
								Box(modifier = Modifier.animateItem()) {
									if (itemWithDays.item.isFresh && itemWithDays.days.isNotEmpty()) {
										FreshShoppingListItemCard(
											itemWithDays = itemWithDays,
											onToggleItem = { onToggleItem(itemWithDays.item) },
											onToggleDay = onToggleDay,
											onEdit = { editingItem = itemWithDays.item },
											onDelete = { onDeleteItem(itemWithDays.item.id) },
										)
									} else {
										ShoppingListItemRow(
											item = itemWithDays.item,
											onToggle = { onToggleItem(itemWithDays.item) },
											onEdit = { editingItem = itemWithDays.item },
											onDelete = { onDeleteItem(itemWithDays.item.id) },
										)
									}
								}
							}
						}
					}
				}
			}
		}
	}

	if (editingItem != null) {
		val current = editingItem!!
		EditShoppingListItemDialog(
			title = stringResource(R.string.edit_item),
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
private fun FreshShoppingListItemCard(
	itemWithDays: ShoppingListItemWithDays,
	onToggleItem: () -> Unit,
	onToggleDay: (dayId: Long, isBought: Boolean) -> Unit,
	onEdit: () -> Unit,
	onDelete: () -> Unit,
) {
	var expanded by remember { mutableStateOf(false) }
	var showMenu by remember { mutableStateOf(false) }

	val totalDays = itemWithDays.days.size
	val boughtDays = itemWithDays.boughtDaysCount
	val isAllBought = itemWithDays.isAllDaysBought

	val chevronRotation by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
		label = "fresh_chevron_rotation",
	)

	OutlinedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.medium,
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
		) {
			Row(
				modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Checkbox(
					checked = isAllBought,
					onCheckedChange = { onToggleItem() },
				)

				Spacer(modifier = Modifier.width(8.dp))

				Column(
					modifier = Modifier.weight(1f),
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = itemWithDays.item.name,
							style = MaterialTheme.typography.bodyLarge,
							fontWeight = if (isAllBought) FontWeight.Normal else FontWeight.SemiBold,
							textDecoration = if (isAllBought) TextDecoration.LineThrough else TextDecoration.None,
							color = if (isAllBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.weight(1f, fill = false),
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
						)

						Spacer(modifier = Modifier.width(6.dp))

						Box(
							modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
						) {
							Text(
								text = stringResource(R.string.type_fresh_daily),
								style = MaterialTheme.typography.labelSmall,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.onTertiaryContainer,
								maxLines = 1,
							)
						}
					}

					Text(
						text = "$boughtDays/$totalDays " + stringResource(R.string.days_purchased),
						style = MaterialTheme.typography.bodySmall,
						color = if (isAllBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}

				Icon(
					imageVector = Icons.Rounded.KeyboardArrowDown,
					contentDescription = null,
					modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
				)

				Box {
					IconButton(onClick = { showMenu = true }) {
						Icon(
							imageVector = Icons.Rounded.MoreVert,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}

					DropdownMenu(
						expanded = showMenu,
						onDismissRequest = { showMenu = false },
					) {
						DropdownMenuItem(
							text = { Text(stringResource(R.string.edit_item)) },
							onClick = {
								showMenu = false
								onEdit()
							},
							leadingIcon = {
								Icon(Icons.Rounded.Edit, contentDescription = null)
							},
						)
						DropdownMenuItem(
							text = { Text(stringResource(R.string.delete)) },
							onClick = {
								showMenu = false
								onDelete()
							},
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

			AnimatedVisibility(visible = expanded) {
				Column(
					modifier = Modifier.fillMaxWidth(),
				) {
					HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

					itemWithDays.days.forEachIndexed { index, day ->
						val dayName = day.dayOfWeek.getDisplayName(
							TextStyle.FULL,
							LocalLocale.current.platformLocale,
						).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

						Row(
							modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDay(day.id, !day.isBought) }
                                .padding(horizontal = 24.dp, vertical = 6.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Checkbox(
								checked = day.isBought,
								onCheckedChange = { onToggleDay(day.id, !day.isBought) },
							)

							Spacer(modifier = Modifier.width(8.dp))

							Text(
								text = dayName,
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = FontWeight.SemiBold,
								textDecoration = if (day.isBought) TextDecoration.LineThrough else TextDecoration.None,
								color = if (day.isBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
								modifier = Modifier.weight(1f),
							)

							Text(
								text = day.displayQuantity,
								style = MaterialTheme.typography.bodySmall,
								fontWeight = FontWeight.Bold,
								textDecoration = if (day.isBought) TextDecoration.LineThrough else TextDecoration.None,
								color = if (day.isBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
							)
						}

						if (index < itemWithDays.days.lastIndex) {
							HorizontalDivider(
								modifier = Modifier.padding(horizontal = 24.dp),
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
private fun CompletedListCard(
	onDeleteClick: () -> Unit,
) {
	val scale by animateFloatAsState(
		targetValue = 1f,
		animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
		label = "completion_scale",
	)

	OutlinedCard(
		modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 8.dp),
		shape = MaterialTheme.shapes.large,
	) {
		Column(
			modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp),
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
private fun SectionHeader(title: String, count: Int) {
	Row(
		modifier = Modifier.padding(vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.primary,
		)

		Spacer(modifier = Modifier.width(8.dp))

		Box(
			modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp),
		) {
			Text(
				text = count.toString(),
				style = MaterialTheme.typography.labelSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
			)
		}
	}
}

@Composable
private fun ShoppingListItemRow(
	item: ShoppingListItem,
	onToggle: () -> Unit,
	onEdit: () -> Unit,
	onDelete: () -> Unit,
) {
	var showMenu by remember { mutableStateOf(false) }

	OutlinedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.medium,
	) {
		Row(
			modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Checkbox(
				checked = item.isBought,
				onCheckedChange = { onToggle() },
			)

			Spacer(modifier = Modifier.width(8.dp))

			Column(
				modifier = Modifier.weight(1f),
			) {
				Text(
					text = item.name,
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = if (item.isBought) FontWeight.Normal else FontWeight.SemiBold,
					textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
					color = if (item.isBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
				)

				if (item.displayQuantity.isNotBlank()) {
					Text(
						text = item.displayQuantity,
						style = MaterialTheme.typography.bodySmall,
						textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
						color = if (item.isBought) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}

			Box {
				IconButton(onClick = { showMenu = true }) {
					Icon(
						imageVector = Icons.Rounded.MoreVert,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}

				DropdownMenu(
					expanded = showMenu,
					onDismissRequest = { showMenu = false },
				) {
					DropdownMenuItem(
						text = { Text(stringResource(R.string.edit_item)) },
						onClick = {
							showMenu = false
							onEdit()
						},
						leadingIcon = {
							Icon(Icons.Rounded.Edit, contentDescription = null)
						},
					)
					DropdownMenuItem(
						text = { Text(stringResource(R.string.delete)) },
						onClick = {
							showMenu = false
							onDelete()
						},
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
	}
}

@Composable
private fun EditShoppingListItemDialog(
	title: String,
	initialName: String,
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
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text(stringResource(R.string.item_name)) },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
				)

				QuantityInputRow(
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

@Composable
private fun EmptyShoppingListItemsState() {
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
			text = stringResource(R.string.no_items_in_list),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
		)
	}
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
