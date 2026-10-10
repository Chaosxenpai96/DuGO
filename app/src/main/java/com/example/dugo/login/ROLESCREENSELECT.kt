package com.example.dugo.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.R

private val HeadingBlue = Color(0xFF2B4C6F)
private val SubGray = Color(0xFF8A9BB5)
private val PageBg = Color(0xFFFEFCFB)

@Composable
fun RoleSelectScreen(
    onNeedBlood: () -> Unit = {},
    onWantDonate: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(PageBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Needs res/drawable/dugo_logo.png
            Image(
                painter = painterResource(R.drawable.dugo_logo),
                contentDescription = "DuGO",
                modifier = Modifier.height(64.dp)
            )
            Text(
                "Donate Today. Save Tomorrow.",
                fontSize = 12.sp,
                color = SubGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(36.dp))

            Text(
                "How would you like to\nuse the app?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HeadingBlue,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(32.dp))

            // "I Need Blood" card (filled red)
            RoleCard(
                title = "I Need Blood",
                description = "Find a nearby donor\nbased on your blood type.",
                filled = true,
                onClick = onNeedBlood
            )

            Spacer(Modifier.height(16.dp))

            // "I Want to Donate" card (light pink)
            RoleCard(
                title = "I Want to Donate",
                description = "Help others by becoming\na registered donor.",
                filled = false,
                onClick = onWantDonate
            )

            Spacer(Modifier.height(36.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Already have an account?", fontSize = 13.sp, color = SubGray)
                Text(
                    "Log in",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brand,
                    modifier = Modifier.clickable(onClick = onLogin)
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val cardModifier = if (filled) {
        Modifier
            .shadow(10.dp, shape, ambientColor = Brand, spotColor = Brand)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFF0F2D), Color(0xFFD90429))),
                shape
            )
    } else {
        Modifier
            .background(Color(0xFFFFF3F2), shape)
            .border(BorderStroke(1.dp, Color(0xFFF3CFCF)), shape)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardModifier)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filled) {
            PersonDropIcon(
                personColor = Color.White,
                dropColor = Color.White,
                holeColor = Color(0xFFE8102A),
                modifier = Modifier.size(64.dp)
            )
        } else {
            PersonDropIcon(
                personColor = Color(0xFFFF5A64),
                dropColor = Color(0xFFFF5A64),
                holeColor = Color(0xFFFFF3F2),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.width(24.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (filled) Color.White else HeadingBlue
            )
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = if (filled) Color.White.copy(alpha = 0.8f) else SubGray
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (filled) Color.White.copy(alpha = 0.8f) else SubGray
        )
    }
}

/** A person silhouette with a blood-drop badge, drawn in code. */
@Composable
private fun PersonDropIcon(
    personColor: Color,
    dropColor: Color,
    holeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Head
        drawCircle(personColor, radius = w * 0.17f, center = Offset(w * 0.36f, h * 0.2f))

        // Body
        drawRoundRect(
            color = personColor,
            topLeft = Offset(w * 0.02f, h * 0.45f),
            size = Size(w * 0.68f, h * 0.38f),
            cornerRadius = CornerRadius(w * 0.2f, w * 0.2f)
        )

        // Drop badge (bottom right)
        val cx = w * 0.74f
        val cy = h * 0.74f
        val r = w * 0.2f

        val drop = Path().apply {
            moveTo(cx, cy - r * 1.7f)
            cubicTo(cx + r * 0.2f, cy - r * 1.2f, cx + r, cy - r * 0.6f, cx + r, cy)
            cubicTo(cx + r, cy + r * 0.6f, cx + r * 0.55f, cy + r, cx, cy + r)
            cubicTo(cx - r * 0.55f, cy + r, cx - r, cy + r * 0.6f, cx - r, cy)
            cubicTo(cx - r, cy - r * 0.6f, cx - r * 0.2f, cy - r * 1.2f, cx, cy - r * 1.7f)
            close()
        }

        drawPath(drop, color = dropColor)

        // Hole in the middle of the drop
        drawCircle(holeColor, radius = r * 0.45f, center = Offset(cx, cy + r * 0.1f))
        drawCircle(dropColor, radius = r * 0.2f, center = Offset(cx, cy + r * 0.1f))
    }
}