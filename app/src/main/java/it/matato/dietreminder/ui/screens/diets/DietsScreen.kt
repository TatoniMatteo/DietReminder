package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel

@Composable
fun DietsScreen(
    vm: DietViewModel,
    onConfigDiet: (Long) -> Unit,
) {
    val diets by vm.diets.collectAsState()
    var create by rememberSaveable { mutableStateOf(false) }

    DietsContent(
        diets = diets,
        onCreateClick = { create = true },
        onActivate = { vm.activate(it.id) },
        onDuplicate = { vm.duplicate(it.id) },
        onDelete = { vm.deleteDiet(it.id) },
        onConfigure = { onConfigDiet(it.id) }
    )

    if (create) {
        NewDietDialog(
            onDismiss = { create = false },
            onCreate = { name, window ->
                vm.create(name, window)
                create = false
            },
        )
    }
}

@Composable
fun DietsContent(
    diets: List<Diet>,
    onCreateClick: () -> Unit,
    onActivate: (Diet) -> Unit,
    onDuplicate: (Diet) -> Unit,
    onDelete: (Diet) -> Unit,
    onConfigure: (Diet) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_diet),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {
            item {
                DietsHeader(count = diets.size)
            }

            item {
                DietsSection(
                    diets = diets,
                    onActivate = onActivate,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onConfigure = onConfigure,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DietsContentPreview() {
    DietTheme {
        DietsContent(
            diets = listOf(
                Diet(id = 1, name = "Summer Diet", nextMealWindowMinutes = 90, isActive = true),
                Diet(id = 2, name = "Winter Diet", nextMealWindowMinutes = 60, isActive = false),
            ),
            onCreateClick = {},
            onActivate = {},
            onDuplicate = {},
            onDelete = {},
            onConfigure = {}
        )
    }
}