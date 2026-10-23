package com.example.dugo.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FormBg = Color(0xFFFCFCFD)
private val FormBorder = Color(0xFFE1EAF2)
private val FormPlaceholder = Color(0xFF9AA5B1)
private val FormText = Color(0xFF111827)
private val ErrorRed = Color(0xFFD32F2F)

val BLOOD_TYPES = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

/** Accepts 09XXXXXXXXX or +639XXXXXXXXX (Philippine mobile numbers). */
fun isValidPhNumber(number: String): Boolean =
    Regex("^(09\\d{9}|\\+639\\d{9})$").matches(number)

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FormBg,
    unfocusedContainerColor = FormBg,
    errorContainerColor = FormBg,
    focusedBorderColor = Brand,
    unfocusedBorderColor = FormBorder,
    errorBorderColor = ErrorRed,
    focusedTextColor = FormText,
    unfocusedTextColor = FormText,
    cursorColor = Brand
)

@Composable
fun FormHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FormText)
        }
        Text(
            title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = FormText,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            subtitle,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color(0xFF374151),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 20.dp)
        )
    }
}

@Composable
fun FormLabel(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = FormText,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = FormPlaceholder, fontSize = 15.sp) },
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = Color(0xFF9DB3C8)) }
        } else null,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        colors = formFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

/** 4-20 characters: letters, numbers and underscores only. */
fun isValidUsername(username: String): Boolean =
    Regex("^[A-Za-z0-9_]{4,20}$").matches(username)

@Composable
fun FormPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = FormPlaceholder, fontSize = 15.sp) },
        trailingIcon = {
            Text(
                text = if (visible) "Hide" else "Show",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A8FA0),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { visible = !visible }
            )
        },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(14.dp),
        colors = formFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

@Composable
fun BloodTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Column(Modifier.fillMaxWidth()) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(FormBg)
                    .border(1.dp, if (error != null) ErrorRed else FormBorder, shape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.ifEmpty { "Select blood type" },
                    fontSize = 16.sp,
                    fontWeight = if (selected.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                    color = if (selected.isEmpty()) FormPlaceholder else FormText
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF7C93A8))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BLOOD_TYPES.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (error != null) {
            Text(
                error,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/** A problem reported after submitting, such as the username already being taken. */
@Composable
fun FormError(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = ErrorRed,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun FormButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}