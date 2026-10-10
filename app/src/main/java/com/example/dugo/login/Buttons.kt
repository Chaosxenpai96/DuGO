package com.example.dugo.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Brand = Color(0xFFD90429)
private val TextDark = Color(0xFF2B2D3A)
private val TextGray = Color(0xFF8A8FA0)

@Composable
fun ForgotPasswordLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "Forgot Password?",
        modifier = modifier.clickable(onClick = onClick),
        color = Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold
    )
}

@Composable
fun LoginButton(onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFE8202F), Color(0xFFC4061F))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("Log In  →", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE6E1DF))
        Text("or", Modifier.padding(horizontal = 12.dp), color = TextGray, fontSize = 12.sp)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE6E1DF))
    }
}

@Composable
fun GoogleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFFF28B93)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = TextDark)
    ) {
        // Simple "G" badge so no image file is needed; swap in a real logo later
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF4285F4)),
            contentAlignment = Alignment.Center
        ) {
            Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text("Continue with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}