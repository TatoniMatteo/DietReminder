package it.matato.dietreminder.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.viewmodel.AccountViewModel

@Composable
fun CloudSyncSection(vm: AccountViewModel) {
    val status by vm.syncStatus.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var resolution by remember { mutableStateOf<Boolean?>(null) }
    Text(stringResource(when {
        status.conflict -> R.string.cloud_sync_conflict
        status.running -> R.string.cloud_sync_running
        status.error != null -> R.string.cloud_sync_error
        status.pending -> R.string.cloud_sync_pending
        status.lastSyncedAt > 0 -> R.string.cloud_sync_done
        else -> R.string.cloud_sync_waiting
    }), style = MaterialTheme.typography.bodySmall)
    if (status.running || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message != null && message?.startsWith("BACKUP_") != true) Text(stringResource(R.string.cloud_sync_action_error), color = MaterialTheme.colorScheme.error)
    if (status.conflict) {
        TextButton(enabled = !busy && !status.running, onClick = { resolution = true }) { Text(stringResource(R.string.cloud_use_server)) }
        TextButton(enabled = !busy && !status.running, onClick = { resolution = false }) { Text(stringResource(R.string.cloud_keep_local)) }
    } else {
        TextButton(enabled = !status.running && !busy, onClick = vm::synchronize) { Text(stringResource(R.string.cloud_sync_now)) }
    }
    resolution?.let { useServer ->
        AlertDialog(
            onDismissRequest = { resolution = null },
            title = { Text(stringResource(if (useServer) R.string.cloud_use_server else R.string.cloud_keep_local)) },
            text = { Text(stringResource(if (useServer) R.string.cloud_replace_local_warning else R.string.cloud_replace_server_warning)) },
            confirmButton = { TextButton(onClick = { resolution = null; vm.resolveSync(useServer) }) { Text(stringResource(R.string.account_continue)) } },
            dismissButton = { TextButton(onClick = { resolution = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
