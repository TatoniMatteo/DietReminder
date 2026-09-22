package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDietDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var window by rememberSaveable { mutableFloatStateOf(90f) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        NewDietDialogContent(
            name = name,
            window = window,
            onNameChange = { name = it },
            onWindowChange = { window = it },
            onDismiss = onDismiss,
            onCreate = { onCreate(name.trim(), window.toInt()) }
        )
    }
}

@Composable
fun NewDietDialogContent(
    name: String,
    window: Float,
    onNameChange: (String) -> Unit,
    onWindowChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = stringResource(R.string.new_diet),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = {
                    Text(stringResource(R.string.diet_name))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.window_minutes),
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Text(
                        text = "${window.toInt()} min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Slider(
                    value = window,
                    onValueChange = onWindowChange,
                    valueRange = 0f..120f,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = onCreate,
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewDietDialogContentPreview() {
    DietTheme {
        NewDietDialogContent(
            name = "My New Diet",
            window = 60f,
            onNameChange = {},
            onWindowChange = {},
            onDismiss = {},
            onCreate = {}
        )
    }
}