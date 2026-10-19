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
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
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

            credentialManager.getCredentialAsync(
                context,
                request,
                CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        val token = runCatching {
                            GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                        }.getOrNull()

                        if (token != null) {
                            onGoogleIdToken(token)
                        } else {
                            onError("Couldn't read the Google sign-in token. Please try again.")
                        }
                    }

                    override fun onError(e: GetCredentialException) {
                        onError(googleErrorMessage(e))
                    }
                }
            )
        },
        enabled = enabled,
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
            Text("Continue with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Turns a Credential Manager failure into something worth showing the user. */
private fun googleErrorMessage(e: Exception): String = when (e) {
    is GetCredentialCancellationException -> "Google sign-in was cancelled."
    is GetCredentialProviderConfigurationException ->
        "Google sign-in isn't set up yet. Check the Firebase configuration."
    is GetCredentialException -> e.message ?: "Google sign-in failed. Please try again."
    else -> "Google sign-in failed. Please try again."
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
