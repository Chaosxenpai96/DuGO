# DuGO – Requests Page Code Only

This file has **only** the Requests page, the "Sample Hospital" placeholder and the urgent notification. It does not include the Facebook button or the nearest-donor algorithm.

Do the steps in order. Every block starts with a comment banner that says:
- **FILE** – which file to open or create
- **ACTION** – create new / replace / add under a line
- **WHERE** – the exact spot

Paths are relative to `app/src/main/java/com/example/dugo/` unless it says otherwise.

---

## STEP 1 – Create `home/BloodRequest.kt`

```kotlin
// ============================================================
// FILE:   home/BloodRequest.kt
// ACTION: CREATE A NEW FILE in the "home" folder
//         (right-click home > New > Kotlin Class/File > BloodRequest)
// ============================================================
package com.example.dugo.home

// TODO: replace after hospital research
const val PLACEHOLDER_HOSPITAL = "Sample Hospital"

data class BloodRequest(
    val id: Int,
    val bloodType: String,
    val urgent: Boolean,
    val note: String,
    val hospital: String = PLACEHOLDER_HOSPITAL,
    val hospitalLat: Double? = null,   // fill in once real hospitals are added
    val hospitalLng: Double? = null
)

val sampleRequests = listOf(
    BloodRequest(1, "A+", true, "Patient needs two units before 6:00 PM today. Please reach out if you are eligible to donate."),
    BloodRequest(2, "O-", true, "Emergency surgery scheduled tonight. One unit needed as soon as possible."),
    BloodRequest(3, "B+", false, "Scheduled transfusion this Friday. One unit needed, no rush."),
    BloodRequest(4, "AB+", false, "Needed within the week for a recovering patient.")
)
```

---

## STEP 2 – Create `home/RequestsScreen.kt`

```kotlin
// ============================================================
// FILE:   home/RequestsScreen.kt
// ACTION: CREATE A NEW FILE in the "home" folder
// ============================================================
package com.example.dugo.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.dugo.login.Brand

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val CardBorder = Color(0xFFF3D9DC)

@Composable
fun RequestsScreen(
    roleLabel: String = "Blood Donor",
    requests: List<BloodRequest> = sampleRequests,
    selectedTab: HomeTab = HomeTab.Requests,
    onTabSelected: (HomeTab) -> Unit = {},
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    showTestButton: Boolean = false,
    onTestUrgentAlert: () -> Unit = {}
) {
    val context = LocalContext.current
    var helping by remember { mutableStateOf<BloodRequest?>(null) }
    val sorted = remember(requests) { requests.sortedByDescending { it.urgent } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .statusBarsPadding()
    ) {
        Column(Modifier.background(Color.White)) {
            HomeHeader(roleLabel, onNotifications, onProfile)
        }
        HorizontalDivider(color = Color(0xFFE7E9EE))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("Blood requests", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("Urgent needs near Dalaguete", fontSize = 13.sp, color = Gray)
                }
            }
            items(sorted, key = { it.id }) { request ->
                RequestCard(request = request, onHelp = { helping = request })
            }
            if (showTestButton) {
                item {
                    TextButton(onClick = onTestUrgentAlert, modifier = Modifier.fillMaxWidth()) {
                        Text("Test urgent alert (arrives in 5 sec, leave the app)", color = Brand, fontSize = 12.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE7E9EE))
        Box(Modifier.background(Color.White)) { BottomNav(selectedTab, onTabSelected) }
    }

    // After "I can help": tell the donor which hospital to go to
    helping?.let { req ->
        AlertDialog(
            onDismissRequest = { helping = null },
            containerColor = Color.White,
            title = { Text("Thank you for helping!", fontWeight = FontWeight.Bold) },
            text = { Text("Please go to ${req.hospital} to donate ${req.bloodType} blood.\n\n${req.note}") },
            confirmButton = {
                TextButton(onClick = {
                    val uri: Uri = if (req.hospitalLat != null && req.hospitalLng != null) {
                        "geo:${req.hospitalLat},${req.hospitalLng}?q=${req.hospitalLat},${req.hospitalLng}(${Uri.encode(req.hospital)})".toUri()
                    } else {
                        "geo:0,0?q=${Uri.encode(req.hospital)}".toUri()
                    }
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        .onFailure { Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show() }
                    helping = null
                }) { Text("Get directions", color = Brand, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { helping = null }) { Text("Close", color = Gray) }
            }
        )
    }
}

@Composable
private fun RequestCard(request: BloodRequest, onHelp: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, CardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                Text(request.bloodType, color = Brand, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${request.bloodType} blood needed", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(request.hospital, fontSize = 12.sp, color = Gray)
            }
            Spacer(Modifier.width(8.dp))
            if (request.urgent) {
                Pill("URGENT", Color(0xFFFFF0E0), Color(0xFFE8590C))
            } else {
                Pill("ROUTINE", Color(0xFFEEF0F3), Gray)
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(request.note, fontSize = 11.sp, color = Gray, lineHeight = 18.sp)
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE1D2B), contentColor = Color.White)
        ) {
            Text("I can help", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold
    )
}
```

