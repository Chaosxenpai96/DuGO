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
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.example.dugo.home.HomeScreenMap
import com.example.dugo.home.HomeTab
import com.example.dugo.login.LoginScreen
import com.example.dugo.login.RegisterDonor
import com.example.dugo.login.RegisterSeeker
import com.example.dugo.login.RoleSelectScreen
import com.example.dugo.ui.theme.DuGOTheme

enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker, Home }

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

                // Back button: register -> role select -> login (Login and Home close the app)
                BackHandler(enabled = screen != Screen.Login && screen != Screen.Home) {
                    screen = when (screen) {
                        Screen.RegisterDonor, Screen.RegisterSeeker -> Screen.RoleSelect
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
                        onFacebook = { Toast.makeText(this, "Facebook sign-in", Toast.LENGTH_SHORT).show() },
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
                        onTabSelected = { tab ->
                            if (tab != HomeTab.Map) {
                                Toast.makeText(this, "${tab.label} coming soon", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNotifications = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                        onProfile = { Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show() },
                        onMessageDonor = { donor ->
                            // Opens the phone's messaging app with a ready-to-send text
                            val intent = Intent(Intent.ACTION_SENDTO, "smsto:${donor.phone}".toUri()).apply {
                                putExtra(
                                    "sms_body",
                                    "Hi ${donor.name}, I found you on DuGO. I'm looking for ${donor.bloodType} blood. Can you help?"
                                )
                            }
                            runCatching { startActivity(intent) }.onFailure {
                                Toast.makeText(this, "No messaging app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}