package com.example.dugo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.dugo.home.UserSession
import com.example.dugo.login.LoginScreen
import com.example.dugo.login.RegisterDonor
import com.example.dugo.login.RegisterSeeker
import com.example.dugo.login.RoleSelectScreen
import com.example.dugo.ui.theme.DuGOTheme
import kotlinx.coroutines.launch

enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker, Home, Requests, Messages, Profile }

// Testing shortcut: only works in debug builds. Remove before releasing the app.
private const val ADMIN_USER = "admin"
private const val ADMIN_PASSWORD = "admin123"

class MainActivity : ComponentActivity() {

    private val isDebugBuild: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * The Facebook SDK delivers its result through onActivityResult, which happens
     * outside the Compose scope, so the result is parked here and read by the
     * composition when it next runs.
     */
    private var pendingFacebookToken: String? = null
    private var pendingAuthError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The Facebook SDK initialises itself through a ContentProvider; this only
        // makes sure the App ID from the manifest is applied.
        AuthRepository.initializeFacebook(this)

        // Match the FCM topic subscriptions to the saved notification preferences
        com.example.dugo.auth.NotificationManager.syncTopics(this)

        FacebookAuthCallback.register(
            onToken = { token -> pendingFacebookToken = token },
            onFailure = { message -> pendingAuthError = message }
        )

        setContent {
            DuGOTheme {
                var screen by remember { mutableStateOf(Screen.Login) }
                var roleLabel by remember { mutableStateOf("Blood Donor") }
                var authError by remember { mutableStateOf<String?>(null) }
                var isAuthLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                fun onAuthSuccess() {
                    authError = null
                    isAuthLoading = false
                    roleLabel = UserSession.profile.role
                    screen = Screen.Home
                }

                // Pick up a Facebook token that arrived through onActivityResult
                val facebookToken = pendingFacebookToken
                if (facebookToken != null) {
                    pendingFacebookToken = null
                    isAuthLoading = true
                    scope.launch {
                        when (val result = AuthRepository.signInWithFacebook(facebookToken)) {
                            is AuthResult.Success -> {
                                AuthRepository.syncProfileToFirestore()
                                onAuthSuccess()
                            }
                            is AuthResult.Failure -> {
                                isAuthLoading = false
                                authError = result.message
                            }
                        }
                    }
                }

                pendingAuthError?.let { message ->
                    pendingAuthError = null
                    authError = message
                }

                // Back button: secondary screens go back to Home; register goes back to RoleSelect
                val secondaryScreens = setOf(Screen.Requests, Screen.Messages, Screen.Profile)
                BackHandler(enabled = screen != Screen.Login && screen != Screen.Home) {
                    screen = when (screen) {
                        Screen.RegisterDonor, Screen.RegisterSeeker -> Screen.RoleSelect
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
                                isAuthLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        AuthRepository.syncProfileToFirestore()
                                        onAuthSuccess()
                                    }
                                    is AuthResult.Failure -> authError = result.message
                                }
                            }
                        },
                        onGoogleIdToken = { idToken ->
                            isAuthLoading = true
                            authError = null
                            scope.launch {
                                when (val result = AuthRepository.signInWithGoogle(idToken)) {
                                    is AuthResult.Success -> {
                                        AuthRepository.syncProfileToFirestore()
                                        onAuthSuccess()
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
                        onSubmit = { form ->
                            // TODO: save the form to your backend / Firebase here
                            roleLabel = "Blood Donor"
                            screen = Screen.Home
                        }
                    )

                    Screen.RegisterSeeker -> RegisterSeeker(
                        onBack = { screen = Screen.RoleSelect },
                        onSubmit = { form ->
                            // TODO: save the form to your backend / Firebase here
                            roleLabel = "Blood Seeker"
                            screen = Screen.Home
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
                        onMessageDonor = { screen = Screen.Messages }
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
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> screen = Screen.Home
                                HomeTab.Requests -> screen = Screen.Requests
                                HomeTab.Messages -> { /* already here */ }
                                HomeTab.Profile -> screen = Screen.Profile
                            }
                        },
                        onNotifications = { screen = Screen.Profile },
                        onProfile = { screen = Screen.Profile }
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
