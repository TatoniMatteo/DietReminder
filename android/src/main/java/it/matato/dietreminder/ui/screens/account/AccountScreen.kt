package it.matato.dietreminder.ui.screens.account

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.common.model.PasswordRules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.viewmodel.AccountViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import it.matato.dietreminder.data.auth.requestGoogleCredential
import it.matato.dietreminder.data.auth.clearGoogleCredential
import kotlinx.coroutines.launch

val LocalAccountViewModel = staticCompositionLocalOf<AccountViewModel?> { null }

@Composable
fun AccountScreen(vm: AccountViewModel) {
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val google by vm.googleProviders.collectAsState()
    val context = LocalContext.current
    val account by vm.account.collectAsState()
    LaunchedEffect(vm, account.serverUrl) { vm.loadGoogleConfiguration() }
    AccountAccessContent(
        onConfigureServer = vm::openServerConfiguration, serverUrl = account.serverUrl,
        busy = busy, message = message, firstRegistrationRequired = google.firstRegistrationRequired,
        onLogin = vm::login, onRegister = vm::register,
        onForgot = vm::forgot, onReset = vm::reset,
        onLocal = vm::useLocal, onClearMessage = vm::clearMessage,
        onGoogle = if (google.signInEnabled) ({ vm.googleSignIn({ id, nonce -> requestGoogleCredential(context, id, nonce) }) }) else null,
    )
}

@Composable
internal fun AccountAccessContent(
    busy: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onForgot: (String) -> Unit,
    onReset: (String, String) -> Unit,
    onLocal: () -> Unit,
    onClearMessage: () -> Unit,
    onGoogle: (() -> Unit)? = null,
    firstRegistrationRequired: Boolean = false,
    serverUrl: String = "",
    onConfigureServer: (() -> Unit)? = null,
) {
    var selectedMode by rememberSaveable { mutableStateOf("login") }
    val mode = if (firstRegistrationRequired) "register" else selectedMode
    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val valid = when (mode) {
        "forgot" -> email.isNotBlank()
        "reset" -> token.isNotBlank() && PasswordRules.isValid(password) && password == confirmation
        "register" -> email.isNotBlank() && username.isNotBlank() && PasswordRules.isValid(password) && password == confirmation
        else -> email.isNotBlank() && password.isNotBlank()
    }
    fun switchMode(value: String) {
        selectedMode = value
        password = ""
        confirmation = ""
        focusManager.clearFocus()
        onClearMessage()
    }
    fun submit() {
        if (busy || !valid) return
        focusManager.clearFocus()
        when (mode) {
            "register" -> onRegister(email.trim(), password, username.trim())
            "forgot" -> onForgot(email.trim())
            "reset" -> onReset(token.trim(), password)
            else -> onLogin(email.trim(), password)
        }
        password = ""
        confirmation = ""
    }
    Surface(color = colors.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.screen, vertical = AppSpace.group),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.item)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = androidx.compose.ui.graphics.Color.Transparent) {
                        androidx.compose.foundation.Image(painterResource(R.drawable.brand_logo), null, Modifier.size(54.dp))
                    }
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.account_brand_caption), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                if (onConfigureServer != null) {
                    TextButton(onClick = onConfigureServer, enabled = !busy, modifier = Modifier.testTag("configureServer")) {
                        Text(stringResource(R.string.server_change) + if (serverUrl.isNotEmpty()) " · $serverUrl" else "")
                    }
                }
                Spacer(Modifier.height(AppSpace.section))
                Text(
                    stringResource(if (firstRegistrationRequired) R.string.account_first_registration_title else when (mode) {
                        "register" -> R.string.account_create_title
                        "forgot" -> R.string.account_recover_title
                        "reset" -> R.string.account_reset
                        else -> R.string.account_welcome
                    }),
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(if (firstRegistrationRequired) R.string.account_first_registration_description else when (mode) {
                        "register" -> R.string.account_register_description
                        "forgot" -> R.string.account_recover_description
                        "reset" -> R.string.account_reset_description
                        else -> R.string.account_login_description
                    }),
                    style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(AppSpace.section))
                Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLow) {
                    Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.group)) {
                        if (!firstRegistrationRequired && (mode == "login" || mode == "register")) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                listOf("login" to R.string.account_login, "register" to R.string.account_register).forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = mode == value, onClick = { switchMode(value) }, enabled = !busy,
                                        modifier = Modifier.testTag("accountMode_$value"),
                                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                                        icon = {},
                                    ) { Text(stringResource(label), maxLines = 1) }
                                }
                            }
                        } else if (!firstRegistrationRequired) {
                            TextButton(onClick = { switchMode("login") }, enabled = !busy, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.account_back_to_login))
                            }
                        }
                        if ((mode == "login" || mode == "register") && onGoogle != null) {
                            OutlinedButton(onClick = onGoogle, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("accountGoogle")) {
                                Icon(painterResource(R.drawable.google_g), null, tint = androidx.compose.ui.graphics.Color.Unspecified)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.google_continue))
                            }
                        }
                        if (mode != "reset") {
                            OutlinedTextField(
                                email, { email = it }, label = { Text(stringResource(R.string.account_email)) },
                                leadingIcon = { Icon(Icons.Rounded.MailOutline, null) }, enabled = !busy, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (mode == "forgot") ImeAction.Done else ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }, onDone = { submit() }),
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (mode == "register") {
                            OutlinedTextField(
                                username, { username = it }, label = { Text(stringResource(R.string.account_username)) },
                                leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) }, enabled = !busy, singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (mode == "reset") {
                            OutlinedTextField(
                                token, { token = it }, label = { Text(stringResource(R.string.account_reset_code)) },
                                enabled = !busy, singleLine = true, shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (mode != "forgot") {
                            AccountPasswordField(password, { password = it }, R.string.account_password, busy, mode == "login", ::submit)
                            if (mode != "login") {
                                Text(stringResource(R.string.account_password_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                AccountPasswordField(confirmation, { confirmation = it }, R.string.account_password_confirm, busy, true, ::submit)
                            }
                        }
                        if (mode == "login") {
                            TextButton(onClick = { switchMode("forgot") }, enabled = !busy, modifier = Modifier.align(Alignment.End), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text(stringResource(R.string.account_forgot))
                            }
                        }
                        if (mode == "register" || mode == "reset") PasswordCriteria(password, confirmation)
                        message?.let {
                            val success = it == "RESET_SENT" || it == "PASSWORD_RESET" || it == "PASSWORD_CHANGED"
                            Surface(shape = RoundedCornerShape(12.dp), color = if (success) colors.secondaryContainer else colors.errorContainer) {
                                Text(stringResource(accountMessage(it)), Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                    style = MaterialTheme.typography.bodyMedium, color = if (success) colors.onSecondaryContainer else colors.onErrorContainer)
                            }
                        }
                        Button(
                            onClick = ::submit, enabled = !busy && valid,
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("accountSubmit"),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(stringResource(when (mode) {
                                "register" -> R.string.account_register
                                "forgot" -> R.string.account_send_code
                                "reset" -> R.string.account_save_password
                                else -> R.string.account_login
                            }), style = MaterialTheme.typography.labelLarge)
                        }
                        if (mode == "forgot") {
                            TextButton(onClick = { switchMode("reset") }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text(stringResource(R.string.account_have_code))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(AppSpace.section))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.group)) {
                    HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
                    Text(stringResource(R.string.account_or), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
                }
                TextButton(onClick = onLocal, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.account_continue_local), textAlign = TextAlign.Center)
                }
                Text(stringResource(R.string.account_local_description), style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AccountPasswordField(
    value: String, onChange: (String) -> Unit, label: Int, busy: Boolean, last: Boolean, onSubmit: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value, onChange, label = { Text(stringResource(label)) }, enabled = !busy, singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = !busy) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    stringResource(if (visible) R.string.account_hide_password else R.string.account_show_password))
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }, onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun AccountSettingsSection() {
    val vm = LocalAccountViewModel.current ?: return
    val state by vm.account.collectAsState()
    val busy by vm.busy.collectAsState()
    val google by vm.googleProviders.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(vm) { vm.loadGoogleConfiguration() }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.serverUrl.ifEmpty { stringResource(R.string.server_not_configured) })
            OutlinedButton(enabled = !busy, onClick = vm::openServerConfiguration) { Text(stringResource(R.string.server_configure)) }
            Text(stringResource(R.string.cloud_account), style = MaterialTheme.typography.titleMedium)
            Text(state.session?.account?.email ?: stringResource(R.string.account_local_mode))
            if (state.session != null) {
                CloudSyncSection(vm)
                if (google.signInEnabled && state.session?.account?.googleLinked != true) {
                    OutlinedButton(enabled = !busy, onClick = { vm.googleSignIn({ id, nonce -> requestGoogleCredential(context, id, nonce) }, link = true) }) {
                        Icon(painterResource(R.drawable.google_g), null, tint = androidx.compose.ui.graphics.Color.Unspecified)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.google_link))
                    }
                }
                Button(enabled = !busy, onClick = { coroutineScope.launch { clearGoogleCredential(context) }; vm.logout() }) { Text(stringResource(R.string.account_logout)) }
            } else {
                Button(enabled = !busy, onClick = vm::enterCloud) { Text(stringResource(R.string.account_login)) }
            }
        }
    }
}

