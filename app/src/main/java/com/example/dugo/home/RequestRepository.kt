package com.example.dugo.home

import android.util.Log
import com.example.dugo.auth.AuthRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Result of posting a request. */
sealed class PostResult {
    data object Posted : PostResult()
    /** Saved on the phone; Firestore uploads it once the connection is back. */
    data object Queued : PostResult()
    data class Failed(val message: String) : PostResult()
}

/** Keeps blood requests in the Firestore `requests` collection, shared by every user. */
object RequestRepository {

    private const val TAG = "RequestRepository"

    /** How long to wait for the server before treating a post as queued offline. */
    private const val POST_TIMEOUT_MS = 10_000L

    private val requests
        get() = FirebaseFirestore.getInstance().collection("requests")

    /**
     * Calls [onChange] with the newest requests now and again whenever one is
     * added or removed. Remove the returned registration when the screen closes.
     */
    fun listen(
        onChange: (List<BloodRequest>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        requests
            .orderBy("postedAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Could not load requests", error)
                    onError(friendlyMessage(error))
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().mapNotNull(::toRequest))
            }

    /** Saves [request] as posted by the signed-in user. */
    suspend fun post(request: BloodRequest): PostResult {
        val uid = AuthRepository.currentUser?.uid
            ?: return PostResult.Failed("Sign in to post a request.")
        val data = mapOf(
            "posterUid" to uid,
            "posterName" to request.posterName,
            "bloodType" to request.bloodType,
            "location" to request.location,
            "hospital" to request.hospital,
            "notes" to request.notes,
            "urgency" to request.urgency.name,
            "contact" to request.contact,
            "postedAt" to request.postedAt
        )
        return try {
            // Offline, the write never completes until reconnecting, but it is kept
            // locally and uploaded later, so a timeout means queued rather than lost.
            val saved = withTimeoutOrNull(POST_TIMEOUT_MS) { requests.add(data).await() }
            if (saved != null) PostResult.Posted else PostResult.Queued
        } catch (e: Exception) {
            Log.w(TAG, "Could not post the request", e)
            PostResult.Failed(friendlyMessage(e))
        }
    }

    /**
     * Marks a request as fulfilled. Only the poster may do this (see firestore.rules).
     * Returns null on success, or a message to show the user.
     */
    suspend fun markFulfilled(requestId: String): String? =
        try {
            // Offline, the update is kept locally and uploaded later, so a timeout is fine.
            withTimeoutOrNull(POST_TIMEOUT_MS) {
                requests.document(requestId)
                    .update(
                        mapOf(
                            "fulfilled" to true,
                            "fulfilledAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not mark the request as fulfilled", e)
            friendlyMessage(e)
        }

    private fun toRequest(doc: DocumentSnapshot): BloodRequest? {
        val posterName = doc.getString("posterName") ?: return null
        val bloodType = doc.getString("bloodType") ?: return null
        return BloodRequest(
            id = doc.id,
            posterUid = doc.getString("posterUid").orEmpty(),
            posterName = posterName,
            bloodType = bloodType,
            location = doc.getString("location").orEmpty(),
            hospital = doc.getString("hospital").orEmpty(),
            notes = doc.getString("notes").orEmpty(),
            urgency = UrgencyLevel.entries.find { it.name == doc.getString("urgency") }
                ?: UrgencyLevel.Normal,
            contact = doc.getString("contact").orEmpty(),
            postedAt = doc.getLong("postedAt") ?: 0L,
            fulfilled = doc.getBoolean("fulfilled") ?: false
        )
    }

    private fun friendlyMessage(e: Exception): String =
        when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "The database rules don't allow this. Check the Firestore rules."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "The Firestore database hasn't been created yet."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "No internet connection. Check your network and try again."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Sign in again to see requests."
            else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        }
}