---

## STEP 3 – Create `notifications/NotificationHelper.kt`

```kotlin
// ============================================================
// FILE:   notifications/NotificationHelper.kt
// ACTION: 1) CREATE A NEW FOLDER named "notifications"
//            (right-click com.example.dugo > New > Package > notifications)
//         2) CREATE A NEW FILE in it called NotificationHelper
// ============================================================
package com.example.dugo.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dugo.MainActivity
import com.example.dugo.R
import com.example.dugo.home.BloodRequest

object NotificationHelper {
    const val CHANNEL_URGENT = "urgent_requests"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_URGENT,
                "Urgent blood requests",
                NotificationManager.IMPORTANCE_HIGH   // heads-up pop-up + sound
            ).apply {
                description = "Alerts when someone urgently needs blood"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showUrgentRequest(context: Context, request: BloodRequest) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        createChannels(context)

        val openApp = PendingIntent.getActivity(
            context,
            request.id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = "${request.hospital} • ${request.note}"
        val notification = NotificationCompat.Builder(context, CHANNEL_URGENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // swap for a white DuGO icon later
            .setContentTitle("URGENT: ${request.bloodType} blood needed")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setColor(0xFFD90429.toInt())
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(request.id, notification)
    }
}
```

---

## STEP 4 – Edit `home/HomeScreenMap.kt` (just 2 words)

The Requests page reuses the header and bottom bar from the map screen, so they must not be `private`.

```kotlin
// ============================================================
// FILE:   home/HomeScreenMap.kt   (existing file)
// ACTION: remove the word "private" from these two functions
// ============================================================

// CHANGE THIS:   private fun HomeHeader(roleLabel: String, onNotifications: () -> Unit, onProfile: () -> Unit) {
// TO THIS:
@Composable
internal fun HomeHeader(roleLabel: String, onNotifications: () -> Unit, onProfile: () -> Unit) {

// CHANGE THIS:   private fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
// TO THIS:
@Composable
internal fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
```

---

## STEP 5 – Edit `MainActivity.kt`

```kotlin
// ============================================================
// FILE:   MainActivity.kt   (existing file)
// EDIT 5.1 – IMPORTS
// ACTION: ADD these lines to the import list at the top
// ============================================================
import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.dugo.home.BloodRequest
import com.example.dugo.home.RequestsScreen
import com.example.dugo.notifications.NotificationHelper
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 5.2 – NOTIFICATION PERMISSION LAUNCHER
// ACTION: ADD inside   class MainActivity : ComponentActivity() {
//         ABOVE   override fun onCreate(...)
// ============================================================
    // Android 13+ needs the user's OK before we can show notifications
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 5.3 – CREATE THE NOTIFICATION CHANNEL
// ACTION: ADD inside onCreate, right AFTER the line   enableEdgeToEdge()
// ============================================================
        NotificationHelper.createChannels(this)
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 5.4 – TAB STATE + ASK FOR PERMISSION
// ACTION: ADD inside setContent > DuGOTheme { ... }
//         right UNDER the line   var roleLabel by remember { mutableStateOf("Blood Donor") }
// ============================================================
                var tab by remember { mutableStateOf(HomeTab.Map) }

                // Ask for notification permission once the user is inside the app
                LaunchedEffect(screen) {
                    if (screen == Screen.Home && Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// EDIT 5.5 – HOME SCREEN (map tab + requests tab)
// ACTION: REPLACE the whole   Screen.Home -> HomeScreenMap( ... )   block
//         with this one
// ============================================================
                    Screen.Home -> {
                        val onTab: (HomeTab) -> Unit = { picked ->
                            if (picked == HomeTab.Map || picked == HomeTab.Requests) {
                                tab = picked
                            } else {
                                Toast.makeText(this, "${picked.label} coming soon", Toast.LENGTH_SHORT).show()
                            }
                        }

                        if (tab == HomeTab.Requests) {
                            RequestsScreen(
                                roleLabel = roleLabel,
                                selectedTab = tab,
                                onTabSelected = onTab,
                                onNotifications = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                                onProfile = { Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show() },
                                showTestButton = isDebugBuild,
                                onTestUrgentAlert = {
                                    Toast.makeText(this, "Leave the app now, alert in 5 seconds", Toast.LENGTH_SHORT).show()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        NotificationHelper.showUrgentRequest(
                                            this,
                                            BloodRequest(
                                                id = 99,
                                                bloodType = "A+",
                                                urgent = true,
                                                note = "Patient needs two units before 6:00 PM today."
                                            )
                                        )
                                    }, 5000)
                                }
                            )
                        } else {
                            HomeScreenMap(
                                roleLabel = roleLabel,
                                selectedTab = tab,
                                onTabSelected = onTab,
                                onNotifications = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                                onProfile = { Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show() },
                                onMessageDonor = { donor ->
                                    Toast.makeText(this, "Messaging ${donor.name} coming soon", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
```

