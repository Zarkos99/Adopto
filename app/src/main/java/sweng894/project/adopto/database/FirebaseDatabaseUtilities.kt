package sweng894.project.adopto.database

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
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
suspend fun getUserData(user_id: String): User? {
    return withTimeoutOrNull(10000) {  // 🔥 Timeout after 10 seconds
        try {
            val document = Firebase.firestore
                .collection(Strings.get(R.string.firebase_collection_users))
                .document(user_id)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            println("ERROR: Firestore query failed - ${e.message}")
            null
        }
    } ?: run {
        println("ERROR: Firestore query timed out after 5 seconds")
        null
    }
}

suspend fun getAnimalData(animal_id: String): Animal? {
    return withTimeoutOrNull(10000) {  // 🔥 Timeout after 10 seconds
        try {
            val document = Firebase.firestore
                .collection(Strings.get(R.string.firebase_collection_animals))
                .document(animal_id)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(Animal::class.java)
            } else {
                Log.w("getAnimalData", "Document does not exist, returning null.")
                null
            }
        } catch (e: Exception) {
            println("ERROR: Firestore query failed - ${e.message}")
            null
        }
    } ?: run {
        println("ERROR: Firestore query timed out or failed to find animal: $animal_id")
        null
    }
}

fun fetchAllUserAnimals(
    user_animal_ids: List<String>,
    on_complete: (List<Animal>) -> Unit
) {
    val firebase_database = Firebase.firestore
    val animal_list = mutableListOf<Animal>()

    if (user_animal_ids.isEmpty()) {
        on_complete(emptyList()) // No animals, return an empty list
        return
    }

    val batchRequests = user_animal_ids.map { animalId ->
        firebase_database.collection(Strings.get(R.string.firebase_collection_animals))
            .document(animalId)
            .get()
    }

    Tasks.whenAllSuccess<DocumentSnapshot>(batchRequests)
        .addOnSuccessListener { results ->
            results.forEach { document ->
                val animal = document.toObject(Animal::class.java)
                if (animal != null) {
                    animal_list.add(animal)
                }
            }
            on_complete(animal_list) // Return list of animals after all queries complete
        }
        .addOnFailureListener { exception ->
            Log.e("FirebaseDatabaseUtilities ERROR", "Failed to fetch animal data.", exception)
            on_complete(emptyList()) // Return empty list if an error occurs
        }
}

fun addUserToDatabase(new_user: User) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    if (new_user.user_id.isEmpty()) {
        new_user.user_id = user_id
    }

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_users)
    ).document(user_id).set(new_user)
        .addOnFailureListener {
            Log.w(
                "FirebaseDatabaseUtilities ERROR",
                "Failed to create document"
            )
        }
}

fun addAnimalToDatabase(
    new_animal: Animal
) {
    val firebase_database = Firebase.firestore

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_animals)
    ).document(new_animal.animal_id).set(new_animal).addOnSuccessListener {
        Log.d(
            "TRACE",
            "Successfully pushed animal to database. Adding animal_id to user's hosted_animal_ids"
        )
        appendToDataFieldArray(
            Strings.get(R.string.firebase_collection_users),
            getCurrentUserId(),
            User::hosted_animal_ids,
            new_animal.animal_id
        )
    }
        .addOnFailureListener {
            Log.w(
                "FirebaseDatabaseUtilities ERROR",
                "Failed to create document"
            )
        }
}

fun removeAnimalFromDatabase(
    animal: Animal
) {
    val firebase_database = Firebase.firestore

    val images_to_be_removed =
        mutableListOf(animal.profile_image_path).filterNotNull().toMutableList()
    images_to_be_removed.addAll(animal.supplementary_image_paths)
    //Remove all animal images from cloud storage
    deleteImagesFromCloudStorage(images_to_be_removed.toTypedArray())

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_animals)
    ).document(animal.animal_id).delete().addOnSuccessListener {
        Log.d(
            "TRACE",
            "Successfully deleted animal from database. Removing animal_id from user's hosted_animal_ids"
        )
        removeFromDataFieldArray(
            Strings.get(R.string.firebase_collection_animals),
            getCurrentUserId(),
            User::hosted_animal_ids,
            arrayOf(animal.animal_id)
        )
    }.addOnFailureListener {
        Log.e(
            "removeAnimalFromDatabase Error",
            "Failed to delete animal from database."
        )
    }
}

fun <T> updateDataField(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_value: Any,
    onUploadSuccess: (() -> Unit)? = null
) {
    val validCollections = setOf(
        Strings.get(R.string.firebase_collection_users),
        Strings.get(R.string.firebase_collection_animals)
    )

    if (collection !in validCollections) {
        throw IllegalArgumentException("Invalid collection: $collection")
    }

    val firebase_database = Firebase.firestore
    val document_ref =
        firebase_database.collection(collection).document(document_id)

    document_ref.set(mapOf(field_name.name to field_value), SetOptions.merge())
        .addOnFailureListener {
            Log.w(
                "FirebaseDatabaseUtilities ERROR",
                "Failed to update ${field_name.name} to $field_value"
            )
        }
        .addOnSuccessListener { onUploadSuccess?.invoke() }
}

fun <T> appendToDataFieldArray(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_value: Any,
    onUploadSuccess: (() -> Unit)? = null
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
            "FirebaseDatabaseUtilities ERROR",
            "Failed to update ${field_name.name} to $field_value"
        )
    }.addOnSuccessListener { onUploadSuccess?.invoke() }
}

/**
 * Removes items from array type data fields. If data fields are arrays of image paths, this will also delete the images on the cloud storage.
 */
fun <T> removeFromDataFieldArray(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    values_to_be_removed: Array<String>,
    onRemovalSuccess: (() -> Unit)? = null
) {
    val firebase_database = Firebase.firestore
    val document_ref = firebase_database.collection(collection).document(document_id)

    // Use Firestore's native arrayRemove operation to efficiently remove values
    document_ref.update(field_name.name, FieldValue.arrayRemove(*values_to_be_removed))
        .addOnSuccessListener {
            println("Successfully removed values from ${field_name.name}")
            _syncDatabaseForRemovedImages(field_name, values_to_be_removed)
        }.addOnFailureListener { println("Error updating field: ${it.message}") }
        .addOnSuccessListener { onRemovalSuccess?.invoke() }
}

fun <T> _syncDatabaseForRemovedImages(
    field_name: KProperty1<T, *>,
    values_to_be_removed: Array<String>
) {
    if (field_name.name.contains("image", ignoreCase = true)) {
        deleteImagesFromCloudStorage(values_to_be_removed)
    }
}


