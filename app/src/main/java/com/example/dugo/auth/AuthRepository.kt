package com.example.dugo.auth

import android.content.Context
import android.util.Log
import com.example.dugo.home.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Where a sign-in came from — used for provider-specific handling and UI messaging. */
enum class AuthProvider { EMAIL, GOOGLE, FACEBOOK }

/** Result of a sign-in attempt. */
sealed class AuthResult {
    data class Success(val provider: AuthProvider, val user: FirebaseUser) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Wraps Firebase Authentication. Keeps the current user's profile in [UserSession]
 * so the rest of the app can read it without knowing about Firebase.
 */
object AuthRepository {

    private const val TAG = "AuthRepository"

    /** The signed-in user, or null when signed out. */
    val currentUser: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser

    val isSignedIn: Boolean
        get() = currentUser != null

    // -----------------------------------------------------------------------
    // Email / password
    // -----------------------------------------------------------------------

    suspend fun signInWithEmail(email: String, password: String): AuthResult = try {
        val user = FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.trim(), password)
            .await()
            .user
        if (user != null) {
            applyUserToSession(user)
            AuthResult.Success(AuthProvider.EMAIL, user)
        } else {
            AuthResult.Failure("Sign-in failed. Please try again.")
        }
    } catch (e: Exception) {
        AuthResult.Failure(friendlyMessage(e))
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): AuthResult = try {
        val user = FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email.trim(), password)
            .await()
            .user
        if (user != null) {
            user.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()
            ).await()
            applyUserToSession(user, fullName = fullName, role = role)
            AuthResult.Success(AuthProvider.EMAIL, user)
        } else {
            AuthResult.Failure("Could not create the account. Please try again.")
        }
    } catch (e: Exception) {
        AuthResult.Failure(friendlyMessage(e))
    }

    // -----------------------------------------------------------------------
    // Google
    // -----------------------------------------------------------------------

    /** Signs in with the Google ID token produced by Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): AuthResult = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .await()
            .user
        if (user != null) {
            applyUserToSession(user)
            AuthResult.Success(AuthProvider.GOOGLE, user)
        } else {
            AuthResult.Failure("Google sign-in failed. Please try again.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Google sign-in failed", e)
        AuthResult.Failure(friendlyMessage(e))
    }

    // -----------------------------------------------------------------------
    // Facebook
    // -----------------------------------------------------------------------

    /**
     * Signs in with a Facebook access token. The token is expected to already be
     * requested by the Facebook Login SDK and delivered through the activity result.
     */
    suspend fun signInWithFacebook(accessToken: String): AuthResult = try {
        val credential = com.google.firebase.auth.FacebookAuthProvider
            .getCredential(accessToken)
        val user = FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .await()
            .user
        if (user != null) {
            applyUserToSession(user)
            AuthResult.Success(AuthProvider.FACEBOOK, user)
        } else {
            AuthResult.Failure("Facebook sign-in failed. Please try again.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Facebook sign-in failed", e)
        AuthResult.Failure(friendlyMessage(e))
    }

    /**
     * True when a real App ID has been put in `res/values/facebook.xml`. The
     * bundled placeholder would otherwise send the login flow to a non-existent
     * Facebook app with no explanation.
     */
    fun isFacebookConfigured(context: Context): Boolean {
        val id = context.resources.getIdentifier(
            "facebook_app_id", "string", context.packageName
        )
        if (id == 0) return false
        val value = context.getString(id)
        return value.isNotBlank() && !value.startsWith("REPLACE_WITH")
    }

    /** Starts the Facebook login flow. */
    fun startFacebookLogin(activity: android.app.Activity) {
        FacebookAuthCallback.startLogin(activity)
    }

    fun facebookAccessToken(): String? = FacebookAuthCallback.currentAccessToken()

    /**
     * The Facebook SDK initialises itself through a ContentProvider, so this only
     * needs to confirm the App ID is present in the manifest resources.
     */
    fun initializeFacebook(context: Context) {
        val appId = context.resources.getIdentifier(
            "facebook_app_id", "string", context.packageName
        )
        if (appId != 0) {
            com.facebook.FacebookSdk.setApplicationId(context.getString(appId))
        }
    }

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------

    /** Copies the Firebase user into [UserSession] so the UI can read it directly. */
    fun applyUserToSession(
        user: FirebaseUser,
        fullName: String? = null,
        role: String? = null
    ) {
        val existing = UserSession.profile
        UserSession.profile = existing.copy(
            fullName = fullName
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: existing.fullName,
            username = (user.email ?: user.uid).takeIf { it.isNotBlank() } ?: existing.username,
            bloodType = existing.bloodType,
            location = existing.location,
            contactNumber = user.phoneNumber ?: existing.contactNumber,
            role = role ?: existing.role
        )
    }

    /** Signs out of both Firebase and Facebook. */
    fun signOut(onComplete: () -> Unit = {}) {
        FirebaseAuth.getInstance().signOut()
        runCatching { com.facebook.login.LoginManager.getInstance().logOut() }
        onComplete()
    }

    /**
     * Writes the profile to Firestore so the app has a server-side record.
     * Failure is not fatal — sign-in still succeeds without it.
     */
    fun syncProfileToFirestore(onComplete: (Boolean) -> Unit = {}) {
        val user = currentUser ?: return onComplete(false)
        val profile = UserSession.profile
        val data = mapOf(
            "uid" to user.uid,
            "fullName" to profile.fullName,
            "username" to profile.username,
            "bloodType" to profile.bloodType,
            "location" to profile.location,
            "contactNumber" to profile.contactNumber,
            "role" to profile.role,
            "email" to (user.email ?: "")
        )
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(data)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    /** Turns Firebase exception text into something a user can act on. */
    private fun friendlyMessage(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("email address is already in use", ignoreCase = true) ->
                "That email is already registered. Try signing in instead."
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("wrong password", ignoreCase = true) ||
                raw.contains("supplied auth credential", ignoreCase = true) ->
                "Incorrect email or password."
            raw.contains("no user record", ignoreCase = true) ||
                raw.contains("user not found", ignoreCase = true) ->
                "No account found for that email."
            raw.contains("weak password", ignoreCase = true) ->
                "Password is too weak. Use at least 6 characters."
            raw.contains("network", ignoreCase = true) ->
                "No internet connection. Check your network and try again."
            raw.contains("blocked", ignoreCase = true) ->
                "This sign-in method is disabled for now."
            raw.contains("api key", ignoreCase = true) ||
                raw.contains("ApiException", ignoreCase = true) ->
                "Sign-in is not configured yet. Check the Firebase setup."
            raw.isNotBlank() -> raw
            else -> "Something went wrong. Please try again."
        }
    }
}
