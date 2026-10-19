package it.matato.dietreminder.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.auth.ServerAddress
import it.matato.dietreminder.viewmodel.AccountViewModel

@Composable
fun ServerConfigurationScreen(vm: AccountViewModel) {
    val state by vm.account.collectAsState()
    val busy by vm.busy.collectAsState()
    ServerConfigurationContent(
        currentUrl = state.serverUrl,
        signedIn = state.session != null,
        busy = busy,
        canCancel = state.serverUrl.isNotEmpty() || (state.serverSetupComplete && !state.cloud),
        onSave = vm::configureServer,
        onLocal = vm::useLocal,
        onCancel = vm::closeServerConfiguration,
    )
}

@Composable
internal fun ServerConfigurationContent(
    currentUrl: String,
    signedIn: Boolean,
    busy: Boolean,
    canCancel: Boolean,
    onSave: (String) -> Unit,
    onLocal: () -> Unit,
    onCancel: () -> Unit,
) {
    var address by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = canCancel && !busy, onBack = onCancel)
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.server_configure), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.server_description), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = address, onValueChange = { address = it; invalid = false },
                label = { Text(stringResource(R.string.server_address)) },
                placeholder = { Text("https://dietreminder.example.com/") },
                singleLine = true, enabled = !busy, isError = invalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = { Text(stringResource(if (invalid) R.string.server_invalid else R.string.server_address_help)) },
                modifier = Modifier.fillMaxWidth().testTag("serverAddress"),
            )
            Button(
                enabled = !busy && address.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("serverSave"),
                onClick = {
                    val normalized = runCatching { ServerAddress.normalize(address) }.getOrNull()
                    if (normalized == null) invalid = true
                    else if (signedIn && normalized != currentUrl) pending = normalized
                    else onSave(normalized)
                },
            ) { Text(stringResource(R.string.server_continue)) }
            TextButton(onClick = onLocal, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_continue_local))
            }
            if (canCancel) TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.back)) }
        }
    }
    pending?.let { url ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.server_change)) },
            text = { Text(stringResource(R.string.server_change_warning)) },
            confirmButton = { TextButton(onClick = { pending = null; onSave(url) }) { Text(stringResource(R.string.server_continue)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.back)) } },
        )
    }
}
