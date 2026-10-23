package com.example.dugo.login

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.location.currentLocation
import com.example.dugo.location.describeLocation
import com.example.dugo.location.hasLocationPermission
import kotlinx.coroutines.launch
import java.util.Locale

/** What the location picker found. */
data class PickedLocation(
    val latitude: Double,
    val longitude: Double,
    /** A place name, or empty when it couldn't be worked out. */
    val label: String
)

/**
 * Finds the device's position as soon as the screen opens and fills the
 * location field in.
 *
 * The user still gets a button to try again, and can type over the result.
 */
@Composable
fun AutoLocationField(
    location: String,
    onLocationChange: (String) -> Unit,
    onPicked: (PickedLocation) -> Unit,
    modifier: Modifier = Modifier,
    /** Whether to ask for permission and locate the person straight away. */
    autoStart: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf<PickedLocation?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var hasTried by remember { mutableStateOf(false) }

    // Fills in the place name, but only while the user hasn't typed their own
    fun applyPlace(place: String) {
        if (place.isNotBlank() && location.isBlank()) onLocationChange(place)
    }

    fun lookUp() {
        if (isLocating) return
        isLocating = true
        note = "Getting your location…"
        scope.launch {
            val gps = currentLocation(context)
            if (gps == null) {
                note = "Couldn't get your location. Try again."
                isLocating = false
                return@launch
            }
            val place = describeLocation(context, gps.first, gps.second)
                ?: String.format(Locale.getDefault(), "%.4f, %.4f", gps.first, gps.second)
            pin = PickedLocation(gps.first, gps.second, place)
            applyPlace(place)
            note = "Pin location saved"
            isLocating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allowed = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) lookUp() else {
            note = "Location permission denied"
            isLocating = false
        }
    }

    // Asks for permission and locates the person the first time the form opens
    LaunchedEffect(autoStart) {
        if (!autoStart) return@LaunchedEffect
        if (hasLocationPermission(context)) {
            lookUp()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    OutlinedButton(
        onClick = {
            if (hasLocationPermission(context)) lookUp() else permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        enabled = !isLocating,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand),
        border = ButtonDefaults.outlinedButtonBorder(enabled = !isLocating)
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                isLocating -> "Getting your location…"
                pin != null -> "Location filled in — tap to update"
                else -> "Use my current location"
            },
            fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
    }

    if (note != null) {
        Text(
            note!!,
            color = if (pin != null) Color(0xFF2E9E6B) else Color(0xFF8A8F99),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )
    }
}
