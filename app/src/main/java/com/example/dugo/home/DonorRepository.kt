package com.example.dugo.home

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Real donors, read from the `users` collection in Firestore.
 *
 * A donor only shows up on the map once they have saved a position, since
 * without coordinates there is nowhere to put their pin.
 */
object DonorRepository {

    private const val TAG = "DonorRepository"

    private val users
        get() = FirebaseFirestore.getInstance().collection("users")

    /**
     * Watches every signed-up donor that has a position saved.
     */
    fun listenDonors(
        onChange: (List<Donor>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        users
            .whereEqualTo("role", "Blood Donor")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Could not load donors", error)
                    onError(error.message ?: "Couldn't load donors")
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().mapNotNull(::toDonor))
            }

    /** The donors signed up so far, read once. */
    suspend fun loadDonors(): List<Donor> =
        try {
            users.whereEqualTo("role", "Blood Donor").get().await()
                .documents.mapNotNull(::toDonor)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load donors", e)
            emptyList()
        }

    private fun toDonor(doc: DocumentSnapshot): Donor? {
        val lat = doc.getDouble("latitude") ?: return null
        val lng = doc.getDouble("longitude") ?: return null
        val uid = doc.getString("uid") ?: doc.id
        return Donor(
            id = uid.hashCode(),
            name = doc.getString("fullName")?.takeIf { it.isNotBlank() } ?: "Donor",
            bloodType = doc.getString("bloodType")?.takeIf { it.isNotBlank() } ?: "—",
            availableToday = doc.getBoolean("availableToday") ?: true,
            area = doc.getString("location")?.substringBefore(',')?.trim().orEmpty(),
            phone = doc.getString("contactNumber").orEmpty(),
            lat = lat,
            lng = lng,
            uid = uid
        )
    }
}
