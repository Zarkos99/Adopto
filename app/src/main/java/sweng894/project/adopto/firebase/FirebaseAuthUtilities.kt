package sweng894.project.adopto.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User

fun getCurrentUserId(): String? {
    val user_id = Firebase.auth.currentUser?.uid
    if (user_id.isNullOrEmpty()) {
        var tag = "getCurrentUserId()"
        val trace = Throwable().stackTrace
        if (trace.size >= 3) {
            val prev = trace[2] // [0] = logPreviousScope, [1] = current function, [2] = previous
            val class_name = prev.className.substringAfterLast(".")
            val method_name = prev.methodName
            tag = "$class_name.$method_name()"
        }
        Log.e(
            tag,
            "No authenticated user found"
        )
    }
    return user_id
}

fun updateUserDisplayName(new_display_name: String) {
    val firebase_user = Firebase.auth.currentUser
    val user_id = getCurrentUserId()

    if (user_id.isNullOrEmpty()) {
        return
    }

    firebase_user?.updateProfile(userProfileChangeRequest {
        displayName = new_display_name
    })

    updateDataField(
        FirebaseCollections.USERS,
        user_id,
        User::display_name,
        new_display_name
    )
}