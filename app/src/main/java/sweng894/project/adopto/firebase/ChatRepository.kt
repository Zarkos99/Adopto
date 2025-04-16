package sweng894.project.adopto.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import sweng894.project.adopto.data.Chat
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.Message
import java.time.Instant
import java.time.format.DateTimeFormatter

object ChatRepository {
    private const val TAG = "ChatRepository"
    private const val READ_DEBOUNCE_INTERVAL_MS = 3000L // 3 seconds
    private val last_read_updates = mutableMapOf<String, Long>()

    fun createOrGetChat(
        participant_ids: List<String>,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val chats_ref = db.collection(FirebaseCollections.CHATS)

        chats_ref
            .whereEqualTo(Chat::participant_ids.name, participant_ids.sorted())
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val chat_id = result.documents.first().id
                    onComplete(chat_id)
                } else {
                    val new_chat = Chat(participant_ids = participant_ids)
                    val chat_doc = chats_ref.document()
                    chat_doc.set(new_chat.copy(chat_id = chat_doc.id))
                        .addOnSuccessListener {
                            onComplete(chat_doc.id)
                        }
                        .addOnFailureListener { onError(it) }
                }
            }
            .addOnFailureListener { onError(it) }
    }

    fun sendMessage(
        chat_id: String,
        sender_id: String,
        receiver_id: String,
        content: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val chat_ref = db.collection(FirebaseCollections.CHATS).document(chat_id)
        val messages_ref = chat_ref.collection("Messages")

        // Set max allowed messages
        val MAX_MESSAGES = 200

        // Create the message data
        val new_message = mapOf(
            Message::sender_id.name to sender_id,
            Message::receiver_id.name to receiver_id,
            Message::content.name to content,
            Message::timestamp.name to Instant.now().toString()
        )

        // Add new message first
        messages_ref.add(new_message).addOnSuccessListener {
            // Update chat last_updated time
            chat_ref.update(
                "last_updated",
                DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            ).addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener { onError() }

            // Clean up old messages (as a side task)
            messages_ref
                .orderBy(Message::timestamp.name, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.size() > MAX_MESSAGES) {
                        val to_delete = snapshot.documents.take(snapshot.size() - MAX_MESSAGES)
                        for (doc in to_delete) {
                            doc.reference.delete()
                        }
                    }
                }
        }.addOnFailureListener { onError() }
    }

    fun getMessages(
        chat_id: String,
        onResult: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection(FirebaseCollections.CHATS)
            .document(chat_id)
            .collection("Messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { query_snapshot ->
                val messages = query_snapshot.documents.mapNotNull { it.toObject<Message>() }
                onResult(messages)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getUserChats(
        user_id: String,
        onResult: (List<Chat>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection(FirebaseCollections.CHATS)
            .whereArrayContains(Chat::participant_ids.name, user_id)
            .get()
            .addOnSuccessListener { result ->
                val chats = result.documents.mapNotNull { it.toObject<Chat>() }
                onResult(chats)
            }
            .addOnFailureListener { onError(it) }
    }

    fun markChatAsReadDebounced(chat_id: String, user_id: String) {
        val now = System.currentTimeMillis()
        val key = "$chat_id-$user_id"
        val last = last_read_updates[key] ?: 0L

        if (now - last >= READ_DEBOUNCE_INTERVAL_MS) {
            last_read_updates[key] = now
            markChatAsRead(chat_id, user_id)
        }
    }

    fun markChatAsRead(chat_id: String, user_id: String, onSuccess: (() -> Unit)? = null) {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        FirebaseFirestore.getInstance()
            .collection(FirebaseCollections.CHATS)
            .document(chat_id)
            .update(Chat::last_read_timestamps.name + ".$user_id", now)
            .addOnSuccessListener { onSuccess?.invoke() }
    }
}