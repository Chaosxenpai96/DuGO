package com.example.dugo.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.dugo.login.BLOOD_TYPES
import com.example.dugo.login.BloodTypeDropdown
import com.example.dugo.login.Brand
import com.example.dugo.login.FormButton
import com.example.dugo.login.FormError
import com.example.dugo.login.FormLabel
import com.example.dugo.login.FormTextField
import com.example.dugo.login.AutoLocationField
import com.example.dugo.login.isValidPhNumber
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val UrgentRed = Color(0xFFEE1D2B)
private val NormalGreen = Color(0xFF2E9E6B)

@Composable
fun RequestsScreen(
    roleLabel: String = "Blood Seeker",
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    selectedTab: HomeTab = HomeTab.Requests,
    onTabSelected: (HomeTab) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<BloodRequest>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    // Hidden from this feed only; the request stays in the database for everyone else
    var dismissed by remember { mutableStateOf(setOf<String>()) }
    var showPostDialog by remember { mutableStateOf(false) }
    var isPosting by remember { mutableStateOf(false) }
    var postError by remember { mutableStateOf<String?>(null) }
    var filterUrgency by remember { mutableStateOf<UrgencyLevel?>(null) }
    var filterBloodType by remember { mutableStateOf("All blood types") }
    var selectedRequest by remember { mutableStateOf<BloodRequest?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showChatScreen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val registration = RequestRepository.listen(
            onChange = { requests = it; loadError = null },
            onError = { loadError = it }
        )
        onDispose { registration.remove() }
    }

    val filtered = requests.filter { req ->
        req.id !in dismissed &&
        (filterUrgency == null || req.urgency == filterUrgency) &&
        (filterBloodType == "All blood types" || req.bloodType == filterBloodType)
    }.sortedWith(compareByDescending<BloodRequest> { it.urgency == UrgencyLevel.Urgent }
        .thenByDescending { it.postedAt })

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
        RequestsHeader(roleLabel, onNotifications, onProfile, onPost = { showPostDialog = true })

        HorizontalDivider(color = Line)

        // Filter bar
        FilterBar(
            selectedUrgency = filterUrgency,
            onUrgencySelected = { filterUrgency = if (filterUrgency == it) null else it },
            selectedBloodType = filterBloodType,
            onBloodTypeSelected = { filterBloodType = it }
        )

        HorizontalDivider(color = Line)

        // Feed
        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val error = loadError
                    if (error != null) {
                        Text("Couldn't load requests", color = Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = UrgentRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                    } else {
                        Text("No requests yet", color = Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Be the first to post a blood request.", color = Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filtered.forEach { req ->
                    RequestCard(
                        request = req,
                        onContact = {
                            selectedRequest = req
                            showContactDialog = true
                        },
                        onDismiss = { dismissed = dismissed + req.id }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (showContactDialog && selectedRequest != null) {
            ContactDialog(
                request = selectedRequest!!,
                onDismiss = {
                    showContactDialog = false
                    selectedRequest = null
                },
                onCall = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, "tel:${selectedRequest?.contact}".toUri())
                        )
                    }.onFailure {
                        Toast.makeText(context, "No dialer found", Toast.LENGTH_SHORT).show()
                    }
                    showContactDialog = false
                },
                onMessage = {
                    showContactDialog = false
                    showChatScreen = true
                }
            )
        }

        if (showChatScreen) {
            MessagesScreen(
                selectedTab = HomeTab.Messages,
                onTabSelected = onTabSelected,
                onNotifications = onNotifications,
                onProfile = onProfile,
                startWith = Donor(
                    id = 0,
                    name = selectedRequest?.posterName ?: "Requester",
                    bloodType = selectedRequest?.bloodType ?: "",
                    availableToday = true,
                    area = selectedRequest?.location ?: "",
                    phone = selectedRequest?.contact ?: "",
                    lat = 0.0,
                    lng = 0.0,
                    uid = selectedRequest?.id ?: ""
                ),
                onStarted = { showChatScreen = false }
            )
            return
        }

        HorizontalDivider(color = Line)
        BottomNavBar(selectedTab, onTabSelected)
    }

    if (showPostDialog) {
        PostRequestDialog(
            isPosting = isPosting,
            errorMessage = postError,
            initialName = UserSession.profile.fullName,
            initialLocation = UserSession.profile.location,
            onDismiss = {
                showPostDialog = false
                postError = null
            },
            onSubmit = { newRequest ->
                isPosting = true
                postError = null
                scope.launch {
                    val result = RequestRepository.post(newRequest)
                    isPosting = false
                    when (result) {
                        PostResult.Posted -> {
                            showPostDialog = false
                            Toast.makeText(context, "Request posted!", Toast.LENGTH_SHORT).show()
                        }
                        PostResult.Queued -> {
                            showPostDialog = false
                            Toast.makeText(
                                context,
                                "You're offline. The request will post when you reconnect.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is PostResult.Failed -> postError = result.message
                    }
                }
            }
        )
    }
}

@Composable
private fun RequestsHeader(
    roleLabel: String,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onPost: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Blood Requests", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(roleLabel, color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onPost,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UrgentRed, contentColor = Color.White),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Post Request", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Gray)
        }
    }
}

