package com.example.dugo.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DonorForm(
    val fullName: String,
    val bloodType: String,
    val location: String,
    val contactNumber: String,
    val availableAnytime: Boolean,
    val specificDates: String,
    val username: String,
    val password: String
)

private enum class Availability { Anytime, SpecificDates }

@Composable
fun RegisterDonor(
    onBack: () -> Unit = {},
    onSubmit: (DonorForm) -> Unit = {},
    errorMessage: String? = null,
    isSubmitting: Boolean = false
) {
    var fullName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf(Availability.Anytime) }
    var dates by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val nameOk = fullName.isNotBlank()
    val bloodOk = bloodType.isNotEmpty()
    val locationOk = location.isNotBlank()
    val contactOk = isValidPhNumber(contact)
    val datesOk = availability == Availability.Anytime || dates.isNotBlank()
    val usernameOk = isValidUsername(username)
    val passwordOk = password.length >= 8
    val confirmOk = confirmPassword.isNotEmpty() && confirmPassword == password
    val allValid = nameOk && bloodOk && locationOk && contactOk && datesOk &&
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
                title = "Register as a Donor",
                subtitle = "Your generosity can save lives. Fill in your details to become a registered blood donor.",
                onBack = onBack
            )

            FormLabel("Full Name")
            FormTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Juan Dela Cruz",
                error = if (submitted && !nameOk) "Please enter your full name" else null
            )

            FormLabel("Blood Type")
            BloodTypeDropdown(
                selected = bloodType,
                onSelected = { bloodType = it },
                error = if (submitted && !bloodOk) "Please select your blood type" else null
            )

            FormLabel("Location")
            FormTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = "Cebu City, Cebu",
                leadingIcon = Icons.Outlined.LocationOn,
                error = if (submitted && !locationOk) "Please enter your location" else null
            )

            FormLabel("Contact Number")
            FormTextField(
                value = contact,
                onValueChange = { input -> contact = input.filter { it.isDigit() || it == '+' }.take(13) },
                placeholder = "09XX XXX XXXX",
                keyboardType = KeyboardType.Phone,
                error = if (submitted && !contactOk) "Enter a valid number (09XXXXXXXXX or +639XXXXXXXXX)" else null
            )

            FormLabel("I am available to donate:")
            RadioRow(
                label = "Anytime",
                selected = availability == Availability.Anytime,
                onClick = { availability = Availability.Anytime }
            )
            RadioRow(
                label = "Only on specific dates",
                selected = availability == Availability.SpecificDates,
                onClick = { availability = Availability.SpecificDates }
            )

            if (availability == Availability.SpecificDates) {
                Spacer(Modifier.padding(top = 8.dp))
                FormTextField(
                    value = dates,
                    onValueChange = { dates = it },
                    placeholder = "e.g. Oct 12, Oct 15, weekends",
                    error = if (submitted && !datesOk) "Please enter the dates you're available" else null
                )
            }

            FormLabel("Username")
            FormTextField(
                value = username,
                onValueChange = { input -> username = input.filter { it.isLetterOrDigit() || it == '_' }.take(20) },
                placeholder = "e.g. juan_delacruz",
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
            color = Color(0xFFDC4660),
            enabled = !isSubmitting,
            onClick = {
                submitted = true
                if (allValid) {
                    onSubmit(
                        DonorForm(
                            fullName = fullName.trim(),
                            bloodType = bloodType,
                            location = location.trim(),
                            contactNumber = contact,
                            availableAnytime = availability == Availability.Anytime,
                            specificDates = if (availability == Availability.SpecificDates) dates.trim() else "",
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

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFDC4660),
                unselectedColor = Color(0xFFB8CAD9)
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
    }
}