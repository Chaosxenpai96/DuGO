package com.example.bloodbank.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodbank.R

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
        // add res/drawable/google_logo.png
        Image(painterResource(R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Continue with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
