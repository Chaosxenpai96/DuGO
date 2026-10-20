package com.example.dugo.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.dugo.R
import com.example.dugo.login.Brand
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val Red = Color(0xFFEE1D2B)

/** Keys and accessors for settings stored on this device. */
object AppSettings {
    const val PREFS_NAME = "dugo_settings"

    // Notifications
    const val NOTIF_MASTER = "notif_master"
    const val NOTIF_REQUESTS = "notif_requests"
    const val NOTIF_URGENT = "notif_urgent"
    const val NOTIF_MESSAGES = "notif_messages"
    const val NOTIF_REMINDERS = "notif_reminders"

    // App settings
    const val DEFAULT_RADIUS_KM = "default_radius_km"

    // Privacy
    const val SHOW_CONTACT = "show_contact"
    const val EXPORT_CONTACT = "export_contact"

    // Data
    const val DONATIONS = "donation_history"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

enum class SettingsPage(val title: String) {
    Notifications("Notifications"),
    AppSettings("App Settings"),
    PrivacySecurity("Privacy & Security"),
    DonationHistory("Donation History"),
    About("About DuGO")
}

// ---------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        HorizontalDivider(color = Line)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF7F8FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) Brand else Gray, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Ink else Gray
            )
            Text(subtitle, fontSize = 12.sp, color = Gray, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Brand)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF7F8FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Brand, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Gray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text, color = Gray, fontSize = 12.sp, lineHeight = 17.sp)
}

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppSettings.prefs(context) }

    var master by remember { mutableStateOf(prefs.getBoolean(AppSettings.NOTIF_MASTER, true)) }
    var requests by remember { mutableStateOf(prefs.getBoolean(AppSettings.NOTIF_REQUESTS, true)) }
    var urgent by remember { mutableStateOf(prefs.getBoolean(AppSettings.NOTIF_URGENT, true)) }
    var messages by remember { mutableStateOf(prefs.getBoolean(AppSettings.NOTIF_MESSAGES, true)) }
    var reminders by remember { mutableStateOf(prefs.getBoolean(AppSettings.NOTIF_REMINDERS, false)) }
    var hasPermission by remember {
        mutableStateOf(com.example.dugo.auth.NotificationManager.hasPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            com.example.dugo.auth.NotificationManager.syncTopics(context)
        }
    }

    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        // Keep the Firebase topic subscriptions in step with the preference
        com.example.dugo.auth.NotificationManager.syncTopics(context)
    }

    SettingsScaffold("Notifications", onBack) {
        if (!hasPermission) {
            // Android 13+ needs the runtime permission before any alert can show
            OutlinedButton(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(
                                android.provider.Settings.EXTRA_APP_PACKAGE,
                                context.packageName
                            )
                        runCatching { context.startActivity(intent) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allow notifications on this device")
            }
            Spacer(Modifier.height(10.dp))
        }

        InfoText("Choose which alerts DuGO may send you.")
        Spacer(Modifier.height(8.dp))

        SwitchRow(
            icon = Icons.Outlined.Notifications,
            title = "Allow notifications",
            subtitle = "Turn all alerts on or off",
            checked = master,
            onCheckedChange = { master = it; save(AppSettings.NOTIF_MASTER, it) }
        )

        HorizontalDivider(color = Line)

        SectionLabel("ALERTS")
        SwitchRow(
            icon = Icons.Outlined.Favorite,
            title = "Blood request alerts",
            subtitle = "New requests posted near your location",
            checked = requests,
            enabled = master,
            onCheckedChange = { requests = it; save(AppSettings.NOTIF_REQUESTS, it) }
        )
        SwitchRow(
            icon = Icons.Outlined.Call,
            title = "Urgent request alerts",
            subtitle = "Notify me immediately about urgent requests",
            checked = urgent,
            enabled = master,
            onCheckedChange = { urgent = it; save(AppSettings.NOTIF_URGENT, it) }
        )
        SwitchRow(
            icon = Icons.Outlined.MailOutline,
            title = "Message notifications",
            subtitle = "New chat messages from donors and seekers",
            checked = messages,
            enabled = master,
            onCheckedChange = { messages = it; save(AppSettings.NOTIF_MESSAGES, it) }
        )
        SwitchRow(
            icon = Icons.Outlined.DateRange,
            title = "Appointment reminders",
            subtitle = "Reminders for scheduled donations",
            checked = reminders,
            enabled = master,
            onCheckedChange = { reminders = it; save(AppSettings.NOTIF_REMINDERS, it) }
        )

        Spacer(Modifier.height(12.dp))
        InfoText("Preferences are saved on this device. Push delivery will start working once the backend is connected.")
    }
}

