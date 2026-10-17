package com.example.dugo.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UrgencyLevel(val label: String, val emoji: String) {
    Urgent("Urgent", "🔴"),
    Normal("Normal", "🟢")
}

data class BloodRequest(
    val id: Int,
    val posterName: String,
    val bloodType: String,
    val location: String,
    val hospital: String,
    val notes: String,
    val urgency: UrgencyLevel,
    val contact: String,
    val postedAt: Long = System.currentTimeMillis()
) {
    fun timeAgo(): String {
        val diff = System.currentTimeMillis() - postedAt
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    fun formattedDate(): String =
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(postedAt))
}

/** Sample requests shown before any real backend is connected. */
val sampleRequests = mutableListOf(
    BloodRequest(
        id = 1,
        posterName = "Ana Reyes",
        bloodType = "O+",
        location = "Poblacion, Dalaguete",
        hospital = "Dalaguete District Hospital",
        notes = "Needed for scheduled surgery on Monday morning.",
        urgency = UrgencyLevel.Urgent,
        contact = "09171234567",
        postedAt = System.currentTimeMillis() - 15 * 60_000
    ),
    BloodRequest(
        id = 2,
        posterName = "Rico Tano",
        bloodType = "A+",
        location = "Consolacion, Dalaguete",
        hospital = "Private clinic",
        notes = "For a family member recovering from an operation.",
        urgency = UrgencyLevel.Normal,
        contact = "09281234567",
        postedAt = System.currentTimeMillis() - 2 * 60 * 60_000
    ),
    BloodRequest(
        id = 3,
        posterName = "Lhen Cruz",
        bloodType = "B-",
        location = "Coro, Dalaguete",
        hospital = "Cebu South Medical Center",
        notes = "Emergency — accident victim, please respond ASAP.",
        urgency = UrgencyLevel.Urgent,
        contact = "09391234567",
        postedAt = System.currentTimeMillis() - 5 * 60 * 60_000
    )
)
