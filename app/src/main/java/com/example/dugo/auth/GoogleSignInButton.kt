package com.example.dugo.auth

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A "Continue with Google" button backed by Credential Manager.
 *
 * [onGoogleIdToken] receives the Google ID token, which should be handed to
 * [AuthRepository.signInWithGoogle]. [onError] is called with a user-facing
 * message when the flow cannot complete.
 */
@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onGoogleIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val serverClientId = remember { googleWebClientId(context) }
    val scope = rememberCoroutineScope()
    var isConnecting by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .setCredentialOptions(listOf(option))
                .build()

            isConnecting = true
            scope.launch {
                val signal = CancellationSignal()
                try {
                    // Credential Manager does not always call back (for example when
                    // Google Play services is missing an account or module), so the
                    // wait is bounded to avoid leaving the button stuck.
                    val response = withTimeout(20_000) {
                        suspendCancellableCoroutine<GetCredentialResponse> { continuation ->
                            credentialManager.getCredentialAsync(
                                context,
                                request,
                                signal,
                                ContextCompat.getMainExecutor(context),
                                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                                    override fun onResult(result: GetCredentialResponse) {
                                        if (continuation.isActive) continuation.resume(result)
                                    }

                                    override fun onError(e: GetCredentialException) {
                                        if (continuation.isActive) continuation.resumeWithException(e)
                                    }
                                }
                            )
                            continuation.invokeOnCancellation { signal.cancel() }
                        }
                    }

                    val token = runCatching {
                        GoogleIdTokenCredential.createFrom(response.credential.data).idToken
                    }.getOrNull()

                    if (token != null) {
                        onGoogleIdToken(token)
                    } else {
                        onError("Couldn't read the Google sign-in token. Please try again.")
                    }
                } catch (e: TimeoutCancellationException) {
                    onError(
                        "Google sign-in took too long. Make sure a Google account is " +
                            "added on this device, then try again."
                    )
                } catch (e: GetCredentialException) {
                    onError(googleErrorMessage(e))
                } finally {
                    isConnecting = false
                }
            }
        },
        enabled = enabled && !isConnecting,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFFD5D8DE)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF2B2D3A)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(
                if (isConnecting) "Connecting…" else "Continue with Google",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Turns a Credential Manager failure into something worth showing the user. */
private fun googleErrorMessage(e: Exception): String {
    val raw = e.message.orEmpty()
    return when {
        e is GetCredentialCancellationException -> "Google sign-in was cancelled."
        e is GetCredentialProviderConfigurationException ->
            "Google sign-in isn't set up yet. Check the Firebase configuration."
        raw.contains("no credentials available", ignoreCase = true) ->
            "No Google account found on this device. Add one in Settings, then try again."
        e is GetCredentialException -> raw.ifBlank { "Google sign-in failed. Please try again." }
        else -> "Google sign-in failed. Please try again."
    }
}

/**
 * Reads the OAuth web client id that the Google Services Gradle plugin generates
 * from `google-services.json`. Returns an empty string when it is not available,
 * which makes the sign-in attempt fail with a readable message rather than a crash.
 */
private fun googleWebClientId(context: Context): String = runCatching {
    context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        .takeIf { it != 0 }
        ?.let { context.getString(it) }
        .orEmpty()
}.getOrDefault("")
