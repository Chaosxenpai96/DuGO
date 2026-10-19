package com.example.dugo.auth

import android.content.Context
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Handles the result of the Facebook Login SDK flow and hands the access token
 * to the caller so it can be exchanged for a Firebase credential.
 *
 * The Facebook SDK v19 initialises itself through a ContentProvider, so there is
 * no manual `sdkInitialize` call to make.
 */
object FacebookAuthCallback {

    private var onToken: ((String) -> Unit)? = null
    private var onFailure: ((String) -> Unit)? = null
    private var callbackManager: CallbackManager? = null

    /** Registers the callback with the Facebook SDK. Call once, from onCreate. */
    fun register(onToken: (String) -> Unit, onFailure: (String) -> Unit) {
        this.onToken = onToken
        this.onFailure = onFailure

        // The SDK is only initialised once both the App ID and client token are
        // configured. Touching LoginManager before that throws, so skip it.
        if (!FacebookSdk.isInitialized()) return

        val manager = callbackManager ?: CallbackManager.Factory.create().also {
            callbackManager = it
        }

        LoginManager.getInstance().registerCallback(
            manager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val token = result.accessToken?.token
                    if (token != null) {
                        this@FacebookAuthCallback.onToken?.invoke(token)
                    } else {
                        this@FacebookAuthCallback.onFailure?.invoke(
                            "Facebook sign-in failed. Please try again."
                        )
                    }
                }

                override fun onCancel() {
                    this@FacebookAuthCallback.onFailure?.invoke("Facebook sign-in was cancelled.")
                }

                override fun onError(error: FacebookException) {
                    this@FacebookAuthCallback.onFailure?.invoke(
                        error.message ?: "Facebook sign-in failed. Please try again."
                    )
                }
            }
        )
    }

    /** Starts the Facebook login flow. */
    fun startLogin(activity: android.app.Activity) {
        if (!FacebookSdk.isInitialized()) {
            onFailure?.invoke(
                "Facebook login isn't configured yet. Add your App ID and Client " +
                    "Token to res/values/facebook.xml."
            )
            return
        }
        // Only public_profile is requested: it is always granted. Asking for
        // "email" before the app has that permission approved makes Facebook
        // show an "Invalid Scopes" warning and returns no address anyway. Add
        // "email" back here once it is approved in the Meta dashboard.
        LoginManager.getInstance()
            .logInWithReadPermissions(activity, listOf("public_profile"))
    }

    /** The current access token, if the user is still signed in to Facebook. */
    fun currentAccessToken(): String? = runCatching {
        if (FacebookSdk.isInitialized()) AccessToken.getCurrentAccessToken()?.token else null
    }.getOrNull()

    /** Forwards the Facebook login result to the SDK. Call from onActivityResult. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): Boolean =
        callbackManager?.onActivityResult(requestCode, resultCode, data) ?: false
}
