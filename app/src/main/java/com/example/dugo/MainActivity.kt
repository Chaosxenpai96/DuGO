package com.example.dugo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dugo.login.LoginScreen
import com.example.dugo.ui.theme.DuGOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuGOTheme {
                LoginScreen(
                    onLogin = { user, _ ->
                        // TODO: call your login logic / API here
                        Toast.makeText(this, "Logging in as $user", Toast.LENGTH_SHORT).show()
                    },
                    onGoogle = { Toast.makeText(this, "Google sign-in", Toast.LENGTH_SHORT).show() },
                    onForgotPassword = { Toast.makeText(this, "Forgot password", Toast.LENGTH_SHORT).show() },
                    onSignUp = { Toast.makeText(this, "Sign up", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
}