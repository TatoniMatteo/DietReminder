package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.viewmodel.DietViewModel

@Composable
fun DietsScreen(
    vm: DietViewModel,
    padding: PaddingValues,
    onConfigDiet: (Long) -> Unit
) {
    val diets by vm.diets.collectAsState()
    var create by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { create = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Rounded.Add, stringResource(R.string.new_diet))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.diets),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(diets, key = Diet::id) { diet ->
                DietCard(
                    diet = diet,
                    onActivate = { vm.activate(diet.id) },
                    onDuplicate = { vm.duplicate(diet.id) },
                    onDelete = { vm.deleteDiet(diet.id) },
                    onClick = { onConfigDiet(diet.id) }
                )
            }
        }
    }

    if (create) {
        NewDietDialog(
            onDismiss = { create = false },
            onCreate = { name, window ->
                vm.create(name, window)
                create = false
            }
        )
    }
}

@Composable
private fun DietCard(
    diet: Diet,
    onActivate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = if (diet.isActive) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = diet.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (diet.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.window_minutes)}: ${diet.nextMealWindowMinutes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (diet.isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (diet.isActive) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.active),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!diet.isActive) {
                    Button(onClick = onActivate, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.activate))
                    }
                }
                OutlinedIconButton(onClick = onClick, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Rounded.Edit, stringResource(R.string.configure))
                }
                OutlinedIconButton(onClick = onDuplicate, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Rounded.ContentCopy, stringResource(R.string.duplicate))
                }
                OutlinedIconButton(
                    onClick = onDelete,
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDietDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var window by rememberSaveable { mutableStateOf("90") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.new_diet), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.diet_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = window,
                    onValueChange = { window = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.window_minutes)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = { onCreate(name, window.toIntOrNull() ?: 90) },
                        enabled = name.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.create))
                    }
                }
            }
        }
    }
}
