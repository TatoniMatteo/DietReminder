package it.matato.dietreminder.permission

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.AppLog

@Composable
fun rememberPermissionRequester(
    context: Context,
    onCompleted: () -> Unit = {},
): () -> Unit {
    var pendingRequests by remember {
        mutableStateOf(emptyList<PermissionRequest>())
    }

    var currentRequest by remember {
        mutableStateOf<PermissionRequest?>(null)
    }

    var active by remember {
        mutableStateOf(false)
    }

    val currentOnCompleted by rememberUpdatedState(onCompleted)

    fun advance() {
        currentRequest = pendingRequests.firstOrNull()
        pendingRequests = pendingRequests.drop(1)
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        AppLog.d("Runtime permission result: granted=$granted")
        advance()
    }

    val specialPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        AppLog.d("Returned from special permission settings")
        advance()
    }

    LaunchedEffect(currentRequest) {
        when (val request = currentRequest) {
            null -> {
                if (active) {
                    active = false
                    currentOnCompleted()
                }
            }

            is PermissionRequest.Runtime -> {
                AppLog.d("Requesting runtime permission: ${request.permission}")
                runtimePermissionLauncher.launch(request.permission)
            }

            is PermissionRequest.Special -> {
                val intent = PermissionManager.createSpecialPermissionIntent(
                    context,
                    request.permission,
                )

                if (intent != null) {
                    AppLog.d(
                        "Opening special permission settings: ${request.permission}",
                    )
                    specialPermissionLauncher.launch(intent)
                } else {
                    AppLog.w(
                        "No settings intent available for: ${request.permission}",
                    )
                    advance()
                }
            }
        }
    }

    return {
        if (active) {
            AppLog.d("Permission request already in progress")
        } else {
            val requests = buildList {
                PermissionManager.getMissingRuntimePermissions(context)
                    .forEach { add(PermissionRequest.Runtime(it)) }

                PermissionManager.getMissingSpecialPermissions(context)
                    .forEach { add(PermissionRequest.Special(it)) }
            }

            AppLog.d("Starting permission check: ${requests.size} permission(s)")

            if (requests.isEmpty()) {
                currentOnCompleted()
            } else {
                active = true
                pendingRequests = requests.drop(1)
                currentRequest = requests.first()
            }
        }
    }
}

private sealed interface PermissionRequest {

    data class Runtime(
        val permission: String,
    ) : PermissionRequest

    data class Special(
        val permission: PermissionManager.SpecialPermission,
    ) : PermissionRequest
}

@Preview(showBackground = true)
@Composable
private fun PermissionRequesterPreview() {
    DietTheme {
        val context = LocalContext.current
        val requestPermissions = rememberPermissionRequester(context)

        Button(
            onClick = requestPermissions,
        ) {
            Text("Check permissions")
        }
    }
}