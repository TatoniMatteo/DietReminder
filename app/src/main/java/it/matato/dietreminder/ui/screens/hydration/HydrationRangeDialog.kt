package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHydrationRangeDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, Int) -> Unit,
) {
    val startState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true,
    )

    val endState = rememberTimePickerState(
        initialHour = 18,
        initialMinute = 0,
        is24Hour = true,
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        AddHydrationRangeDialogContent(
            startState = startState,
            endState = endState,
            onDismiss = onDismiss,
            onAdd = {
                onAdd(
                    startState.hour * 60 + startState.minute,
                    endState.hour * 60 + endState.minute,
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHydrationRangeDialogContent(
    startState: TimePickerState,
    endState: TimePickerState,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
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
                IconContainer(
                    icon = Icons.Rounded.Schedule,
                    size = 50.dp,
                    iconSize = 26.dp,
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = stringResource(R.string.new_window),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.start),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                TimeInput(state = startState)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.end),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                TimeInput(state = endState)
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
                    onClick = onAdd,
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AddHydrationRangeDialogContentPreview() {
    DietTheme {
        AddHydrationRangeDialogContent(
            startState = rememberTimePickerState(9, 0, true),
            endState = rememberTimePickerState(18, 0, true),
            onDismiss = {},
            onAdd = {}
        )
    }
}