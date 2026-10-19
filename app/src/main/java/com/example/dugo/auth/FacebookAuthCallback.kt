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
        LoginManager.getInstance()
            .logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    /** The current access token, if the user is still signed in to Facebook. */
    fun currentAccessToken(): String? = AccessToken.getCurrentAccessToken()?.token

    /** Forwards the Facebook login result to the SDK. Call from onActivityResult. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): Boolean =
        callbackManager?.onActivityResult(requestCode, resultCode, data) ?: false
}
