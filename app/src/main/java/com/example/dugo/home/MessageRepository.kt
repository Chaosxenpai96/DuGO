package com.example.dugo.home

import android.util.Log
import com.example.dugo.auth.AuthRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Conversations and their messages, stored in Firestore.
 *
 * A conversation lives at `conversations/{id}`, where the id is the two users'
 * uids in a fixed order, so both people find the same conversation. Each message
 * is a document under `conversations/{id}/messages/{messageId}`.
 */
object MessageRepository {

    private const val TAG = "MessageRepository"

    private val db
        get() = FirebaseFirestore.getInstance()

    private val conversations
        get() = db.collection("conversations")

    /** The conversation id for two users, the same for both of them. */
    fun conversationId(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    /**
     * Watches the signed-in user's conversations. Each one carries the other
     * person's name, blood type and availability, so the list can be drawn.
     */
    fun listenConversations(
        onChange: (List<Conversation>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        conversations
            .whereArrayContains("participantIds", AuthRepository.currentUser?.uid ?: "")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Could not load conversations", error)
                    onError(friendlyMessage(error))
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().mapNotNull(::toConversation))
            }

    /** Watches the messages in one conversation, newest last. */
    fun listenMessages(
        conversationId: String,
        onChange: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        conversations
            .document(conversationId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Could not load messages", error)
                    onError(friendlyMessage(error))
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().mapNotNull(::toMessage))
            }

    /**
     * Makes sure a conversation with the other person exists and returns its id.
     * Used when opening a chat from a donor card, before any message is sent.
     */
    suspend fun ensureConversation(
        recipientUid: String,
        recipientName: String,
        recipientBloodType: String,
        recipientAvailable: Boolean
    ): String {
        val myUid = AuthRepository.currentUser?.uid ?: return ""
        val id = conversationId(myUid, recipientUid)
        val doc = conversations.document(id)
        if (!doc.get().await().exists()) {
            doc.set(
                mapOf(
                    "participantIds" to listOf(myUid, recipientUid).sorted(),
                    "participantNames" to mapOf(
                        myUid to UserSession.profile.fullName,
                        recipientUid to recipientName
                    ),
                    "participantBloodTypes" to mapOf(
                        myUid to UserSession.profile.bloodType,
                        recipientUid to recipientBloodType
                    ),
                    "participantAvailable" to mapOf(
                        myUid to UserSession.profile.availableToday,
                        recipientUid to recipientAvailable
                    )
                )
            ).await()
        }
        return id
    }

    /**
     * Sends a message to [recipientUid]. The conversation document is created on
     * the first message, with the details of the other person so the list can
     * show them.
     */
    suspend fun sendMessage(
        recipientUid: String,
        recipientName: String,
        recipientBloodType: String,
        recipientAvailable: Boolean,
        text: String
    ) {
        val myUid = AuthRepository.currentUser?.uid ?: return
        val myName = UserSession.profile.fullName
        val id = conversationId(myUid, recipientUid)
        val now = System.currentTimeMillis()

        val message = mapOf(
            "senderUid" to myUid,
            "senderName" to myName,
            "text" to text.trim(),
            "sentAt" to now
        )
        conversations.document(id).collection("messages").add(message).await()

        val summary = mapOf(
            "participantIds" to listOf(myUid, recipientUid).sorted(),
            "participantNames" to mapOf(myUid to myName, recipientUid to recipientName),
            "participantBloodTypes" to mapOf(
                myUid to UserSession.profile.bloodType,
                recipientUid to recipientBloodType
            ),
            "participantAvailable" to mapOf(myUid to UserSession.profile.availableToday, recipientUid to recipientAvailable),
            "lastMessageAt" to now,
            "lastMessageText" to text.trim(),
            "unreadFor" to com.google.firebase.firestore.FieldValue.arrayUnion(recipientUid)
        )
        conversations.document(id).set(summary, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /** Marks a conversation as read by the signed-in user. */
    suspend fun markRead(conversationId: String) {
        val myUid = AuthRepository.currentUser?.uid ?: return
        conversations.document(conversationId)
            .update("unreadFor", com.google.firebase.firestore.FieldValue.arrayRemove(myUid))
            .await()
    }

    private fun toConversation(doc: DocumentSnapshot): Conversation? {
        val names = doc.get("participantNames") as? Map<*, *>
            ?: return null
        val bloodTypes = doc.get("participantBloodTypes") as? Map<*, *>
        val available = doc.get("participantAvailable") as? Map<*, *>
        val myUid = AuthRepository.currentUser?.uid ?: return null
        val otherUid = (doc.get("participantIds") as? List<*>)
            ?.map { it.toString() }
            ?.firstOrNull { it != myUid }
            ?: return null
        val otherName = names[otherUid]?.toString().orEmpty().ifBlank { "Donor" }
        val otherBloodType = bloodTypes?.get(otherUid)?.toString().orEmpty().ifBlank { "—" }
        val otherAvailable = available?.get(otherUid) as? Boolean ?: true
        val lastText = doc.getString("lastMessageText").orEmpty()
        val lastAt = doc.getLong("lastMessageAt") ?: 0L
        val unread = (doc.get("unreadFor") as? List<*>).orEmpty().count { it.toString() == myUid }
        return Conversation(
            id = doc.id,
            contact = Donor(
                id = 0,
                name = otherName,
                bloodType = otherBloodType,
                availableToday = otherAvailable,
                area = "",
                phone = "",
                lat = 0.0,
                lng = 0.0,
                uid = otherUid
            ),
            lastText = lastText,
            lastAt = lastAt,
            unreadCount = unread
        )
    }

    private fun toMessage(doc: DocumentSnapshot): Message? {
        val text = doc.getString("text") ?: return null
        val myUid = AuthRepository.currentUser?.uid
        val readBy = doc.get("readBy") as? List<*> ?: emptyList<Any>()
        return Message(
            id = doc.id.hashCode(),
            senderUid = doc.getString("senderUid").orEmpty(),
            receiverUid = "",
            text = text,
            sentAt = doc.getLong("sentAt") ?: 0L,
            isRead = myUid == null || readBy.contains(myUid)
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
                "Sign in again to see your messages."
            else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        }
}
