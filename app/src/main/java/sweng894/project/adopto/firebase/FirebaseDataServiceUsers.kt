package sweng894.project.adopto.firebase

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User

/**
 * A centralized data interface for Firebase User Data which enables listening to firebase data
 * changes and propagating those to interested activities
 */
class FirebaseDataServiceUsers : Service() {
    private val m_firebase_database = Firebase.firestore
    private val binder = LocalBinder() // Binder given to clients.
    private var m_user_data_update_callbacks = ArrayList<(() -> Unit)>()

    var current_user_data: User? = null

    /**
     * Adds the capability to register a behavioral callback for when the firebase user
     * data snapshot receives an update
     */
    fun registerCallback(callback: (() -> Unit)) {
        m_user_data_update_callbacks.add(callback)
    }

    /**
     * Execute all callbacks
     */
    fun callCallbacks() {
        for (callback in m_user_data_update_callbacks) {
            callback()
        }
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): FirebaseDataServiceUsers = this@FirebaseDataServiceUsers
    }

    override fun onBind(intent: Intent): IBinder {
        val user_id = getCurrentUserId()

        if (!user_id.isNullOrEmpty()) {
            val user_ref =
                m_firebase_database.collection(
                    FirebaseCollections.USERS
                ).document(user_id)

            user_ref.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firebase Database", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("DEBUG", "User data snapshot exists")
                    current_user_data = snapshot.toObject<User>()
                    callCallbacks()
                } else {
                    Log.d("Firebase Database", "Snapshot listener data: null")
                }
            }
        }

        return binder
    }
}