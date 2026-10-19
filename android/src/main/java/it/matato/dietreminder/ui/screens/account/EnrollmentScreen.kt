package it.matato.dietreminder.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.common.model.EnrollmentStatus
import it.matato.dietreminder.viewmodel.AccountViewModel
import kotlinx.coroutines.delay

@Composable
fun EnrollmentScreen(vm: AccountViewModel) {
    val state by vm.account.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val history by vm.enrollmentHistory.collectAsState()
    val account = state.session?.account ?: return
    var code by rememberSaveable(account.userId) { mutableStateOf("") }
    LaunchedEffect(account.userId) {
        while (true) { vm.refreshEnrollment(); delay(15_000) }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(when (account.enrollmentStatus) {
                EnrollmentStatus.PENDING_EMAIL -> R.string.enrollment_verify_title
                EnrollmentStatus.PENDING_APPROVAL -> R.string.enrollment_pending_title
                EnrollmentStatus.REJECTED -> R.string.enrollment_rejected_title
                else -> R.string.enrollment_approved_title
            }), style = MaterialTheme.typography.headlineMedium)
            Text(account.email)
            Text(stringResource(R.string.enrollment_steps))
            if (account.enrollmentStatus == EnrollmentStatus.PENDING_EMAIL) {
                Text(stringResource(R.string.enrollment_verify_help))
                if (account.role == "ADMIN") Text(stringResource(R.string.enrollment_first_admin))
                OutlinedTextField(code, { code = it }, label = { Text(stringResource(R.string.enrollment_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { vm.verifyEmail(code) }, enabled = !busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enrollment_verify_action)) }
                TextButton(onClick = vm::resendVerification, enabled = !busy) { Text(stringResource(R.string.enrollment_resend)) }
            }
            if (account.enrollmentStatus == EnrollmentStatus.PENDING_APPROVAL) Text(stringResource(R.string.enrollment_pending_help))
            if (account.enrollmentStatus == EnrollmentStatus.REJECTED) Text(account.reviewReason ?: stringResource(R.string.enrollment_rejected_help))
            message?.let { Text(stringResource(accountMessage(it)), color = MaterialTheme.colorScheme.primary) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (history.isNotEmpty()) {
                Text(stringResource(R.string.enrollment_history), style = MaterialTheme.typography.titleMedium)
                history.forEach { event -> Text(event.message, style = MaterialTheme.typography.bodyMedium) }
            }
            OutlinedButton(onClick = vm::refreshEnrollment, enabled = !busy) { Text(stringResource(R.string.enrollment_refresh)) }
            TextButton(onClick = vm::logout, enabled = !busy) { Text(stringResource(R.string.account_logout)) }
        }
    }
}

@Composable
fun AdminIntroductionDialog(vm: AccountViewModel) {
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.admin_introduction_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.admin_introduction_welcome))
                Text(stringResource(R.string.admin_introduction_powers_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.admin_introduction_powers))
                Text(stringResource(R.string.admin_introduction_duties_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.admin_introduction_duties))
                Text(stringResource(R.string.admin_introduction_saved))
                message?.let { Text(stringResource(accountMessage(it)), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = vm::acknowledgeAdminIntroduction, enabled = !busy) { Text(stringResource(R.string.admin_introduction_confirm)) } }
    )
}