// ---------------------------------------------------------------------------
// App settings
// ---------------------------------------------------------------------------

@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppSettings.prefs(context) }

    var radiusKm by remember {
        mutableStateOf(
            prefs.getFloat(AppSettings.DEFAULT_RADIUS_KM, 20f).coerceIn(1f, 20f)
        )
    }

    SettingsScaffold("App Settings", onBack) {
        SectionLabel("DONOR SEARCH")
        Text("Default search radius", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Used when the donor map opens", color = Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${radiusKm.toInt()} km", color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = radiusKm,
            onValueChange = { radiusKm = it },
            onValueChangeFinished = {
                prefs.edit().putFloat(AppSettings.DEFAULT_RADIUS_KM, radiusKm).apply()
            },
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = Brand,
                activeTrackColor = Brand,
                inactiveTrackColor = Line
            )
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(8.dp))

        SectionLabel("RESET")
        OutlinedButton(
            onClick = {
                prefs.edit().remove(AppSettings.DEFAULT_RADIUS_KM).apply()
                radiusKm = 20f
                Toast.makeText(context, "Search settings reset", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset search settings")
        }
        Spacer(Modifier.height(8.dp))
        InfoText("This only resets search preferences. Notification and privacy choices are unaffected.")
    }
}

// ---------------------------------------------------------------------------
// Privacy & security
// ---------------------------------------------------------------------------

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppSettings.prefs(context) }

    var showContact by remember { mutableStateOf(prefs.getBoolean(AppSettings.SHOW_CONTACT, true)) }
    var exportContact by remember { mutableStateOf(prefs.getBoolean(AppSettings.EXPORT_CONTACT, true)) }
    var showClearDialog by remember { mutableStateOf(false) }

    SettingsScaffold("Privacy & Security", onBack) {
        SectionLabel("VISIBILITY")
        SwitchRow(
            icon = Icons.Outlined.Person,
            title = "Show my contact number",
            subtitle = "Display your number on your profile",
            checked = showContact,
            onCheckedChange = {
                showContact = it
                prefs.edit().putBoolean(AppSettings.SHOW_CONTACT, it).apply()
            }
        )
        SwitchRow(
            icon = Icons.Outlined.Email,
            title = "Include contact in data export",
            subtitle = "Add your number to exported data",
            checked = exportContact,
            onCheckedChange = {
                exportContact = it
                prefs.edit().putBoolean(AppSettings.EXPORT_CONTACT, it).apply()
            }
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(8.dp))

        SectionLabel("YOUR DATA")
        InfoText("DuGO keeps your profile, settings and donation history on this device. No data is sent anywhere yet.")
        Spacer(Modifier.height(12.dp))

        ActionRow(
            icon = Icons.Outlined.Email,
            title = "Export my data",
            subtitle = "Share a text copy of your profile and donations",
            onClick = { shareExport(context, exportContact) }
        )

        OutlinedButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear local data")
        }

        Spacer(Modifier.height(8.dp))
        InfoText("Security: no account password is stored on this device. Sign-in and account security will be handled by the backend once it is connected.")
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear local data?") },
            text = {
                Text("This removes your saved settings and donation history from this device. It cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSettings.clear(context)
                        // Keep an explicit empty history so sample records are not re-seeded
                        saveDonations(AppSettings.prefs(context), emptyList())
                        UserSession.profile = UserSession.profile.copy(totalDonations = 0)
                        showClearDialog = false
                        Toast.makeText(context, "Local data cleared", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                ) {
                    Text("Clear", color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Donation history
// ---------------------------------------------------------------------------

@Composable
fun DonationHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppSettings.prefs(context) }

    var records by remember { mutableStateOf(loadDonations(prefs)) }
    var showLogDialog by remember { mutableStateOf(false) }

    fun persist(list: MutableList<DonationRecord>) {
        records = list
        saveDonations(prefs, list)
        UserSession.profile = UserSession.profile.copy(totalDonations = list.size)
    }

    SettingsScaffold("Donation History", onBack) {
        val sorted = records.sortedByDescending { it.date }
        val last = sorted.firstOrNull()
        val eligibleAt = last?.let { it.date + ELIGIBILITY_DAYS * DAY_MS }
        val eligibleNow = eligibleAt == null || System.currentTimeMillis() >= eligibleAt

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF7F8FA))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(records.size.toString(), "Donations")
            StatItem(last?.let { formatDate(it.date) } ?: "—", "Last")
            StatItem(
                if (eligibleNow) "Eligible" else formatDate(eligibleAt),
                "Next eligible"
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showLogDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log a donation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        if (sorted.isEmpty()) {
            Text(
                "No donations recorded yet. Log your first donation to start tracking.",
                color = Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            sorted.forEach { record ->
                DonationCard(
                    record = record,
                    onDelete = {
                        persist(records.filterNot { it.id == record.id }.toMutableList())
                        Toast.makeText(context, "Donation removed", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        InfoText("Whole-blood donors are typically eligible again about 90 days after a donation.")
    }

    if (showLogDialog) {
        LogDonationDialog(
            bloodType = UserSession.profile.bloodType,
            onDismiss = { showLogDialog = false },
            onSave = { hospital, location, notes ->
                val nextId = (records.maxOfOrNull { it.id } ?: 0) + 1
                val updated = records.toMutableList().apply {
                    add(
                        DonationRecord(
                            id = nextId,
                            date = System.currentTimeMillis(),
                            bloodType = UserSession.profile.bloodType,
                            hospital = hospital,
                            location = location,
                            notes = notes
                        )
                    )
                }
                persist(updated)
                showLogDialog = false
                Toast.makeText(context, "Donation logged", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, fontSize = 11.sp, color = Gray)
    }
}

@Composable
private fun DonationCard(record: DonationRecord, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brand.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Favorite, contentDescription = null, tint = Brand, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(record.hospital.ifBlank { "Donation" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.bloodType, color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "  •  ${formatDate(record.date)}${if (record.location.isNotBlank()) "  •  ${record.location}" else ""}",
                    color = Gray, fontSize = 12.sp
                )
            }
            if (record.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(record.notes, color = Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Icon(
            Icons.Outlined.Close,
            contentDescription = "Remove donation",
            tint = Gray,
            modifier = Modifier.size(18.dp).clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun LogDonationDialog(
    bloodType: String,
    onDismiss: () -> Unit,
    onSave: (hospital: String, location: String, notes: String) -> Unit
) {
    var hospital by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val hospitalOk = hospital.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Log a Donation", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Gray,
                        modifier = Modifier.size(22.dp).clickable(onClick = onDismiss)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Blood type $bloodType · today", color = Gray, fontSize = 13.sp)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Hospital / Center") },
                    singleLine = true,
                    isError = submitted && !hospitalOk,
                    supportingText = if (submitted && !hospitalOk) {
                        { Text("Please enter where you donated") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            submitted = true
                            if (hospitalOk) onSave(hospital.trim(), location.trim(), notes.trim())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) { Text("Save") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// About
// ---------------------------------------------------------------------------

@Composable
fun AboutDuGOScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var infoDialog by remember { mutableStateOf<InfoDialogType?>(null) }

    SettingsScaffold("About DuGO", onBack) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.dugo_logo),
                contentDescription = "DuGO",
                modifier = Modifier.height(56.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("DuGO", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Version ${appVersion(context)}", fontSize = 13.sp, color = Gray)
        }

        Spacer(Modifier.height(8.dp))
        InfoText(
            "DuGO connects blood donors and seekers across Cebu. " +
                "Post a request, find nearby donors, and coordinate directly — " +
                "built to make urgent blood needs easier to meet."
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(8.dp))

        ActionRow(
            icon = Icons.Outlined.Email,
            title = "Contact us",
            subtitle = "dugo.support@example.com",
            onClick = { sendEmail(context) }
        )
        ActionRow(
            icon = Icons.Outlined.Info,
            title = "Privacy policy",
            onClick = { infoDialog = InfoDialogType.Privacy }
        )
        ActionRow(
            icon = Icons.Outlined.Settings,
            title = "Terms of use",
            onClick = { infoDialog = InfoDialogType.Terms }
        )
        ActionRow(
            icon = Icons.Outlined.LocationOn,
            title = "Location",
            subtitle = "Cebu, Philippines",
            onClick = {
                val uri = "geo:10.3167,123.8854?q=Cebu,+Philippines".toUri()
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    .onFailure { Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show() }
            }
        )

        Spacer(Modifier.height(20.dp))
        Text(
            "© 2026 DuGO",
            color = Gray,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    when (infoDialog) {
        InfoDialogType.Privacy -> AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text("Privacy policy") },
            text = {
                Text(
                    "This is a demo build. Your profile, settings and donation history are stored only on this device. " +
                        "No data is uploaded. When online features are added, this policy will be updated to describe them."
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) { Text("Close") }
            }
        )
        InfoDialogType.Terms -> AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text("Terms of use") },
            text = {
                Text(
                    "DuGO helps people coordinate blood donations. Information in this demo app is sample data. " +
                        "Do not rely on it for medical decisions. Always confirm eligibility and requirements with the hospital or blood center."
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) { Text("Close") }
            }
        )
        null -> Unit
    }
}

private enum class InfoDialogType { Privacy, Terms }

private fun appVersion(context: Context): String =
    runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "1.0"

private fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:dugo.support@example.com".toUri()).apply {
        putExtra(Intent.EXTRA_SUBJECT, "DuGO support")
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show() }
}

// ---------------------------------------------------------------------------
// Data export
// ---------------------------------------------------------------------------

private fun shareExport(context: Context, includeContact: Boolean) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "My DuGO data")
        putExtra(Intent.EXTRA_TEXT, buildExportText(context, includeContact))
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Export my data")) }
        .onFailure { Toast.makeText(context, "Couldn't open the share sheet", Toast.LENGTH_SHORT).show() }
}

private fun buildExportText(context: Context, includeContact: Boolean): String {
    val profile = UserSession.profile
    val donations = loadDonations(AppSettings.prefs(context)).sortedByDescending { it.date }
    return buildString {
        appendLine("DuGO data export")
        appendLine("Name: ${profile.fullName} (@${profile.username})")
        appendLine("Role: ${profile.role}")
        appendLine("Blood type: ${profile.bloodType}")
        appendLine("Location: ${profile.location}")
        if (includeContact) appendLine("Contact: ${profile.contactNumber}")
        appendLine("Total donations: ${donations.size}")
        appendLine()
        appendLine("Donation history:")
        if (donations.isEmpty()) {
            appendLine("No donations recorded.")
        } else {
            donations.forEach { d ->
                val place = listOf(d.hospital, d.location).filter { it.isNotBlank() }.joinToString(", ")
                appendLine("• ${formatDate(d.date)} — $place (${d.bloodType})")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Donation records
// ---------------------------------------------------------------------------

data class DonationRecord(
    val id: Int,
    val date: Long,
    val bloodType: String,
    val hospital: String,
    val location: String,
    val notes: String = ""
)

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val ELIGIBILITY_DAYS = 90L

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

/**
 * Loads the donations logged on this device. A new account starts with none —
 * there's no sample history.
 */
private fun loadDonations(prefs: SharedPreferences): MutableList<DonationRecord> {
    val raw = prefs.getString(AppSettings.DONATIONS, null) ?: return mutableListOf()
    return runCatching {
        val array = JSONArray(raw)
        MutableList(array.length()) { i ->
            val o = array.getJSONObject(i)
            DonationRecord(
                id = o.optInt("id", i + 1),
                date = o.optLong("date", System.currentTimeMillis()),
                bloodType = o.optString("bloodType", UserSession.profile.bloodType),
                hospital = o.optString("hospital", ""),
                location = o.optString("location", ""),
                notes = o.optString("notes", "")
            )
        }
    }.getOrElse { mutableListOf() }
}

private fun saveDonations(prefs: SharedPreferences, list: List<DonationRecord>) {
    val array = JSONArray()
    list.forEach { r ->
        array.put(
            JSONObject().apply {
                put("id", r.id)
                put("date", r.date)
                put("bloodType", r.bloodType)
                put("hospital", r.hospital)
                put("location", r.location)
                put("notes", r.notes)
            }
        )
    }
    prefs.edit().putString(AppSettings.DONATIONS, array.toString()).apply()
}
