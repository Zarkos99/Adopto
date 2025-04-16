package sweng894.project.adopto.firebase

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import sweng894.project.adopto.data.Chat
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.Message
import java.time.Instant

object ChatState {
    var active_chat_id: String? = null
}

class ChatsListenerViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _unread_map = MutableLiveData<Map<String, Boolean>>()
    private var prev_unread_map: Map<String, Boolean> = emptyMap()
    val unread_map: LiveData<Map<String, Boolean>> = _unread_map

    private val _user_chats = MutableLiveData<List<Chat>>()
    private var prev_user_chats: List<Chat> = emptyList()
    val user_chats: LiveData<List<Chat>> = _user_chats


    private val _selected_chat_id = MutableLiveData<Chat?>()
    private var prev_selected_chat_id: Chat? = null
    val selected_chat_id: LiveData<Chat?> = _selected_chat_id
    private var current_active_chat_id: String? = null

    private var listener_registration: ListenerRegistration? = null

    fun startListening(user_id: String) {
        listener_registration?.remove()

        listener_registration = firestore.collection(FirebaseCollections.CHATS)
            .whereArrayContains(Chat::participant_ids.name, user_id)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _user_chats.postValue(emptyList())
                    prev_user_chats = emptyList()
                    _unread_map.postValue(emptyMap())
                    prev_unread_map = emptyMap()
                    return@addSnapshotListener
                }

                val chat_list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val chat = doc.toObject(Chat::class.java)
                        chat?.copy(chat_id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { chat ->
                    try {
                        Instant.parse(chat.last_updated)
                    } catch (e: Exception) {
                        Instant.EPOCH // fallback to a default past time
                    }
                }

                if (!areChatListsEqual(prev_user_chats, chat_list)) {
                    prev_user_chats = chat_list
                    _user_chats.postValue(chat_list)
                }

                val unread_status_map = mutableMapOf<String, Boolean>()
                val pending_checks = chat_list.size
                var completed_checks = 0

                if (pending_checks == 0) {
                    _unread_map.postValue(emptyMap())
                    prev_unread_map = emptyMap()
                    return@addSnapshotListener
                }

                fun maybePostUnreadMap() {
                    completed_checks++
                    if (completed_checks == pending_checks) {
                        if (unread_status_map != prev_unread_map) {
                            prev_unread_map = unread_status_map
                            _unread_map.postValue(unread_status_map)
                        }
                    }
                }

                chat_list.forEach { chat ->
                    firestore.collection(FirebaseCollections.CHATS)
                        .document(chat.chat_id)
                        .collection("Messages")
                        .orderBy(
                            Message::timestamp.name,
                            com.google.firebase.firestore.Query.Direction.DESCENDING
                        )
                        .limit(1)
                        .get()
                        .addOnSuccessListener { result ->
                            val last_message = result.documents.firstOrNull()
                            val last_message_ts = last_message?.getString(Message::timestamp.name)
                            val last_read_ts = chat.last_read_timestamps[user_id]

                            val sender_id = last_message?.getString(Message::sender_id.name)
                            Log.d(
                                "ChatsListenerViewModel",
                                "sender_id != user_id: ${sender_id != user_id}"
                            )
                            Log.d(
                                "ChatsListenerViewModel",
                                "chat.chat_id != current_active_chat_id: ${chat.chat_id != current_active_chat_id}"
                            )
                            Log.d(
                                "ChatsListenerViewModel",
                                "(last_read_ts == null || Instant.parse(last_message_ts.isAfter(Instant.parse(last_read_ts))): ${
                                    (last_read_ts == null || Instant.parse(last_message_ts)
                                        .isAfter(Instant.parse(last_read_ts)))
                                }"
                            )
                            val is_unread = last_message_ts != null &&
                                    sender_id != user_id && // Only unread if current user didn't send it
                                    chat.chat_id != current_active_chat_id && // Don't count as unread if it's the active chat
                                    (last_read_ts == null || Instant.parse(last_message_ts)
                                        .isAfter(Instant.parse(last_read_ts)))

                            unread_status_map[chat.chat_id] = is_unread
                            maybePostUnreadMap()
                        }
                        .addOnFailureListener {
                            unread_status_map[chat.chat_id] = false
                            maybePostUnreadMap()
                        }
                }
            }
    }

    private fun areChatListsEqual(old_list: List<Chat>, new_list: List<Chat>): Boolean {
        if (old_list.size != new_list.size) return false
        return old_list.zip(new_list).all { (old, new) -> old == new }
    }

    fun selectChat(chat: Chat?) {
        if (chat != prev_selected_chat_id) {
            prev_selected_chat_id = chat
            current_active_chat_id = chat?.chat_id
            _selected_chat_id.value = chat
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener_registration?.remove()
    }
}
