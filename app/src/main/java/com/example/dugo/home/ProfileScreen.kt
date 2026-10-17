package com.example.dugo.home

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dugo.login.Brand

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val BubbleMe = Color(0xFFEE1D2B)
private val Green = Color(0xFF2E9E6B)

@Composable
fun ProfileScreen(
    roleLabel: String = "Blood Donor",
    selectedTab: HomeTab = HomeTab.Profile,
    onTabSelected: (HomeTab) -> Unit = {},
    onNotifications: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val profile = UserSession.profile
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
                    .clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink)
            }
        }

        HorizontalDivider(color = Line)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Profile card
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF0F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Gray, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("@${profile.username}", fontSize = 14.sp, color = Gray)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Role badge
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BubbleMe.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                profile.role,
                                color = BubbleMe,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (profile.role == "Blood Donor") {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (profile.availableToday) Green.copy(alpha = 0.1f) else Gray.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (profile.availableToday) "Available" else "Unavailable",
                                    color = if (profile.availableToday) Green else Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7F8FA))
                        .border(1.dp, Line, CircleShape)
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = Ink, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats (for donors)
            if (profile.role == "Blood Donor") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F8FA))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = profile.totalDonations.toString(), label = "Donations")
                    StatItem(value = profile.bloodType, label = "Blood Type")
                    StatItem(value = profile.joinedDate, label = "Joined")
                }
                Spacer(Modifier.height(20.dp))
            }

            // Info items
            Text("Account Info", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gray, modifier = Modifier.padding(bottom = 8.dp))

            ProfileInfoRow(icon = Icons.Outlined.Favorite, label = "Blood Type", value = profile.bloodType)
            ProfileInfoRow(icon = Icons.Outlined.LocationOn, label = "Location", value = profile.location)
            ProfileInfoRow(icon = Icons.Outlined.Phone, label = "Contact", value = profile.contactNumber)

            Spacer(Modifier.height(20.dp))

            Text("Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gray, modifier = Modifier.padding(bottom = 8.dp))

            SettingsRow(icon = Icons.Outlined.Notifications, label = "Notifications", onClick = onNotifications)
            SettingsRow(icon = Icons.Outlined.Settings, label = "App Settings", onClick = { /* TODO */ })
            SettingsRow(icon = Icons.Outlined.Lock, label = "Privacy & Security", onClick = { /* TODO */ })
            SettingsRow(icon = Icons.Outlined.Info, label = "About DuGO", onClick = { /* TODO */ })
            SettingsRow(icon = Icons.Outlined.Refresh, label = "Donation History", onClick = { /* TODO */ })

            Spacer(Modifier.height(20.dp))

            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BubbleMe
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "DuGO v1.0.0",
                color = Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(color = Line)
        ProfileBottomNav(selectedTab, onTabSelected)
    }

    // Edit profile dialog placeholder
    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                UserSession.profile = updated
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, fontSize = 12.sp, color = Gray)
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
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
            Text(label, fontSize = 12.sp, color = Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProfileBottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
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

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var fullName by remember { mutableStateOf(profile.fullName) }
    var location by remember { mutableStateOf(profile.location) }
    var contact by remember { mutableStateOf(profile.contactNumber) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Column {
                Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)

                Spacer(Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(profile.copy(fullName = fullName, location = location, contactNumber = contact))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}