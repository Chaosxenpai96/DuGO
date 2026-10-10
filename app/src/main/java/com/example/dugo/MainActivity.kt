package com.example.dugo

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
import com.example.dugo.login.LoginScreen
import com.example.dugo.login.RegisterDonor
import com.example.dugo.login.RegisterSeeker
import com.example.dugo.login.RoleSelectScreen
import com.example.dugo.ui.theme.DuGOTheme

enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuGOTheme {
                var screen by remember { mutableStateOf(Screen.Login) }

                // Phone's back button: register -> role select -> login
                BackHandler(enabled = screen != Screen.Login) {
                    screen = when (screen) {
                        Screen.RegisterDonor, Screen.RegisterSeeker -> Screen.RoleSelect
                        else -> Screen.Login
                    }
                }

                when (screen) {
                    Screen.Login -> LoginScreen(
                        onLogin = { user, _ ->
                            Toast.makeText(this, "Logging in as $user", Toast.LENGTH_SHORT).show()
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
                            // TODO: save form to your backend / Firebase here
                            Toast.makeText(
                                this,
                                "Donor account created for ${form.fullName} (${form.bloodType})",
                                Toast.LENGTH_LONG
                            ).show()
                            screen = Screen.Login
                        }
                    )

                    Screen.RegisterSeeker -> RegisterSeeker(
                        onBack = { screen = Screen.RoleSelect },
                        onSubmit = { form ->
                            // TODO: save form to your backend / Firebase here
                            Toast.makeText(
                                this,
                                "Seeker account created for ${form.fullName} (needs ${form.bloodTypeNeeded})",
                                Toast.LENGTH_LONG
                            ).show()
                            screen = Screen.Login
                        }
                    )
                }
            }
        }
    }
}