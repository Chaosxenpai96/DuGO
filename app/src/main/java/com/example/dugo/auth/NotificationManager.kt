package com.example.dugo.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dugo.home.AppSettings
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Bridges the notification preferences stored on the device to Firebase Cloud
 * Messaging topic subscriptions.
 *
 * Each toggle maps to a topic, so a server can send a message to everyone who
 * opted into that kind of alert. The preference flags stay the source of truth
 * locally; this class keeps the server-side subscriptions in step.
 */
object NotificationManager {

    /** Topic names a message sender can target. */
    const val TOPIC_REQUESTS = "blood_requests"
    const val TOPIC_URGENT = "urgent_requests"
    const val TOPIC_MESSAGES = "messages"
    const val TOPIC_REMINDERS = "reminders"

    /** True when the OS-level notification permission has been granted. */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when the user has notifications switched on in the app. */
    fun isEnabled(context: Context): Boolean =
        AppSettings.prefs(context).getBoolean(AppSettings.NOTIF_MASTER, true)

    /**
     * Subscribes to or unsubscribes from the topics that match the current
     * preferences. Safe to call whenever a toggle changes.
     */
    fun syncTopics(context: Context) {
        val prefs = AppSettings.prefs(context)
        val master = prefs.getBoolean(AppSettings.NOTIF_MASTER, true)

        subscribe(TOPIC_REQUESTS, master && prefs.getBoolean(AppSettings.NOTIF_REQUESTS, true))
        subscribe(TOPIC_URGENT, master && prefs.getBoolean(AppSettings.NOTIF_URGENT, true))
        subscribe(TOPIC_MESSAGES, master && prefs.getBoolean(AppSettings.NOTIF_MESSAGES, true))
        subscribe(TOPIC_REMINDERS, master && prefs.getBoolean(AppSettings.NOTIF_REMINDERS, false))
    }

    private fun subscribe(topic: String, subscribe: Boolean) {
        runCatching {
            if (subscribe) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            }
        }
    }

    /** The FCM registration token, used when a backend needs to target this device. */
    fun currentToken(onToken: (String) -> Unit) {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> if (!token.isNullOrBlank()) onToken(token) }
        }
    }
}
