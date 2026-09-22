package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.dialog.DeveloperDangerDialog
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DeveloperDangerZone(
    onResetDatabase: () -> Unit,
    onDisableDeveloperMode: () -> Unit,
) {
    val error = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer

    var dialog by remember { mutableStateOf<DangerDialogState?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.danger_zone),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = error,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = errorContainer.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DangerAction(
                    icon = Icons.Rounded.DeveloperMode,
                    title = stringResource(R.string.disable_developer_mode),
                    description = stringResource(R.string.disable_developer_mode_description),
                    onClick = {
                        dialog = DangerDialogState(
                            title = R.string.disable_developer_mode,
                            message = R.string.disable_developer_mode_confirmation,
                            onConfirm = onDisableDeveloperMode,
                        )
                    },
                )

                DangerAction(
                    icon = Icons.Rounded.DeleteForever,
                    title = stringResource(R.string.reset_database),
                    description = stringResource(R.string.reset_database_description),
                    onClick = {
                        dialog = DangerDialogState(
                            title = R.string.reset_database,
                            message = R.string.reset_database_confirmation,
                            onConfirm = onResetDatabase,
                        )
                    },
                )
            }
        }
    }

    dialog?.let { state ->
        DeveloperDangerDialog(
            title = stringResource(state.title),
            message = stringResource(state.message),
            onDismiss = {
                dialog = null
            },
            onConfirm = {
                dialog = null
                state.onConfirm()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperDangerZonePreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DeveloperDangerZone(
                onResetDatabase = {},
                onDisableDeveloperMode = {},
            )
        }
    }
}

private data class DangerDialogState(
    val title: Int,
    val message: Int,
    val onConfirm: () -> Unit,
)

@Composable
private fun DangerAction(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}