package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun SettingsBackupSection(
    diets: List<Diet>,
    onImport: () -> Unit,
    onExport: (Long, String) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.backup_data),
        icon = Icons.Rounded.Backup,
    ) {
        SettingsListItem(
            title = stringResource(R.string.import_diet),
            subtitle = stringResource(R.string.import_desc),
            leadingIcon = Icons.Rounded.FileUpload,
            onClick = onImport,
        )

        if (diets.isNotEmpty()) {
            SettingsDivider()

            diets.forEachIndexed { index, diet ->
                if (index > 0) {
                    SettingsDivider()
                }

                SettingsListItem(
                    title = stringResource(
                        R.string.export_name,
                        diet.name,
                    ),
                    leadingIcon = Icons.Rounded.FileDownload,
                    onClick = {
                        val fileName = "${diet.name.lowercase().replace(" ", "_")}.dr"
                        onExport(diet.id, fileName)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsBackupSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsBackupSection(
                diets = listOf(
                    Diet(id = 1, name = "Summer Diet", nextMealWindowMinutes = 90, isActive = true),
                ),
                onImport = {},
                onExport = { _, _ -> },
            )
        }
    }
}