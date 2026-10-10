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
    val notes: String
)

@Composable
fun RegisterSeeker(
    onBack: () -> Unit = {},
    onSubmit: (SeekerForm) -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val nameOk = fullName.isNotBlank()
    val bloodOk = bloodType.isNotEmpty()
    val locationOk = location.isNotBlank()
    val contactOk = isValidPhNumber(contact)
    val allValid = nameOk && bloodOk && locationOk && contactOk

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

            Spacer(Modifier.padding(bottom = 16.dp))
        }

        FormButton(
            text = "Create Account",
            color = Color(0xFFF0002B),
            onClick = {
                submitted = true
                if (allValid) {
                    onSubmit(
                        SeekerForm(
                            fullName = fullName.trim(),
                            bloodTypeNeeded = bloodType,
                            location = location.trim(),
                            contactNumber = contact,
                            notes = notes.trim()
                        )
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}