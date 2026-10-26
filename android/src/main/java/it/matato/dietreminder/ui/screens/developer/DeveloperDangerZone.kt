package it.matato.dietreminder.ui.screens.developer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.ui.screens.settings.SettingsSection
import it.matato.dietreminder.ui.screens.settings.SettingsListItem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.dialog.DeveloperDangerDialog
import it.matato.dietreminder.ui.screens.settings.SettingsDivider

@Composable
fun DeveloperDangerZone(onResetDatabase: () -> Unit, onDisableDeveloperMode: () -> Unit) {
    var reset by remember { mutableStateOf<Boolean?>(null) }
    val offline = LocalOfflineMode.current
    SettingsSection(stringResource(R.string.danger_zone), Icons.Rounded.Warning) {
        SettingsListItem(stringResource(R.string.disable_developer_mode),
            stringResource(R.string.disable_developer_mode_description), Icons.Rounded.DeveloperMode,
            onClick = { reset = false })
        SettingsDivider()
        SettingsListItem(stringResource(R.string.reset_database),
            stringResource(R.string.reset_database_description), Icons.Rounded.DeleteForever,
            enabled = !offline, onClick = { reset = true })
    }
    reset?.let { isReset ->
        DeveloperDangerDialog(
            title = stringResource(if (isReset) R.string.reset_database else R.string.disable_developer_mode),
            message = stringResource(if (isReset) R.string.reset_database_confirmation else R.string.disable_developer_mode_confirmation),
            onDismiss = { reset = null },
            onConfirm = { reset = null; if (isReset) onResetDatabase() else onDisableDeveloperMode() },
        )
    }
}
