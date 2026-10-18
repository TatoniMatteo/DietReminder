package it.matato.dietreminder.data.auth

import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

suspend fun requestGoogleCredential(context: Context, clientId: String, nonce: String): String {
    val manager = CredentialManager.create(context)
    val option = GetSignInWithGoogleOption.Builder(clientId).setNonce(nonce).build()
    return try {
        val result = manager.getCredential(MutableContextWrapper(context), GetCredentialRequest.Builder().addCredentialOption(option).build())
        require(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    } catch (_: GetCredentialCancellationException) { throw CancellationException("Google sign-in cancelled") }
}
suspend fun clearGoogleCredential(context: Context) {
    try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    catch (ex: CancellationException) { throw ex }
    catch (_: Exception) { /* Backend logout remains available if a provider cannot clear its local state. */ }
}
