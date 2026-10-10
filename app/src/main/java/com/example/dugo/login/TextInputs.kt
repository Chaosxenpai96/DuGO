package com.example.dugo.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldBg = Color(0xFFFFFBFA)
private val FieldStroke = Color(0xFFEDE7E5)
private val Placeholder = Color(0xFFB4B8C4)
private val TextDark = Color(0xFF2B2D3A)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    focusedBorderColor = Brand,
    unfocusedBorderColor = FieldStroke,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark,
    cursorColor = Brand
)

/** Email or phone number field. */
@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        placeholder = { Text("Email or Phone Number", color = Placeholder, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = TextDark) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}

/** Password field with a Show/Hide toggle. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        placeholder = { Text("Password", color = Placeholder, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextDark) },
        trailingIcon = {
            Text(
                text = if (visible) "Hide" else "Show",
                color = Color(0xFF8A8FA0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable(onClick = onToggleVisible)
            )
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}