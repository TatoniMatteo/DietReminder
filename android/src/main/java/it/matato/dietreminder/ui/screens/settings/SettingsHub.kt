package it.matato.dietreminder.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.ui.screens.account.LocalAccountViewModel
import it.matato.dietreminder.ui.theme.AppSpace

internal enum class SettingsPage(val key: String, @StringRes val title: Int, @StringRes val description: Int, val icon: ImageVector) {
    APPEARANCE("appearance",R.string.settings_page_appearance,R.string.settings_appearance_summary,Icons.Rounded.Palette),
    ACCOUNT("account",R.string.settings_page_account,R.string.settings_account_summary,Icons.Rounded.Security),
    PLANNING("planning",R.string.planning,R.string.settings_planning_summary,Icons.Rounded.Schedule),
    NOTIFICATIONS("notifications",R.string.notifications_alarms,R.string.settings_notifications_summary,Icons.Rounded.Notifications),
    BACKUP("backup",R.string.backup_data,R.string.settings_backup_summary,Icons.Rounded.CloudUpload),
    ABOUT("about",R.string.app_info,R.string.settings_about_summary,Icons.Rounded.Info),
    DEVELOPER("developer",R.string.settings_page_developer,R.string.settings_developer_summary,Icons.Rounded.Code),
}

@Composable
internal fun SettingsAccountSummary(onClick: () -> Unit) {
    val vm = LocalAccountViewModel.current
    val state = vm?.account?.collectAsState()?.value
    AppCard(Modifier.fillMaxWidth().testTag("settingsAccountSummary").clickable(onClick = onClick)) {
        Row(Modifier.padding(AppSpace.card), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            Icon(Icons.Rounded.AccountCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(state?.session?.account?.username ?: stringResource(R.string.settings_page_account), style = MaterialTheme.typography.titleLarge)
                Text(state?.session?.account?.email ?: stringResource(R.string.account_local_mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsHub(developer: Boolean, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.section)) {
        for ((title, entries) in listOf(R.string.settings_group_personalize to listOf(SettingsPage.APPEARANCE), R.string.settings_group_organize to listOf(SettingsPage.PLANNING,SettingsPage.NOTIFICATIONS), R.string.settings_group_data to listOf(SettingsPage.BACKUP,SettingsPage.ABOUT) + if (developer) listOf(SettingsPage.DEVELOPER) else emptyList())) {
            SettingsSection(stringResource(title), entries.first().icon) {
                entries.forEachIndexed { index, page ->
                    if (index > 0) SettingsDivider()
                    Box(Modifier.testTag("settingsPage_${page.key}")) {
                        SettingsListItem(stringResource(page.title),stringResource(page.description),page.icon,
                            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { onOpen(page.key) })
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsShoppingNotificationsLink(onOpen: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_shopping_notifications),Icons.Rounded.ShoppingCart) {
        SettingsListItem(stringResource(R.string.nav_shopping_lists),stringResource(R.string.settings_shopping_reminder_summary),Icons.Rounded.ShoppingCart,onClick=onOpen)
    }
}
