package it.matato.dietreminder.ui.screens.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.matato.dietreminder.R
import it.matato.dietreminder.viewmodel.AccountViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GoogleBackupSection(vm: AccountViewModel) {
    val account by vm.account.collectAsState()
    val cached by vm.googleBackupState.collectAsState()
    val status = cached.status.takeIf { cached.accountId == account.session?.account?.userId }
    val backups = cached.backups.takeIf { cached.accountId == account.session?.account?.userId } ?: emptyList()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var confirmation by remember { mutableStateOf<Pair<String, String>?>(null) }
    DisposableEffect(owner, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshGoogleBackups() }
        owner.lifecycle.addObserver(observer)
        vm.refreshGoogleBackups()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text(stringResource(R.string.google_backup_title), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.google_backup_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (status?.available == false) Text(stringResource(R.string.google_not_configured), style = MaterialTheme.typography.bodySmall)
    if (status?.available == true) {
        status.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { vm.connectGoogleDrive { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }) { Text(stringResource(R.string.google_backup_connect)) }
            TextButton(enabled = !busy, onClick = vm::refreshGoogleBackups) { Text(stringResource(R.string.google_backup_refresh)) }
        }
        if (status.connected) {
            Button(enabled = !busy, onClick = vm::createGoogleBackup) { Text(stringResource(R.string.google_backup_create)) }
            if (backups.isEmpty()) Text(stringResource(R.string.google_backup_empty), style = MaterialTheme.typography.bodySmall)
            backups.forEach { backup ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(runCatching { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(backup.createdTime)) }.getOrDefault(backup.name), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !busy, onClick = { confirmation = "restore" to backup.id }) { Text(stringResource(R.string.google_backup_restore)) }
                    IconButton(enabled = !busy, onClick = { confirmation = "delete" to backup.id }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.google_backup_delete)) }
                }
            }
            TextButton(enabled = !busy, onClick = { confirmation = "disconnect" to "" }) { Text(stringResource(R.string.google_backup_disconnect)) }
        }
    }
    if (message != null) {
        val text = when (message) {
            "BACKUP_CREATED" -> R.string.google_backup_created
            "BACKUP_RESTORED" -> R.string.google_backup_restored
            "BACKUP_SYNC_REQUIRED" -> R.string.google_backup_sync_required
            else -> R.string.google_backup_error
        }
        Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = if (message in listOf("BACKUP_CREATED", "BACKUP_RESTORED")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
    confirmation?.let { (action, id) ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(stringResource(R.string.google_backup_title)) },
            text = { Text(stringResource(when (action) { "restore" -> R.string.google_backup_restore_warning; "delete" -> R.string.google_backup_delete_warning; else -> R.string.google_backup_disconnect_warning })) },
            confirmButton = { TextButton(onClick = { confirmation = null; when (action) { "restore" -> vm.restoreGoogleBackup(id); "delete" -> vm.deleteGoogleBackup(id); else -> vm.disconnectGoogleDrive() } }) { Text(stringResource(R.string.account_continue)) } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
