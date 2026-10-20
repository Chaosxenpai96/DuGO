package com.example.dugo.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class SeekerForm(
    val fullName: String,
    val bloodTypeNeeded: String,
    val location: String,
    val contactNumber: String,
    val notes: String,
    val username: String,
    val password: String
)

@Composable
fun RegisterSeeker(
    onBack: () -> Unit = {},
    onSubmit: (SeekerForm) -> Unit = {},
    errorMessage: String? = null,
    isSubmitting: Boolean = false
) {
    var fullName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val nameOk = fullName.isNotBlank()
    val bloodOk = bloodType.isNotEmpty()
    val locationOk = location.isNotBlank()
    val contactOk = isValidPhNumber(contact)
    val usernameOk = isValidUsername(username)
    val passwordOk = password.length >= 8
    val confirmOk = confirmPassword.isNotEmpty() && confirmPassword == password
    val allValid = nameOk && bloodOk && locationOk && contactOk &&
            usernameOk && passwordOk && confirmOk

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            FormHeader(
                title = "Register as a Blood Seeker",
                subtitle = "Tell us what you need so we can help you find a compatible donor.",
                onBack = onBack
            )

            FormLabel("Full Name")
            FormTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Maria Santos",
                error = if (submitted && !nameOk) "Please enter your full name" else null
            )

            FormLabel("Blood Type Needed")
            BloodTypeDropdown(
                selected = bloodType,
                onSelected = { bloodType = it },
                error = if (submitted && !bloodOk) "Please select the blood type needed" else null
            )

            FormLabel("Location")
            FormTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = "Cebu City, Cebu",
                leadingIcon = Icons.Outlined.LocationOn,
                error = if (submitted && !locationOk) "Please enter your location or hospital" else null
            )

            FormLabel("Contact Number")
            FormTextField(
                value = contact,
                onValueChange = { input -> contact = input.filter { it.isDigit() || it == '+' }.take(13) },
                placeholder = "09XX XXX XXXX",
                keyboardType = KeyboardType.Phone,
                error = if (submitted && !contactOk) "Enter a valid number (09XXXXXXXXX or +639XXXXXXXXX)" else null
            )

            FormLabel("Additional Notes (optional)")
            FormTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "e.g. For surgery, accident, etc.",
                singleLine = false,
                minLines = 3
            )

            FormLabel("Username")
            FormTextField(
                value = username,
                onValueChange = { input -> username = input.filter { it.isLetterOrDigit() || it == '_' }.take(20) },
                placeholder = "e.g. maria_santos",
                error = if (submitted && !usernameOk) "Use 4-20 letters, numbers or underscores" else null
            )

            FormLabel("Password")
            FormPasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = "At least 8 characters",
                error = if (submitted && !passwordOk) "Password must be at least 8 characters" else null
            )

            FormLabel("Confirm Password")
            FormPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Re-enter your password",
                error = if (submitted && !confirmOk) "Passwords do not match" else null
            )

            Spacer(Modifier.padding(bottom = 16.dp))
        }

        if (errorMessage != null) {
            FormError(errorMessage, Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
        }

        FormButton(
            text = if (isSubmitting) "Creating account…" else "Create Account",
            color = Color(0xFFF0002B),
            enabled = !isSubmitting,
            onClick = {
                submitted = true
                if (allValid) {
                    onSubmit(
                        SeekerForm(
                            fullName = fullName.trim(),
                            bloodTypeNeeded = bloodType,
                            location = location.trim(),
                            contactNumber = contact,
                            notes = notes.trim(),
                            username = username.trim(),
                            password = password
                        )
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}