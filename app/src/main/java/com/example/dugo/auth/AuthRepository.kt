package com.example.dugo.auth

import android.content.Context
import android.util.Log
import com.example.dugo.home.UserProfile
import com.example.dugo.home.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    /**
     * Accounts register with a username, but Firebase Auth needs an email, so the
     * username is mapped onto this reserved domain. It never receives mail, which
     * means password-reset emails can't reach username accounts.
     */
    private const val USERNAME_EMAIL_DOMAIN = "users.dugo.example.com"

    /** Turns what the user typed (a username or a real email) into the Auth email. */
    fun loginEmail(input: String): String {
        val trimmed = input.trim()
        return if ("@" in trimmed) trimmed else "${trimmed.lowercase()}@$USERNAME_EMAIL_DOMAIN"
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult = try {
        val user = FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(loginEmail(email), password)
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

    /**
     * Creates the account for [profile]'s username and saves the whole profile to
     * Firestore. [extra] holds registration fields that [UserProfile] doesn't model.
     */
    suspend fun register(
        profile: UserProfile,
        password: String,
        extra: Map<String, Any> = emptyMap()
    ): AuthResult = try {
        val user = FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(loginEmail(profile.username), password)
            .await()
            .user
        if (user != null) {
            user.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(profile.fullName)
                    .build()
            ).await()
            UserSession.profile = profile
            syncProfileToFirestore(extra)
            AuthResult.Success(AuthProvider.EMAIL, user)
        } else {
            AuthResult.Failure("Could not create the account. Please try again.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Registration failed", e)
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

    // -----------------------------------------------------------------------
    // Facebook
    // -----------------------------------------------------------------------

    /** Starts the Facebook login flow. */
    fun startFacebookLogin(activity: android.app.Activity) {
        FacebookAuthCallback.startLogin(activity)
    }

    fun facebookAccessToken(): String? = FacebookAuthCallback.currentAccessToken()

    /**
     * Initialises the Facebook SDK when both the App ID and Client Token are
     * present. SDK 19 refuses to initialise without a client token, so a missing
     * value leaves the SDK off instead of throwing.
     */
    fun initializeFacebook(context: Context) {
        val appId = stringResource(context, "facebook_app_id")
        val clientToken = stringResource(context, "facebook_client_token")
        if (!isRealValue(appId) || !isRealValue(clientToken)) return

        runCatching {
            com.facebook.FacebookSdk.setApplicationId(appId)
            com.facebook.FacebookSdk.setClientToken(clientToken)
            com.facebook.FacebookSdk.sdkInitialize(context)
        }.onFailure { Log.w(TAG, "Facebook SDK could not initialise", it) }
    }

    private fun stringResource(context: Context, name: String): String {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        return if (id != 0) context.getString(id) else ""
    }

    private fun isRealValue(value: String): Boolean =
        value.isNotBlank() && !value.startsWith("REPLACE_WITH")

    /**
     * True when both Facebook credentials are filled in. A missing client token
     * means the SDK cannot initialise, so the login button should say so.
     */
    fun isFacebookConfigured(context: Context): Boolean =
        isRealValue(stringResource(context, "facebook_app_id")) &&
            isRealValue(stringResource(context, "facebook_client_token"))

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
            username = (user.email ?: user.displayName ?: user.uid)
                .takeIf { it.isNotBlank() } ?: existing.username,
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
     * Loads the signed-in user's saved profile into [UserSession]. A user with no
     * saved profile yet (a first social sign-in) gets one written instead.
     */
    suspend fun loadProfile() {
        val user = currentUser ?: return
        val doc = try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load the profile", e)
            return
        }
        if (!doc.exists()) {
            syncProfileToFirestore()
            return
        }
        val p = UserSession.profile
        UserSession.profile = p.copy(
            fullName = doc.getString("fullName") ?: p.fullName,
            username = doc.getString("username") ?: p.username,
            bloodType = doc.getString("bloodType") ?: p.bloodType,
            location = doc.getString("location") ?: p.location,
            contactNumber = doc.getString("contactNumber") ?: p.contactNumber,
            role = doc.getString("role") ?: p.role,
            availableToday = doc.getBoolean("availableToday") ?: p.availableToday,
            totalDonations = doc.getLong("totalDonations")?.toInt() ?: p.totalDonations,
            joinedDate = doc.getString("joinedDate") ?: p.joinedDate,
            latitude = doc.getDouble("latitude") ?: p.latitude,
            longitude = doc.getDouble("longitude") ?: p.longitude
        )
    }

    /**
     * Saves the device's GPS position to the profile, which places the donor's pin
     * on the map. Coordinates come straight from the device — no address lookup.
     */
    fun saveLocation(latitude: Double, longitude: Double) {
        UserSession.profile = UserSession.profile.copy(
            latitude = latitude,
            longitude = longitude
        )
        val user = currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(mapOf("latitude" to latitude, "longitude" to longitude), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Could not save the location", it) }
    }

    /**
     * Writes the profile to Firestore so the app has a server-side record. Merges,
     * so fields this doesn't know about (like [extra] from registration) survive.
     * Failure is not fatal — sign-in still succeeds without it.
     */
    fun syncProfileToFirestore(
        extra: Map<String, Any> = emptyMap(),
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = currentUser ?: return onComplete(false)
        val profile = UserSession.profile
        val data = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "fullName" to profile.fullName,
            "username" to profile.username,
            "bloodType" to profile.bloodType,
            "location" to profile.location,
            "contactNumber" to profile.contactNumber,
            "role" to profile.role,
            "availableToday" to profile.availableToday,
            "totalDonations" to profile.totalDonations,
            "joinedDate" to profile.joinedDate,
            "email" to (user.email ?: "")
        )
        profile.latitude?.let { data["latitude"] = it }
        profile.longitude?.let { data["longitude"] = it }
        data.putAll(extra)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnCompleteListener {
                if (!it.isSuccessful) Log.w(TAG, "Could not save the profile", it.exception)
                onComplete(it.isSuccessful)
            }
    }

    /** Turns Firebase exception text into something a user can act on. */
    private fun friendlyMessage(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("email address is already in use", ignoreCase = true) ->
                "That username is already taken. Pick another one or sign in instead."
            raw.contains("badly formatted", ignoreCase = true) ->
                "Enter a valid username or email."
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("wrong password", ignoreCase = true) ||
                raw.contains("supplied auth credential", ignoreCase = true) ->
                "Incorrect username/email or password."
            raw.contains("no user record", ignoreCase = true) ||
                raw.contains("user not found", ignoreCase = true) ->
                "No account found with that username or email."
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
