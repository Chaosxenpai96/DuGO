package com.example.dugo.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
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
    val contact: Donor,
    val messages: MutableList<Message>
) {
    val lastMessage: Message? get() = messages.lastOrNull()
    val unreadCount: Int get() = messages.count { !it.isRead }
}

val sampleConversations: MutableList<Conversation> = mutableListOf(
    Conversation(
        contact = sampleDonors[0], // Mia Santos
        messages = mutableListOf(
            Message(1, 0, 1, "Hi Mia! Are you available to donate today? I urgently need O+ blood.", sentAt = System.currentTimeMillis() - 30 * 60_000, isRead = true),
            Message(2, 1, 0, "Hi! Yes, I'm available. What hospital?", sentAt = System.currentTimeMillis() - 25 * 60_000, isRead = true),
            Message(3, 0, 1, "Dalaguete District Hospital. Can you come around 2 PM?", sentAt = System.currentTimeMillis() - 20 * 60_000, isRead = true),
            Message(4, 1, 0, "Sure, I'll be there. Please prepare the requirements.", sentAt = System.currentTimeMillis() - 5 * 60_000, isRead = false)
        )
    ),
    Conversation(
        contact = sampleDonors[2], // Carlo Reyes
        messages = mutableListOf(
            Message(5, 0, 3, "Hello Carlo, do you still have A+ blood available?", sentAt = System.currentTimeMillis() - 2 * 60 * 60_000, isRead = true),
            Message(6, 3, 0, "Hello! Yes, I can donate next week. When do you need it?", sentAt = System.currentTimeMillis() - 90 * 60_000, isRead = true)
        )
    ),
    Conversation(
        contact = sampleDonors[4], // Paolo Cruz
        messages = mutableListOf(
            Message(7, 4, 0, "Hi, I saw your blood request post. I'm B- and available anytime.", sentAt = System.currentTimeMillis() - 5 * 60 * 60_000, isRead = false)
        )
    )
)
