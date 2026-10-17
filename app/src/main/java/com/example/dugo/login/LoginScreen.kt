package com.example.dugo.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.R

private val TextDark = Color(0xFF2B2D3A)
private val TextGray = Color(0xFF8A8FA0)

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> },
    onGoogle: () -> Unit = {},
    onFacebook: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onSignUp: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFFFDF8F7))) {
        Decorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            // Needs app/src/main/res/drawable/dugo_logo.png (lowercase name)
            Image(
                painter = painterResource(R.drawable.dugo_logo),
                contentDescription = "DuGO",
                modifier = Modifier.height(64.dp)
            )
            Spacer(Modifier.height(24.dp))

            Text("Donate Today.", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("Save Tomorrow.", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Brand)
            Text(
                "Connect with blood donors and help save lives in your community.",
                fontSize = 13.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 56.dp)
            )

            EmailField(email, { email = it })
            Spacer(Modifier.height(16.dp))
            PasswordField(password, { password = it }, showPassword, { showPassword = !showPassword })

            ForgotPasswordLink(
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.End).padding(top = 10.dp, bottom = 22.dp)
            )

            LoginButton(
                onClick = { onLogin(email.trim(), password) },
                enabled = email.isNotBlank() && password.isNotEmpty()
            )

            Spacer(Modifier.height(20.dp))
            OrDivider()
            Spacer(Modifier.height(20.dp))

            GoogleButton(onGoogle)
            Spacer(Modifier.height(12.dp))
            FacebookButton(onFacebook)

            Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Don't have an account?", fontSize = 12.sp, color = TextGray)
                Text(
                    "Sign Up",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brand,
                    modifier = Modifier.clickable(onClick = onSignUp)
                )
            }

            Spacer(Modifier.height(150.dp)) // keeps content above the wave
        }
    }
}

/** Pink blobs at the top and red wave at the bottom. */
@Composable
private fun Decorations() {
    Canvas(Modifier.fillMaxSize()) {
        val blob = Color(0xFFFFE6E6)
        drawCircle(blob, radius = 110.dp.toPx(), center = Offset(0f, 0f))
        drawCircle(blob, radius = 45.dp.toPx(), center = Offset(size.width, 85.dp.toPx()))

        val w = size.width
        val h = 130.dp.toPx()
        val top = size.height - h

        val wave1 = Path().apply {
            moveTo(0f, top + h * 0.38f)
            cubicTo(w * 0.3f, top, w * 0.6f, top + h * 0.7f, w, top + h * 0.15f)
            lineTo(w, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wave1, Color(0xFFE3202F))

        val wave2 = Path().apply {
            moveTo(0f, top + h * 0.7f)
            cubicTo(w * 0.35f, top + h * 0.3f, w * 0.65f, top + h * 0.85f, w, top + h * 0.46f)
            lineTo(w, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wave2, Color(0xD9B0061F))
    }
}