@Composable
private fun FilterBar(
    selectedUrgency: UrgencyLevel?,
    onUrgencySelected: (UrgencyLevel) -> Unit,
    selectedBloodType: String,
    onBloodTypeSelected: (String) -> Unit
) {
    var bloodDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Filter:", color = Gray, fontSize = 13.sp)

        // Urgency chips
        UrgencyLevel.entries.forEach { level ->
            val active = selectedUrgency == level
            val chipColor = if (level == UrgencyLevel.Urgent) UrgentRed else NormalGreen
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) chipColor else chipColor.copy(alpha = 0.1f))
                    .clickable { onUrgencySelected(level) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${level.emoji} ${level.label}",
                    color = if (active) Color.White else chipColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Blood type dropdown
        Box {
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF7F8FA))
                    .border(1.dp, Line, RoundedCornerShape(20.dp))
                    .clickable { bloodDropdownExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedBloodType == "All blood types") "Type" else selectedBloodType,
                    color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = bloodDropdownExpanded, onDismissRequest = { bloodDropdownExpanded = false }) {
                listOf("All blood types") + BLOOD_TYPES.toList()
                DropdownMenuItem(text = { Text("All types") }, onClick = {
                    onBloodTypeSelected("All blood types"); bloodDropdownExpanded = false
                })
                BLOOD_TYPES.forEach { bt ->
                    DropdownMenuItem(text = { Text(bt) }, onClick = {
                        onBloodTypeSelected(bt); bloodDropdownExpanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: BloodRequest,
    onContact: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val urgencyColor = if (request.urgency == UrgencyLevel.Urgent) UrgentRed else NormalGreen

    Column(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (request.urgency == UrgencyLevel.Urgent) UrgentRed.copy(alpha = 0.3f) else Line, shape)
            .background(Color.White, shape)
            .padding(16.dp)
    ) {
        // Top row: urgency badge + dismiss
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(urgencyColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${request.urgency.emoji} ${request.urgency.label}",
                    color = urgencyColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            // Blood type badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brand.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(request.bloodType, color = Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(request.timeAgo(), color = Gray, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Dismiss",
                tint = Gray,
                modifier = Modifier.size(18.dp).clickable(onClick = onDismiss)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Poster name
        Text(request.posterName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)

        // Hospital / location
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (request.hospital.isNotBlank()) "${request.hospital} · ${request.location}"
                else request.location,
                color = Gray, fontSize = 12.sp
            )
        }

        // Notes
        if (request.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(request.notes, color = Ink.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 19.sp)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onContact,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UrgentRed, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Call", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(
                onClick = onContact,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Message", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PostRequestDialog(
    onDismiss: () -> Unit,
    onSubmit: (BloodRequest) -> Unit,
    isPosting: Boolean = false,
    errorMessage: String? = null,
    /** The signed-in user's saved details, used to fill the form in. */
    initialName: String = "",
    initialLocation: String = ""
) {
    var posterName by remember { mutableStateOf(initialName) }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(initialLocation) }
    var hospital by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf(UserSession.profile.contactNumber) }
    var urgency by remember { mutableStateOf(UrgencyLevel.Normal) }
    var submitted by remember { mutableStateOf(false) }

    val nameOk = posterName.isNotBlank()
    val bloodOk = bloodType.isNotEmpty()
    val locationOk = location.isNotBlank()
    val contactOk = isValidPhNumber(contact)
    val allValid = nameOk && bloodOk && locationOk && contactOk

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Post a Blood Request", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Gray, modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(4.dp))
                Text("Let donors know what you need.", color = Gray, fontSize = 13.sp)

                Spacer(Modifier.height(16.dp))

                // Urgency selector
                FormLabel("Urgency Level")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrgencyLevel.entries.forEach { level ->
                        val selected = urgency == level
                        val color = if (level == UrgencyLevel.Urgent) UrgentRed else NormalGreen
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) color else color.copy(alpha = 0.1f))
                                .border(1.dp, if (selected) color else color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { urgency = level }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(level.emoji, fontSize = 20.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    level.label,
                                    color = if (selected) Color.White else color,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                FormLabel("Your Name")
                FormTextField(
                    value = posterName,
                    onValueChange = { posterName = it },
                    placeholder = "e.g. Maria Santos",
                    error = if (submitted && !nameOk) "Please enter your name" else null
                )

                FormLabel("Blood Type Needed")
                BloodTypeDropdown(
                    selected = bloodType,
                    onSelected = { bloodType = it },
                    error = if (submitted && !bloodOk) "Please select a blood type" else null
                )

                FormLabel("Location / Barangay")
                FormTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "e.g. Poblacion, Cebu City",
                    leadingIcon = Icons.Outlined.LocationOn,
                    error = if (submitted && !locationOk) "Please enter a location" else null
                )

                // Fills the location in from the device
                AutoLocationField(
                    location = location,
                    onLocationChange = { location = it },
                    onPicked = { },
                    autoStart = false
                )

                FormLabel("Hospital / Clinic (optional)")
                FormTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    placeholder = "e.g. Cebu General Hospital"
                )

                FormLabel("Contact Number")
                FormTextField(
                    value = contact,
                    onValueChange = { input -> contact = input.filter { it.isDigit() || it == '+' }.take(13) },
                    placeholder = "09XX XXX XXXX",
                    keyboardType = KeyboardType.Phone,
                    error = if (submitted && !contactOk) "Enter a valid number (09XXXXXXXXX)" else null
                )

                FormLabel("Additional Notes (optional)")
                FormTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "e.g. Needed for surgery, accident, etc.",
                    singleLine = false,
                    minLines = 3
                )

                Spacer(Modifier.height(8.dp))

                if (errorMessage != null) {
                    FormError(errorMessage, Modifier.padding(bottom = 8.dp))
                }

                FormButton(
                    text = if (isPosting) "Posting…" else "Post Request",
                    color = UrgentRed,
                    enabled = !isPosting,
                    onClick = {
                        submitted = true
                        if (allValid) {
                            onSubmit(
                                BloodRequest(
                                    posterName = posterName.trim(),
                                    bloodType = bloodType,
                                    location = location.trim(),
                                    hospital = hospital.trim(),
                                    notes = notes.trim(),
                                    urgency = urgency,
                                    contact = contact
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (active) Brand else Gray)
                Text(
                    tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Brand else Gray
                )
            }
        }
    }
}
