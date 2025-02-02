package sweng894.project.adopto.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.suspendCancellableCoroutine
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KProperty1

fun getCurrentUserId(): String {
    var user_id: String? = ""
    val firebase_user = Firebase.auth.currentUser
    firebase_user?.let {
        user_id = it.uid
    }

    return user_id.toString()
}

/**
 * Get user data synchronously. Will lock main thread here to wait for data to return.
 */
suspend fun getUserData(
    user_id: String,
): User? {
    val firebase_database = Firebase.firestore

    return suspendCancellableCoroutine { continuation ->
        firebase_database.collection(Strings.get(R.string.firebase_collection_users))
            .document(user_id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    continuation.resume(document.toObject(User::class.java)) // Return user data
                } else {
                    continuation.resume(null) // Document not found
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception) // Handle errors
            }
    }
}

fun addUserToDatabase(is_shelter: Boolean = false) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val new_user = User(user_id = user_id)
    new_user.is_shelter = is_shelter

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_users)
    ).document(user_id).set(new_user)
}

fun addAnimalToDatabase(
    firebaseDataService: FirebaseDataService,
    new_animal: Animal
) {
    val firebase_database = Firebase.firestore
    val current_user = firebaseDataService.current_user_data

    if (!current_user?.is_shelter!!) {
        Log.e("FIREBASE", "Cannot add new animal to database. User is not a shelter")
        return
    }

    //Ensure shelter id is linked
    new_animal.associated_shelter_id = current_user.user_id

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_animals)
    ).document(new_animal.animal_id.toString()).set(new_animal)
}

fun <T> updateDataField(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_value: Any
) {
    if (collection != Strings.get(R.string.firebase_collection_users) && collection != Strings.get(R.string.firebase_collection_animals)) {
        Log.e("DEVELOPMENT BUG", "Not a valid collection input.")
        return
    }
    val firebase_database = Firebase.firestore
    val document_ref =
        firebase_database.collection(collection).document(document_id)

    document_ref.update(field_name.name, field_value).addOnFailureListener {
        Log.w(
            "FirebaseDatabaseUtilities ERROR",
            "Failed to update ${field_name.name} to $field_value"
        )
    }
}

fun <T> appendToDataFieldArray(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_value: Any
) {
    if (collection != Strings.get(R.string.firebase_collection_users) && collection != Strings.get(R.string.firebase_collection_animals)) {
        Log.e("DEVELOPMENT BUG", "Not a valid collection input.")
        return
    }

    val firebase_database = Firebase.firestore
    val document_ref =
        firebase_database.collection(collection).document(document_id)

    document_ref.update(field_name.name, FieldValue.arrayUnion(field_value)).addOnFailureListener {
        Log.w(
            "appendToDataFieldArray Error",
            "Failed to update ${field_name.name} to $field_value"
        )
    }
}

/**
 * Removes items from array type data fields. If data fields are arrays of image paths, this will also delete the images on the cloud storage.
 */
inline fun <reified T> removeFromDataFieldArray(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    values_to_be_removed: Array<String>
) {
    val firebase_database = Firebase.firestore
    val document_ref = firebase_database.collection(collection).document(document_id)

    // Use Firestore's native arrayRemove operation to efficiently remove values
    document_ref.update(field_name.name, FieldValue.arrayRemove(*values_to_be_removed))
        .addOnSuccessListener {
            println("Successfully removed values from ${field_name.name}")
            _syncDatabaseForRemovedImages(field_name, values_to_be_removed)
        }
        .addOnFailureListener { println("Error updating field: ${it.message}") }
}

fun <T> _syncDatabaseForRemovedImages(
    field_name: KProperty1<T, *>,
    values_to_be_removed: Array<String>
) {
    if (field_name.name.contains("image", ignoreCase = true)) {
        deleteImagesFromCloudStorage(values_to_be_removed)
    }
}


