package com.example.bloodbank.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** Password field with show/hide toggle. */
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
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = TextDark
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}
