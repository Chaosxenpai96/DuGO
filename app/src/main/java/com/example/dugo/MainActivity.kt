package com.example.dugo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.dugo.auth.AuthRepository
import com.example.dugo.auth.AuthResult
import com.example.dugo.auth.FacebookAuthCallback
import com.example.dugo.home.HomeScreenMap
import com.example.dugo.home.HomeTab
import com.example.dugo.home.MessagesScreen
import com.example.dugo.home.ProfileScreen
import com.example.dugo.home.RequestsScreen
import com.example.dugo.home.UserProfile
import com.example.dugo.home.UserSession
import com.example.dugo.login.CompleteProfileScreen
import com.example.dugo.login.LoginScreen
import com.example.dugo.login.RegisterDonor
import com.example.dugo.login.RegisterSeeker
import com.example.dugo.login.RoleSelectScreen
import com.example.dugo.ui.theme.DuGOTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker, CompleteProfile, Home, Requests, Messages, Profile }

/** "October 2026" — the month a new account joined, as shown on the profile. */
private fun joinedDate(): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

// Testing shortcut: only works in debug builds. Remove before releasing the app.
private const val ADMIN_USER = "admin"
private const val ADMIN_PASSWORD = "admin123"

class MainActivity : ComponentActivity() {

    private val isDebugBuild: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * The Facebook SDK delivers its result through onActivityResult, which happens
     * outside the Compose scope, so the result is parked in observable state and
     * picked up by the composition on its next run.
     */
    private val pendingFacebookToken = mutableStateOf<String?>(null)
    private val pendingAuthError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The Facebook SDK initialises itself through a ContentProvider; this only
        // makes sure the App ID from the manifest is applied.
        AuthRepository.initializeFacebook(this)

        // Match the FCM topic subscriptions to the saved notification preferences
        com.example.dugo.auth.NotificationManager.syncTopics(this)

        FacebookAuthCallback.register(
            onToken = { token -> pendingFacebookToken.value = token },
            onFailure = { message -> pendingAuthError.value = message }
        )