---

## STEP 6 – Edit `AndroidManifest.xml`

```xml
<!-- ============================================================
     FILE:   app/src/main/AndroidManifest.xml   (existing file)
     ACTION: ADD this line next to the existing
             <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
             (inside <manifest>, ABOVE <application>)
     ============================================================ -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## HOW TO TEST (no Firebase needed)

1. Run the app and log in with `admin` / `admin123`.
2. Allow notifications when asked.
3. Tap the **Requests** tab, then **"Test urgent alert"**.
4. Press the phone's Home button. About 5 seconds later the heads-up notification appears. Tapping it opens the app.

The test button only shows in debug builds.

---

## OPTIONAL STEP 7 – Alerts when the app is fully closed (Firebase push)

The code above shows the notification, but something must tell the phone that a new urgent request exists. With a fully closed app the standard way is **Firebase Cloud Messaging (FCM)**. You need a Firebase project and a `google-services.json` file first.

```kotlin
// ============================================================
// FILE:   build.gradle.kts   (the ROOT one, next to settings.gradle.kts)
// ACTION: ADD this line inside the   plugins { ... }   block
// ============================================================
    id("com.google.gms.google-services") version "4.4.3" apply false


// ============================================================
// FILE:   app/build.gradle.kts
// ACTION A: ADD this line inside the   plugins { ... }   block at the top
// ============================================================
    id("com.google.gms.google-services")

// ACTION B: ADD these two lines inside   dependencies { ... }
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-messaging")

// (Use the newest versions Android Studio suggests.)
// ALSO: put the downloaded google-services.json file inside the  app/  folder.
```

```kotlin
// ============================================================
// FILE:   notifications/DugoMessagingService.kt
// ACTION: CREATE A NEW FILE in the "notifications" folder
// ============================================================
package com.example.dugo.notifications

import com.example.dugo.home.BloodRequest
import com.example.dugo.home.PLACEHOLDER_HOSPITAL
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DugoMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        if (d["urgent"] != "true") return
        val request = BloodRequest(
            id = d["id"]?.toIntOrNull() ?: System.currentTimeMillis().toInt(),
            bloodType = d["bloodType"] ?: return,
            urgent = true,
            note = d["note"].orEmpty(),
            hospital = d["hospital"] ?: PLACEHOLDER_HOSPITAL
        )
        NotificationHelper.showUrgentRequest(this, request)
    }
}

/** FCM topic names can't contain + or -, so "A+" becomes "urgent_Apos". */
fun urgentTopic(bloodType: String) =
    "urgent_" + bloodType.replace("+", "pos").replace("-", "neg")
```

```xml
<!-- ============================================================
     FILE:   app/src/main/AndroidManifest.xml
     ACTION: ADD inside <application> ... </application>
             (for example right ABOVE the <activity> tag)
     ============================================================ -->
        <service
            android:name=".notifications.DugoMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
```

```kotlin
// ============================================================
// FILE:   MainActivity.kt
// ACTION: ADD inside   Screen.RegisterDonor -> RegisterDonor( ... onSubmit = { form -> ... } )
//         right UNDER the TODO comment, so each donor only gets
//         alerts for their own blood type
// ============================================================
                            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                                .subscribeToTopic(com.example.dugo.notifications.urgentTopic(form.bloodType))
```

**Sending an alert (your server / Cloud Function):** send a **data-only** message to the topic (for example `urgent_Apos`) with the fields `urgent` = `"true"`, `bloodType`, `hospital`, `note`. Do NOT include a `notification` block, otherwise Android shows its own plain notification when the app is closed and skips your code.

---

## Replacing the "Sample Hospital" later

Every request uses `PLACEHOLDER_HOSPITAL = "Sample Hospital"` from `home/BloodRequest.kt`. After your hospital research, give each request its real `hospital`, `hospitalLat` and `hospitalLng`. The card, the notification and the "Get directions" button update automatically, and directions then go to the exact spot.

---

## Quick checklist of files

| Action | File |
|---|---|
| **Create** | `home/BloodRequest.kt` |
| **Create** | `home/RequestsScreen.kt` |
| **Create** | `notifications/NotificationHelper.kt` |
| Edit | `home/HomeScreenMap.kt` (remove `private` from 2 functions) |
| Edit | `MainActivity.kt` |
| Edit | `AndroidManifest.xml` |
| Optional create | `notifications/DugoMessagingService.kt` |
| Optional edit | both `build.gradle.kts` files + add `google-services.json` |
