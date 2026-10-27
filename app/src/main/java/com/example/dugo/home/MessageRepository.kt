package com.example.dugo.home

import android.util.Log
import com.example.dugo.auth.AuthRepository
import com.google.firebase.firestore.DocumentReference
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

    /** Total unread messages across all of the signed-in user's chats. */
    fun listenUnreadTotal(onChange: (Int) -> Unit): ListenerRegistration =
        listenConversations(
            onChange = { list -> onChange(list.sumOf { it.unreadCount }) },
            onError = { onChange(0) }
        )

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

        // The set with merge will create the document if missing, or be a no-op if it exists.
        // Since participants() checks request.auth.uid in request.resource.data.participantIds,
        // and the merged data includes myUid, the write will pass the rules.
        val summary = mapOf(
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
            ),
            "unreadCounts" to emptyMap<String, Int>()
        )
        try {
            doc.set(summary, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            // If the set fails due to rules, the conversation may not be allowed.
            // A denied write is logged, but the app continues: the next sendMessage
            // will attempt another set with merge, and it might succeed then.
            Log.w(TAG, "Could not prepare the conversation", e)
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
    ): String {
        val myUid = AuthRepository.currentUser?.uid ?: return "Sign in to send a message."
        val myName = UserSession.profile.fullName
        val id = conversationId(myUid, recipientUid)
        val now = System.currentTimeMillis()

        // The conversation document goes first: the rules check that the sender
        // is one of its participants, so it has to exist before the message.
        val doc = conversations.document(id)
        try {
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
                "unreadCounts" to readUnreadCounts(doc) + (recipientUid to ((readUnreadCounts(doc)[recipientUid] ?: 0) + 1))
            )
            doc.set(summary, com.google.firebase.firestore.SetOptions.merge()).await()

            val message = mapOf(
                "senderUid" to myUid,
                "senderName" to myName,
                "text" to text.trim(),
                "sentAt" to now
            )
            doc.collection("messages").add(message).await()
            return ""
        } catch (e: Exception) {
            Log.w(TAG, "Could not send the message", e)
            return friendlyMessage(e)
        }
    }

    /** Marks a conversation as read by the signed-in user. */
    suspend fun markRead(conversationId: String) {
        val myUid = AuthRepository.currentUser?.uid ?: return
        val doc = conversations.document(conversationId)
        try {
            if (!doc.get().await().exists()) return
            val counts = readUnreadCounts(doc)
            if (counts[myUid] == null || counts[myUid] == 0) return
            doc.update("unreadCounts", counts + (myUid to 0)).await()
        } catch (e: Exception) {
            // Not being able to clear the badge shouldn't break the screen.
            Log.w(TAG, "Could not mark the conversation as read", e)
        }
    }

    /** The unread counts saved on a conversation, keyed by uid. */
    private suspend fun readUnreadCounts(doc: DocumentReference): Map<String, Int> =
        doc.get().await().get("unreadCounts")
            ?.let { it as? Map<*, *> }
            ?.entries
            ?.mapNotNull { (k, v) ->
                val count = (v as? Number)?.toInt() ?: return@mapNotNull null
                k.toString() to count
            }
            ?.toMap()
            ?: emptyMap()

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
        val unread = ((doc.get("unreadCounts") as? Map<*, *>)?.get(myUid) as? Number)?.toInt() ?: 0
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
        return Message(
            id = doc.id.hashCode(),
            senderUid = doc.getString("senderUid").orEmpty(),
            receiverUid = "",
            text = text,
            sentAt = doc.getLong("sentAt") ?: 0L,
            // The unread badge lives on the conversation, not on each message
            isRead = true
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