        setContent {
            DuGOTheme {
                var screen by remember { mutableStateOf(Screen.Login) }
                var roleLabel by remember { mutableStateOf("Blood Donor") }
                var authError by remember { mutableStateOf<String?>(null) }
                var isAuthLoading by remember { mutableStateOf(false) }
                // The donor a chat was opened with, set when messaging from the map
                var messageWith by remember { mutableStateOf<com.example.dugo.home.Donor?>(null) }
                val scope = rememberCoroutineScope()

                fun onAuthSuccess() {
                    authError = null
                    isAuthLoading = false
                    roleLabel = UserSession.profile.role
                    screen = Screen.Home
                }

                fun register(profile: UserProfile, password: String, extra: Map<String, Any>) {
                    isAuthLoading = true
                    authError = null
                    scope.launch {
                        when (val result = AuthRepository.register(profile, password, extra)) {
                            is AuthResult.Success -> onAuthSuccess()
                            is AuthResult.Failure -> {
                                isAuthLoading = false
                                authError = result.message
                            }
                        }
                    }
                }

                /**
                 * Finishes a sign-in that has no profile yet. A saved profile goes
                 * straight to the app; a first Google or Facebook sign-in is asked
                 * whether they're a donor or a seeker.
                 */
                suspend fun afterSignIn() {
                    val hasProfile = AuthRepository.loadProfile()
                    isAuthLoading = false
                    if (hasProfile) {
                        onAuthSuccess()
                    } else {
                        screen = Screen.CompleteProfile
                    }
                }

                // An error belongs to the screen that showed it
                LaunchedEffect(screen) { authError = null }

                // Pick up a Facebook token that arrived through onActivityResult
                val facebookToken = pendingFacebookToken.value
                if (facebookToken != null) {
                    pendingFacebookToken.value = null
                    isAuthLoading = true
                    scope.launch {
                        when (val result = AuthRepository.signInWithFacebook(facebookToken)) {
                            is AuthResult.Success -> {
                                afterSignIn()
                            }
                            is AuthResult.Failure -> {
                                isAuthLoading = false
                                authError = result.message
                            }
                        }
                    }
                }

                pendingAuthError.value?.let { message ->
                    pendingAuthError.value = null
                    authError = message
                }

                // Back button: secondary screens go back to Home; register goes back to RoleSelect
                val secondaryScreens = setOf(Screen.Requests, Screen.Messages, Screen.Profile)
                BackHandler(enabled = screen != Screen.Login && screen != Screen.Home) {
                    screen = when (screen) {
                        Screen.RegisterDonor, Screen.RegisterSeeker -> Screen.RoleSelect
                        // Signing out is the way out of the role question
                        Screen.CompleteProfile -> {
                            AuthRepository.signOut()
                            Screen.Login
                        }
                        in secondaryScreens -> Screen.Home
                        else -> Screen.Login
                    }
                }

                when (screen) {
                    Screen.Login -> LoginScreen(
                        authError = authError,
                        isLoading = isAuthLoading,
                        onLogin = { user, password ->
                            isAuthLoading = true
                            authError = null
                            scope.launch {
                                val result = AuthRepository.signInWithEmail(user, password)
                                when (result) {
                                    is AuthResult.Success -> {
                                        afterSignIn()
                                    }
                                    is AuthResult.Failure -> {
                                        isAuthLoading = false
                                        authError = result.message
                                    }
                                }
                            }
                        },
                        onGoogleIdToken = { idToken ->
                            isAuthLoading = true
                            authError = null
                            scope.launch {
                                when (val result = AuthRepository.signInWithGoogle(idToken)) {
                                    is AuthResult.Success -> {
                                        afterSignIn()
                                    }
                                    is AuthResult.Failure -> {
                                        isAuthLoading = false
                                        authError = result.message
                                    }
                                }
                            }
                        },
                        onGoogleError = { message -> authError = message },
                        onGoogle = { Toast.makeText(this, "Google sign-in", Toast.LENGTH_SHORT).show() },
                        onFacebook = {
                            authError = null
                            if (AuthRepository.isFacebookConfigured(this)) {
                                AuthRepository.startFacebookLogin(this)
                            } else {
                                authError =
                                    "Facebook login isn't configured yet. Add your App ID " +
                                        "and Client Token to res/values/facebook.xml."
                            }
                        },
                        onForgotPassword = { Toast.makeText(this, "Forgot password", Toast.LENGTH_SHORT).show() },
                        onSignUp = { screen = Screen.RoleSelect }
                    )

                    Screen.RoleSelect -> RoleSelectScreen(
                        onNeedBlood = { screen = Screen.RegisterSeeker },
                        onWantDonate = { screen = Screen.RegisterDonor },
                        onLogin = { screen = Screen.Login }
                    )

                    Screen.RegisterDonor -> RegisterDonor(
                        onBack = { screen = Screen.RoleSelect },
                        errorMessage = authError,
                        isSubmitting = isAuthLoading,
                        onSubmit = { form ->
                            register(
                                UserProfile(
                                    fullName = form.fullName,
                                    username = form.username,
                                    bloodType = form.bloodType,
                                    location = form.location,
                                    contactNumber = form.contactNumber,
                                    role = "Blood Donor",
                                    availableToday = form.availableAnytime,
                                    totalDonations = 0,
                                    joinedDate = joinedDate(),
                                    latitude = form.latitude,
                                    longitude = form.longitude
                                ),
                                form.password,
                                mapOf(
                                    "availableAnytime" to form.availableAnytime,
                                    "specificDates" to form.specificDates
                                )
                            )
                        }
                    )

                    Screen.RegisterSeeker -> RegisterSeeker(
                        onBack = { screen = Screen.RoleSelect },
                        errorMessage = authError,
                        isSubmitting = isAuthLoading,
                        onSubmit = { form ->
                            register(
                                UserProfile(
                                    fullName = form.fullName,
                                    username = form.username,
                                    bloodType = form.bloodTypeNeeded,
                                    location = form.location,
                                    contactNumber = form.contactNumber,
                                    role = "Blood Seeker",
                                    availableToday = false,
                                    totalDonations = 0,
                                    joinedDate = joinedDate(),
                                    latitude = form.latitude,
                                    longitude = form.longitude
                                ),
                                form.password,
                                mapOf("notes" to form.notes)
                            )
                        }
                    )

                    Screen.CompleteProfile -> CompleteProfileScreen(
                        suggestedName = UserSession.profile.fullName,
                        errorMessage = authError,
                        isSubmitting = isAuthLoading,
                        onSubmit = { form ->
                            isAuthLoading = true
                            authError = null
                            scope.launch {
                                val current = UserSession.profile
                                AuthRepository.completeProfile(
                                    current.copy(
                                        role = form.role,
                                        bloodType = form.bloodType,
                                        location = form.location,
                                        contactNumber = form.contactNumber,
                                        availableToday = form.role == "Blood Donor",
                                        totalDonations = 0,
                                        joinedDate = joinedDate(),
                                        latitude = form.latitude,
                                        longitude = form.longitude
                                    )
                                )
                                isAuthLoading = false
                                onAuthSuccess()
                            }
                        }
                    )

                    Screen.Home -> HomeScreenMap(
                        roleLabel = roleLabel,
                        selectedTab = HomeTab.Map,
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> { /* already here */ }
                                HomeTab.Requests -> screen = Screen.Requests
                                HomeTab.Messages -> screen = Screen.Messages
                                HomeTab.Profile -> screen = Screen.Profile
                            }
                        },
                        onNotifications = { screen = Screen.Profile },
                        onProfile = { screen = Screen.Profile },
                        onMessageDonor = { donor ->
                            messageWith = donor
                            screen = Screen.Messages
                        }
                    )

                    Screen.Requests -> RequestsScreen(
                        roleLabel = roleLabel,
                        selectedTab = HomeTab.Requests,
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> screen = Screen.Home
                                HomeTab.Requests -> { /* already here */ }
                                HomeTab.Messages -> screen = Screen.Messages
                                HomeTab.Profile -> screen = Screen.Profile
                            }
                        },
                        onNotifications = { screen = Screen.Profile },
                        onProfile = { screen = Screen.Profile }
                    )

                    Screen.Messages -> MessagesScreen(
                        selectedTab = HomeTab.Messages,
                        startWith = messageWith,
                        onStarted = { messageWith = null },
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> { messageWith = null; screen = Screen.Home }
                                HomeTab.Requests -> { messageWith = null; screen = Screen.Requests }
                                HomeTab.Messages -> { /* already here */ }
                                HomeTab.Profile -> { messageWith = null; screen = Screen.Profile }
                            }
                        },
                        onNotifications = { messageWith = null; screen = Screen.Profile },
                        onProfile = { messageWith = null; screen = Screen.Profile }
                    )

                    Screen.Profile -> ProfileScreen(
                        roleLabel = roleLabel,
                        selectedTab = HomeTab.Profile,
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> screen = Screen.Home
                                HomeTab.Requests -> screen = Screen.Requests
                                HomeTab.Messages -> screen = Screen.Messages
                                HomeTab.Profile -> { /* already here */ }
                            }
                        },
                        onNotifications = { /* already on profile */ },
                        onLogout = {
                            AuthRepository.signOut()
                            screen = Screen.Login
                            roleLabel = "Blood Donor"
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        FacebookAuthCallback.onActivityResult(requestCode, resultCode, data)
    }
}
