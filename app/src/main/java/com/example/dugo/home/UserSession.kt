package com.example.dugo.home

/**
 * Holds the current user's profile data in memory.
 * Replace with a real backend / auth system before releasing.
 */
data class UserProfile(
    val fullName: String,
    val username: String,
    val bloodType: String,
    val location: String,
    val contactNumber: String,
    val role: String,           // "Blood Donor" | "Blood Seeker" | "Admin"
    val availableToday: Boolean = true,
    val totalDonations: Int = 0,
    val joinedDate: String = "October 2026",
    // Set from the device's GPS when the donor opens the map — never geocoded.
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** Singleton session — set this when the user logs in or registers. */
object UserSession {
    var profile: UserProfile = UserProfile(
        fullName = "",
        username = "",
        bloodType = "",
        location = "",
        contactNumber = "",
        role = "Blood Donor",
        availableToday = true,
        totalDonations = 0,
        joinedDate = ""
    )
}
