package com.example.dugo.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UrgencyLevel(val label: String, val emoji: String) {
    Urgent("Urgent", "🔴"),
    Normal("Normal", "🟢")
}

data class BloodRequest(
    val id: String = "",        // Firestore document ID; empty until saved
    val posterUid: String = "",
    val posterName: String,
    val bloodType: String,
    val location: String,
    val hospital: String,
    val notes: String,
    val urgency: UrgencyLevel,
    val contact: String,
    val postedAt: Long = System.currentTimeMillis(),
    val fulfilled: Boolean = false   // set by the poster with "Mark as Done"
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
