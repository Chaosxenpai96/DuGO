package com.example.dugo.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The details a social sign-in still has to supply. */
data class SocialProfileForm(
    val role: String,
    val bloodType: String,
    val location: String,
    val contactNumber: String
)

/**
 * Shown after a Google or Facebook sign-in that doesn't have a profile yet, so
 * the person can say whether they're a donor or a seeker.
 */
@Composable
fun CompleteProfileScreen(
    suggestedName: String = "",
    onSubmit: (SocialProfileForm) -> Unit = {},
    errorMessage: String? = null,
    isSubmitting: Boolean = false
) {
    var isDonor by remember { mutableStateOf(true) }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val bloodOk = bloodType.isNotEmpty()
    val locationOk = location.isNotBlank()
    val contactOk = isValidPhNumber(contact)
    val allValid = bloodOk && locationOk && contactOk

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
                title = "One more step",
                subtitle = if (suggestedName.isNotBlank())
                    "Welcome, $suggestedName. Tell us how you'll use DuGO."
                else
                    "Tell us how you'll use DuGO.",
                onBack = {}
            )

            FormLabel("I am a")
            RoleChoice(
                label = "Blood Donor",
                description = "I'll donate blood to people who need it.",
                selected = isDonor,
                onClick = { isDonor = true }
            )
            Spacer(Modifier.height(10.dp))
            RoleChoice(
                label = "Blood Seeker",
                description = "I'll post requests for blood.",
                selected = !isDonor,
                onClick = { isDonor = false }
            )

            FormLabel("Blood Type")
            BloodTypeDropdown(
                selected = bloodType,
                onSelected = { bloodType = it },
                error = if (submitted && !bloodOk) "Please select a blood type" else null
            )

            FormLabel("Location")
            FormTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = "e.g. Poblacion, Cebu City",
                leadingIcon = Icons.Outlined.LocationOn,
                error = if (submitted && !locationOk) "Please enter your location" else null
            )

            FormLabel("Contact Number")
            FormTextField(
                value = contact,
                onValueChange = { input -> contact = input.filter { it.isDigit() || it == '+' }.take(13) },
                placeholder = "09XX XXX XXXX",
                keyboardType = KeyboardType.Phone,
                error = if (submitted && !contactOk) "Enter a valid number (09XXXXXXXXX)" else null
            )

            Spacer(Modifier.padding(bottom = 16.dp))
        }

        if (errorMessage != null) {
            FormError(errorMessage, Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
        }

        FormButton(
            text = if (isSubmitting) "Saving…" else "Continue",
            color = Brand,
            enabled = !isSubmitting,
            onClick = {
                submitted = true
                if (allValid) {
                    onSubmit(
                        SocialProfileForm(
                            role = if (isDonor) "Blood Donor" else "Blood Seeker",
                            bloodType = bloodType,
                            location = location.trim(),
                            contactNumber = contact
                        )
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun RoleChoice(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Brand.copy(alpha = 0.08f) else Color(0xFFFCFCFD))
            .border(1.dp, if (selected) Brand else Color(0xFFE1EAF2), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Brand,
                unselectedColor = Color(0xFFB8CAD9)
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Text(description, fontSize = 13.sp, color = Color(0xFF6B7280))
        }
    }
}
