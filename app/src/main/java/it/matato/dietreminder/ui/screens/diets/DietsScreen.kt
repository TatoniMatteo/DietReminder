package it.matato.dietreminder.ui.screens.diets

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.viewmodel.DietViewModel

@Composable
fun DietsScreen(
    vm: DietViewModel, padding: PaddingValues, onConfigDiet: (Long) -> Unit
) {
    val diets by vm.diets.collectAsState()
    var create by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(padding), floatingActionButton = {
            FloatingActionButton(
                onClick = { create = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add, contentDescription = stringResource(R.string.new_diet)
                )
            }
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                horizontal = 20.dp, vertical = 24.dp
            )
        ) {
            item {
                DietsHeader(
                    count = diets.size
                )
            }

            item {
                DietsSection(diets = diets, onActivate = { diet ->
                    vm.activate(diet.id)
                }, onDuplicate = { diet ->
                    vm.duplicate(diet.id)
                }, onDelete = { diet ->
                    vm.deleteDiet(diet.id)
                }, onConfigure = { diet ->
                    onConfigDiet(diet.id)
                })
            }
        }
    }

    if (create) {
        NewDietDialog(onDismiss = {
            create = false
        }, onCreate = { name, window ->
            vm.create(name, window)
            create = false
        })
    }
}

@Composable
private fun DietsHeader(
    count: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.diets), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DietsSection(
    diets: List<Diet>,
    onActivate: (Diet) -> Unit,
    onDuplicate: (Diet) -> Unit,
    onDelete: (Diet) -> Unit,
    onConfigure: (Diet) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stringResource(R.string.diets),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (diets.isEmpty()) {
            EmptyDietsCard()
            return
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                diets.forEachIndexed { index, diet ->
                    DietListItem(diet = diet, onActivate = {
                        onActivate(diet)
                    }, onDuplicate = {
                        onDuplicate(diet)
                    }, onDelete = {
                        onDelete(diet)
                    }, onConfigure = {
                        onConfigure(diet)
                    })

                    if (index < diets.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DietListItem(
    diet: Diet, onActivate: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit, onConfigure: () -> Unit
) {
    var menuExpanded by rememberSaveable(
        diet.id
    ) {
        mutableStateOf(false)
    }

    val backgroundColor = if (diet.isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        ListItem(modifier = Modifier.clickable(onClick = onConfigure), headlineContent = {
            Text(
                text = diet.name, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
        }, supportingContent = {
            Text(
                text = stringResource(
                    R.string.window_minutes_value, diet.nextMealWindowMinutes
                ), color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }, leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (diet.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (diet.isActive) {
                        Icons.Rounded.Check
                    } else {
                        Icons.Rounded.Restaurant
                    }, contentDescription = null, tint = if (diet.isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }, modifier = Modifier.size(21.dp)
                )
            }
        }, trailingContent = {
            Box {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.configure)
                    )
                }

                DietActionsMenu(expanded = menuExpanded, isActive = diet.isActive, onDismiss = {
                    menuExpanded = false
                }, onActivate = {
                    menuExpanded = false
                    onActivate()
                }, onConfigure = {
                    menuExpanded = false
                    onConfigure()
                }, onDuplicate = {
                    menuExpanded = false
                    onDuplicate()
                }, onDelete = {
                    menuExpanded = false
                    onDelete()
                })
            }
        })
    }
}

@Composable
private fun DietActionsMenu(
    expanded: Boolean,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onConfigure: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss
    ) {
        if (!isActive) {
            DropdownMenuItem(
                text = {
                Text(stringResource(R.string.activate))
            }, leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Check, contentDescription = null
                )
            }, onClick = onActivate
            )
        }

        DropdownMenuItem(
            text = {
            Text(stringResource(R.string.configure))
        }, leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Tune, contentDescription = null
            )
        }, onClick = onConfigure
        )

        DropdownMenuItem(
            text = {
            Text(stringResource(R.string.duplicate))
        }, leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.ContentCopy, contentDescription = null
            )
        }, onClick = onDuplicate
        )

        DropdownMenuItem(
            text = {
            Text(
                text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error
            )
        }, leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error
            )
        }, onClick = onDelete
        )
    }
}

@Composable
private fun EmptyDietsCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = stringResource(R.string.diets), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.new_diet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewDietDialog(
    onDismiss: () -> Unit, onCreate: (String, Int) -> Unit
) {
    var name by rememberSaveable {
        mutableStateOf("")
    }

    var window by rememberSaveable {
        mutableStateOf("90")
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.new_diet),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name, onValueChange = {
                    name = it
                }, label = {
                    Text(stringResource(R.string.diet_name))
                }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = window, onValueChange = {
                    window = it.filter(Char::isDigit)
                }, label = {
                    Text(stringResource(R.string.window_minutes))
                }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            onCreate(
                                name, window.toIntOrNull() ?: 90
                            )
                        }, enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.create))
                    }
                }
            }
        }
    }
}
