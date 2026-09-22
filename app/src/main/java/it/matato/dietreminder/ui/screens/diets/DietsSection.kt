package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DietsSection(
    diets: List<Diet>,
    onActivate: (Diet) -> Unit,
    onDuplicate: (Diet) -> Unit,
    onDelete: (Diet) -> Unit,
    onConfigure: (Diet) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            Text(
                text = stringResource(R.string.diets),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        if (diets.isEmpty()) {
            EmptyDietsCard()
            return
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column {
                diets.forEachIndexed { index, diet ->
                    DietsListItem(
                        diet = diet,
                        onActivate = { onActivate(diet) },
                        onDuplicate = { onDuplicate(diet) },
                        onDelete = { onDelete(diet) },
                        onConfigure = { onConfigure(diet) },
                    )

                    if (index < diets.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DietsSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DietsSection(
                diets = listOf(
                    Diet(id = 1, name = "Weight Loss", nextMealWindowMinutes = 90, isActive = true),
                    Diet(id = 2, name = "Muscle Gain", nextMealWindowMinutes = 60, isActive = false),
                ),
                onActivate = {},
                onDuplicate = {},
                onDelete = {},
                onConfigure = {},
            )
        }
    }
}