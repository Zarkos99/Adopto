package sweng894.project.adopto.database

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.Message

class MessagesListenerViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private var listener_registration: ListenerRegistration? = null

    private val _messages = MutableLiveData<List<Message>>()
    private var prev_messages: List<Message> = emptyList()
    val messages: LiveData<List<Message>> = _messages

    fun listenToMessages(chat_id: String) {
        listener_registration?.remove()

        listener_registration = firestore.collection(FirebaseCollections.CHATS)
            .document(chat_id)
            .collection("Messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _messages.postValue(emptyList())
                    prev_messages = emptyList()
                    return@addSnapshotListener
                }

                val new_messages = snapshot.documents.mapNotNull {
                    it.toObject(Message::class.java)
                }

                if (new_messages != prev_messages) {
                    prev_messages = new_messages
                    _messages.postValue(new_messages)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listener_registration?.remove()
    }
}