internal fun accountMessage(code: String): Int = when (code) {
    "VERIFICATION_SENT" -> R.string.enrollment_sent
    "INVALID_VERIFICATION_TOKEN" -> R.string.enrollment_invalid_code
    "ACCOUNT_NOT_READY" -> R.string.enrollment_pending_help
    "PERMISSION_DENIED" -> R.string.account_permission_denied
    "GOOGLE_LINK_REQUIRED" -> R.string.google_link_required
    "GOOGLE_ACCOUNT_MISMATCH" -> R.string.google_account_mismatch
    "GOOGLE_NOT_CONFIGURED" -> R.string.google_not_configured
    "INVALID_GOOGLE_TOKEN", "INVALID_GOOGLE_STATE" -> R.string.google_sign_in_error
    "ACCOUNT_DISABLED" -> R.string.google_account_disabled
    "INVALID_CREDENTIALS" -> R.string.account_invalid_credentials
    "ACCOUNT_EXISTS", "DATA_CONFLICT" -> R.string.account_exists
    "INVALID_REQUEST" -> R.string.account_invalid_request
    "RATE_LIMITED" -> R.string.account_rate_limited
    "MAIL_UNAVAILABLE" -> R.string.account_mail_unavailable
    "INVALID_RESET_TOKEN" -> R.string.account_invalid_reset
    "RESET_SENT" -> R.string.account_reset_sent
    "PASSWORD_CHANGED", "PASSWORD_RESET" -> R.string.account_reset_done
    "INVALID_CURRENT_PASSWORD" -> R.string.password_current_invalid
    "PASSWORD_UNCHANGED" -> R.string.password_must_differ
    "NETWORK_ERROR" -> R.string.account_network_error
    else -> R.string.account_generic_error
}
