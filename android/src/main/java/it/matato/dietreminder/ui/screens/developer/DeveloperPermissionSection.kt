package it.matato.dietreminder.ui.screens.developer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.ui.screens.settings.SettingsSection
import it.matato.dietreminder.ui.screens.settings.SettingsListItem

@Composable
fun DeveloperPermissionsSection(onCheckPermissions: () -> Unit) {
    SettingsSection(stringResource(R.string.developer_permissions), Icons.Rounded.Security) {
        SettingsListItem(stringResource(R.string.check_app_permissions),
            stringResource(R.string.check_app_permissions_description), Icons.Rounded.Security, onClick = onCheckPermissions)
    }
}
