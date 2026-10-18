package it.matato.dietreminder.ui.screens.account

import it.matato.dietreminder.common.model.PasswordRules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun AccountBackupSettingsSection() {
    val vm = LocalAccountViewModel.current ?: return
    val state by vm.account.collectAsState()
    if (state.session != null) AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppSpace.card)) { GoogleBackupSection(vm) } }
    else Text(stringResource(R.string.settings_backup_login_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun AccountSecuritySection() {
    val vm = LocalAccountViewModel.current ?: return
    val state by vm.account.collectAsState()
    val email = state.session?.account?.email ?: return
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var open by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            Text(stringResource(R.string.settings_security_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_password_description),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton({ vm.clearMessage(); open = !open }, enabled = !busy) { Text(stringResource(R.string.settings_change_password)) }
            if (open) Column(verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
                Text(email)
                OutlinedTextField(current, { current = it.take(128) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.password_current)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy)
                OutlinedTextField(password, { password = it.take(128) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.password_new)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy)
                OutlinedTextField(confirmation, { confirmation = it.take(128) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.account_password_confirm)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy)
                PasswordCriteria(password, confirmation, current)
                Text(stringResource(R.string.settings_reset_session_hint), style = MaterialTheme.typography.bodySmall)
                if (message != null) Text(stringResource(accountMessage(message!!)), color = MaterialTheme.colorScheme.error)
                Button({ vm.changeCurrentPassword(current, password) }, enabled = !busy && current.isNotEmpty() && PasswordRules.isValid(password) && password == confirmation && password != current) { Text(stringResource(R.string.account_save_password)) }
                TextButton({ open = false; current = ""; password = ""; confirmation = ""; vm.clearMessage() }, enabled = !busy) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}
