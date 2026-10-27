package com.example.dugo

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.dugo.home.HomeScreenMap
import com.example.dugo.home.HomeTab
import com.example.dugo.home.sampleDonors          // [MAP] ADDED
import com.example.dugo.login.LoginScreen
import com.example.dugo.login.RegisterDonor
import com.example.dugo.login.RegisterSeeker
import com.example.dugo.login.RoleSelectScreen
import com.example.dugo.profile.ProfileScreen          // [PROFILE] ADDED
import com.example.dugo.profile.ProfileStore           // [PROFILE] ADDED
import com.example.dugo.profile.UserProfile            // [PROFILE] ADDED
import com.example.dugo.profile.UserRole               // [MAP] ADDED
import com.example.dugo.profile.sampleProfile          // [PROFILE] ADDED
import com.example.dugo.profile.toProfile              // [PROFILE] ADDED
import com.example.dugo.ui.theme.DuGOTheme

// [PROFILE] CHANGED: added Profile at the end
enum class Screen { Login, RoleSelect, RegisterDonor, RegisterSeeker, Home, Profile }

// Testing shortcut: only works in debug builds. Remove before releasing the app.
private const val ADMIN_USER = "admin"
private const val ADMIN_PASSWORD = "admin123"

class MainActivity : ComponentActivity() {

    private val isDebugBuild: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuGOTheme {
                var screen by remember { mutableStateOf(Screen.Login) }
                var roleLabel by remember { mutableStateOf("Blood Donor") }

                // [PROFILE] ADDED: the saved profile (loads from the phone, falls back to demo data)
                val store = remember { ProfileStore(applicationContext) }
                var profile by remember { mutableStateOf<UserProfile>(store.load() ?: sampleProfile()) }
                fun updateProfile(updated: UserProfile) {
                    profile = updated
                    store.save(updated)
                    // TODO: also send `updated` to your backend / Firebase here
                }

                // [MAP] ADDED: donor list. The logged-in donor's own entry follows their profile settings
                // (blood type, "Show me on the donor map", availability pill).
                // TODO: replace with donors loaded from your backend; match by user id instead of name.
                val mapDonors = remember(profile) {
                    sampleDonors.map { d ->
                        if (profile.role == UserRole.Donor && d.name == profile.fullName) {
                            d.copy(
                                bloodType = profile.bloodType,
                                phone = profile.phone,
                                showOnMap = profile.showOnMap,
                                availableToDonate = profile.availableToDonate
                            )
                        } else d
                    }
                }

                // Back button: register -> role select -> login, Profile -> Home (Login and Home close the app)
                BackHandler(enabled = screen != Screen.Login && screen != Screen.Home) {
                    screen = when (screen) {
                        Screen.RegisterDonor, Screen.RegisterSeeker -> Screen.RoleSelect
                        Screen.Profile -> Screen.Home       // [PROFILE] ADDED
                        else -> Screen.Login
                    }
                }

                when (screen) {
                    Screen.Login -> LoginScreen(
                        onLogin = { user, password ->
                            if (isDebugBuild && user == ADMIN_USER && password == ADMIN_PASSWORD) {
                                roleLabel = "Admin"
                                screen = Screen.Home
                            } else {
                                // TODO: check the credentials with your backend
                                Toast.makeText(
                                    this,
                                    "Login isn't connected yet. For testing use admin / admin123",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onGoogle = { Toast.makeText(this, "Google sign-in", Toast.LENGTH_SHORT).show() },
                        onForgotPassword = { Toast.makeText(this, "Forgot password", Toast.LENGTH_SHORT).show() },
                        onSignUp = { screen = Screen.RoleSelect }
                    )

                    Screen.RoleSelect -> RoleSelectScreen(
                        onNeedBlood = { screen = Screen.RegisterSeeker },
                        onWantDonate = { screen = Screen.RegisterDonor },
                        onLogin = { screen = Screen.Login }
                    )

                    Screen.RegisterDonor -> RegisterDonor(
                        onBack = { screen = Screen.RoleSelect },
                        onSubmit = { form ->
                            // TODO: save the form to your backend / Firebase here
                            updateProfile(form.toProfile())     // [PROFILE] ADDED
                            roleLabel = "Blood Donor"
                            screen = Screen.Home
                        }
                    )

                    Screen.RegisterSeeker -> RegisterSeeker(
                        onBack = { screen = Screen.RoleSelect },
                        onSubmit = { form ->
                            // TODO: save the form to your backend / Firebase here
                            updateProfile(form.toProfile())     // [PROFILE] ADDED
                            roleLabel = "Blood Seeker"
                            screen = Screen.Home
                        }
                    )

                    Screen.Home -> HomeScreenMap(
                        roleLabel = roleLabel,
                        donors = mapDonors,                                              // [MAP] ADDED
                        priorityBloodType = if (profile.role == UserRole.Seeker) profile.bloodType else null, // [MAP] ADDED
                        searchRadiusKm = profile.searchRadiusKm,                         // [MAP] ADDED
                        onRadiusChange = { updateProfile(profile.copy(searchRadiusKm = it)) }, // [MAP] ADDED
                        onTabSelected = { tab ->
                            // [PROFILE] CHANGED: Profile tab now opens the profile screen
                            when (tab) {
                                HomeTab.Map -> Unit
                                HomeTab.Profile -> screen = Screen.Profile
                                else -> Toast.makeText(this, "${tab.label} coming soon", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNotifications = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                        onProfile = { screen = Screen.Profile },   // [PROFILE] CHANGED: was a "coming soon" toast
                        onMessageDonor = { donor ->
                            Toast.makeText(this, "Messaging ${donor.name} coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // [PROFILE] ADDED: the whole Profile screen branch
                    Screen.Profile -> ProfileScreen(
                        profile = profile,
                        roleLabel = roleLabel,
                        store = store,
                        onProfileChange = { updateProfile(it) },
                        onTabSelected = { tab ->
                            when (tab) {
                                HomeTab.Map -> screen = Screen.Home
                                HomeTab.Profile -> Unit
                                else -> Toast.makeText(this, "${tab.label} coming soon", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNotifications = { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                        onLogout = { screen = Screen.Login },
                        onDeleteAccount = {
                            store.clear()
                            profile = sampleProfile()
                            screen = Screen.Login
                            // TODO: delete the account on your backend too
                        }
                    )
                }
            }
        }
    }
}
