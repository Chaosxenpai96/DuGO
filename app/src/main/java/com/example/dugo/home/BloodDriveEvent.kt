package com.example.dugo.home

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A blood donation drive shown in the featured banner on the Requests screen. */
data class BloodDriveEvent(
    val id: String = "",
    val title: String,
    val date: Long,            // start of the drive, millis since epoch
    val location: String,
    val description: String = ""
) {
    fun formattedDate(): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date))

    /** True when the drive is on today's date. */
    fun isToday(): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = date }
        val b = Calendar.getInstance()
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Reads blood drives from the Firestore `events` collection. Drives are added by
 * hand in the Firebase console (see firestore.rules: the app can only read them).
 */
object EventRepository {

    private const val TAG = "EventRepository"

    /**
     * Calls [onChange] with the next drive that is today or later, or null when
     * there isn't one. Remove the returned registration when the screen closes.
     */
    fun listenNextDrive(onChange: (BloodDriveEvent?) -> Unit): ListenerRegistration {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return FirebaseFirestore.getInstance().collection("events")
            .whereGreaterThanOrEqualTo("date", startOfToday)
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // The banner is optional, so a failure just hides it.
                    Log.w(TAG, "Could not load blood drives", error)
                    onChange(null)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().firstNotNullOfOrNull(::toEvent))
            }
    }

    private fun toEvent(doc: DocumentSnapshot): BloodDriveEvent? {
        val title = doc.getString("title") ?: return null
        val date = doc.getLong("date") ?: return null
        return BloodDriveEvent(
            id = doc.id,
            title = title,
            date = date,
            location = doc.getString("location").orEmpty(),
            description = doc.getString("description").orEmpty()
        )
    }
}
