package com.example.dugo

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
import androidx.compose.runtime.setValue
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

enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker, Home, Requests, Messages, Profile }

// Testing shortcut: only works in debug builds. Remove before releasing the app.
private const val ADMIN_USER = "admin"
private const val ADMIN_PASSWORD = "admin123"

class MainActivity : ComponentActivity() {

    private val isDebugBuild: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuGOTheme {
                var screen by remember { mutableStateOf(Screen.Login) }
                var roleLabel by remember { mutableStateOf("Blood Donor") }

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
                        onLogin = { user, password ->
                            if (isDebugBuild && user == ADMIN_USER && password == ADMIN_PASSWORD) {
                                roleLabel = "Admin"
                                screen = Screen.Home
                            } else {
                                // TODO: check the credentials with your backend
                                Toast.makeText(
                                    this,
                                    "Login isn't connected yet. For testing use admin / admin123",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onGoogle = { Toast.makeText(this, "Google sign-in", Toast.LENGTH_SHORT).show() },
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
                            screen = Screen.Login
                            roleLabel = "Blood Donor"
                        }
                    )
                }
            }
        }
    }
}