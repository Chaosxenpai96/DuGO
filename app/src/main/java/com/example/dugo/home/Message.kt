package com.example.dugo.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val id: Int,
    /** The uid of the sender. Empty for the signed-in user. */
    val senderUid: String = "",
    /** The uid of the receiver. */
    val receiverUid: String = "",
    val text: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun timeAgo(): String {
        val diff = System.currentTimeMillis() - sentAt
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

    fun formattedTime(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sentAt))
}

data class Conversation(
    /** The Firestore document id of the conversation. */
    val id: String = "",
    val contact: Donor,
    val messages: MutableList<Message> = mutableListOf(),
    /** The last message preview shown in the conversation list. */
    val lastText: String = "",
    val lastAt: Long = 0L,
    /** How many messages the signed-in user has not opened yet. */
    val unreadCount: Int = 0
)