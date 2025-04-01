package sweng894.project.adopto.database

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import sweng894.project.adopto.data.VectorUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KProperty1

fun getCurrentUserId(): String {
    var user_id: String? = ""
    val firebase_user = Firebase.auth.currentUser
    firebase_user?.let {
        user_id = it.uid
    }

    return user_id.toString()
}

fun updateUserDisplayName(new_display_name: String) {
    val firebase_user = Firebase.auth.currentUser
    firebase_user?.updateProfile(userProfileChangeRequest {
        displayName = new_display_name
    })

    updateDataField(
        Strings.get(R.string.firebase_collection_users),
        getCurrentUserId(),
        User::display_name,
        new_display_name
    )
}

fun getUserData(
    user_id: String,
    onUserFound: (User?) -> Unit
) {
    Firebase.firestore
        .collection(Strings.get(R.string.firebase_collection_users))
        .document(user_id)
        .get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                onUserFound.invoke(user)
            } else {
                Log.d("FIREBASE DEBUG", "User document does not exist.")
                onUserFound.invoke(null)
            }
        }
}

fun fetchAllShelters(include_current_user: Boolean = false, onComplete: (List<User>) -> Unit) {
    val firebase_database = Firebase.firestore
    val user_list = mutableListOf<User>()

    // Fetch all documents from the "Users" collection
    firebase_database.collection(Strings.get(R.string.firebase_collection_users))
        .get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val user = document.toObject(User::class.java)
                if (user.is_shelter) {
                    if (include_current_user || getCurrentUserId() != user.user_id) {
                        user_list.add(user)
                    }
                }
            }
            onComplete(user_list) // Return the list after fetching all documents
        }
        .addOnFailureListener { exception ->
            Log.e("FirebaseDatabaseUtilities ERROR", "Failed to fetch users data.", exception)
            onComplete(emptyList()) // Return empty list if an error occurs
        }
}

fun getAnimalData(animal_id: String, onComplete: (Animal?) -> Unit) {
    // Firestore query
    Firebase.firestore
        .collection(Strings.get(R.string.firebase_collection_animals))
        .document(animal_id)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val animal = document.toObject(Animal::class.java)
                onComplete(animal)
            } else {
                Log.w("getAnimalData", "Document does not exist, returning null.")
                onComplete(null)
            }
        }
        .addOnFailureListener { e ->
            println("ERROR: Firestore query failed - ${e.message}")
            onComplete(null)
        }
}

fun fetchAnimals(
    user_animal_ids: List<String>,
    on_complete: (List<Animal>) -> Unit
) {
    val firebase_database = Firebase.firestore
    val animal_list = mutableListOf<Animal>()

    if (user_animal_ids.isEmpty()) {
        on_complete(emptyList()) // No animals, return an empty list
        return
    }

    // Allocate each animal_id to a firebase get() query
    val batchRequests: List<Task<DocumentSnapshot>> = user_animal_ids.map { animalId ->
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

fun fetchAllAnimals(onComplete: (List<Animal>) -> Unit) {
    val firebaseDatabase = Firebase.firestore
    val animal_list = mutableListOf<Animal>()

    // Fetch all documents from the "Animals" collection
    firebaseDatabase.collection(Strings.get(R.string.firebase_collection_animals))
        .get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val animal = document.toObject(Animal::class.java)
                animal_list.add(animal)
            }
            onComplete(animal_list) // Return the list after fetching all documents
        }
        .addOnFailureListener { exception ->
            Log.e("FirebaseDatabaseUtilities ERROR", "Failed to fetch animal data.", exception)
            onComplete(emptyList()) // Return empty list if an error occurs
        }
}

fun fetchAnimalsByShelter(
    shelter_id: String,
    onSuccess: ((List<Animal>) -> Unit)? = null,
    onFailure: ((Exception) -> Unit)? = null
) {
    val firebaseDatabase = Firebase.firestore

    firebaseDatabase.collection(Strings.get(R.string.firebase_collection_animals))
        .whereEqualTo(Animal::associated_shelter_id.name, shelter_id)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val animal_list = querySnapshot.documents.mapNotNull { it.toObject(Animal::class.java) }
            onSuccess?.invoke(animal_list)
        }
        .addOnFailureListener { exception ->
            onFailure?.invoke(exception)
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

fun addAnimalToDatabaseAndAssociateToShelter(
    new_animal: Animal,
    onUploadSuccess: (() -> Unit)? = null

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
        ) { onUploadSuccess?.invoke() }
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
        removeFromDataFieldList(
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

fun setDocumentData(
    collection: String,
    document_id: String,
    new_document_data: Any,
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

    document_ref.set(new_document_data)
        .addOnFailureListener {
            Log.w(
                "FirebaseDatabaseUtilities ERROR",
                "Failed to set document ${document_id} to $new_document_data"
            )
        }
        .addOnSuccessListener { onUploadSuccess?.invoke() }
}

fun <T> updateDataField(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_value: Any?,
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
fun <T> removeFromDataFieldList(
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
            onRemovalSuccess?.invoke()
        }.addOnFailureListener { println("Error updating field: ${it.message}") }
}

fun <T> _syncDatabaseForRemovedImages(
    field_name: KProperty1<T, *>,
    values_to_be_removed: Array<String>
) {
    if (field_name.name.contains("image", ignoreCase = true)) {
        deleteImagesFromCloudStorage(values_to_be_removed)
    }
}

fun <T, V> appendToDataFieldMap(
    collection: String,
    document_id: String,
    field_name: KProperty1<T, *>,
    field_key: String,
    field_value: V,
    onUploadSuccess: (() -> Unit)? = null
) {
    if (collection != Strings.get(R.string.firebase_collection_users) && collection != Strings.get(R.string.firebase_collection_animals)) {
        Log.e("DEVELOPMENT BUG", "Not a valid collection input.")
        return
    }

    val firebase_database = Firebase.firestore
    val document_ref =
        firebase_database.collection(collection).document(document_id)

    document_ref.update("${field_name.name}.$field_key", field_value)
        .addOnSuccessListener { onUploadSuccess?.invoke() }
        .addOnFailureListener { e ->
            Log.w(
                "FirebaseDatabaseUtilities ERROR",
                "Failed to update ${field_name.name} to $field_value", e
            )
        }
}

fun <T> updateExplorePreferencesField(
    field_name: KProperty1<T, *>,
    value: Any,
    onSuccess: (() -> Unit)? = null,
    onFailure: ((Exception) -> Unit)? = null
) {
    val firebase_database = Firebase.firestore
    val field_path =
        "explore_preferences.${field_name.name}" // Constructs the dot notation field path

    var value_mod = value
    if (value is Array<*>) {
        Log.w("Parcelable Warning", "Parcelable does not support arrays. Converting to list.")
        value_mod = value.toList()
    }

    firebase_database.collection(Strings.get(R.string.firebase_collection_users))
        .document(getCurrentUserId())
        .update(field_path, value_mod)
        .addOnSuccessListener {
            onSuccess?.invoke()
            Log.d("FirestoreUpdate", "Successfully updated $field_path to $value_mod")
        }
        .addOnFailureListener { exception ->
            onFailure?.invoke(exception)
            Log.e("FirestoreUpdate", "Error updating $field_path", exception)
        }
}

fun recalculatePreferenceVector() {
    val TAG = "PreferenceVector"
    val user_ref = Firebase.firestore.collection(Strings.get(R.string.firebase_collection_users))
        .document(getCurrentUserId())

    user_ref.get().addOnSuccessListener { snapshot ->
        val user = snapshot.toObject(User::class.java)
        if (user == null) {
            Log.e(TAG, "User not found or failed to deserialize.")
            return@addOnSuccessListener
        }

        val liked_animal_ids = user.liked_animal_ids
        Log.d(TAG, "Liked animal IDs: $liked_animal_ids")

        if (liked_animal_ids.isEmpty()) {
            Log.d(TAG, "No liked animals – skipping vector recalculation.")
            return@addOnSuccessListener
        }

        Firebase.firestore.collection(Strings.get(R.string.firebase_collection_animals))
            .whereIn(FieldPath.documentId(), liked_animal_ids)
            .get()
            .addOnSuccessListener { animalDocs ->
                Log.d(TAG, "Fetched ${animalDocs.size()} liked animals from Firestore")

                val vectors = animalDocs.mapNotNull { doc ->
                    val animal = doc.toObject(Animal::class.java)
                    if (animal.animal_type == null || animal.animal_size == null || animal.animal_age == null) {
                        Log.w(
                            "PreferenceVector",
                            "Missing data in animal ${animal.animal_id}, skipping"
                        )
                        return@mapNotNull null
                    }

                    val vector = VectorUtils.animalToVector(animal)
                    Log.d("PreferenceVector", "Vector for ${animal.animal_id}: $vector")
                    vector
                }

                if (vectors.isEmpty()) {
                    Log.d(TAG, "No valid vectors generated – skipping update.")
                    return@addOnSuccessListener
                }

                val avg_vector = VectorUtils.averageVectors(vectors)
                val vectorMap = avg_vector.mapIndexed { i, v -> i.toString() to v }.toMap()

                Log.d(TAG, "Average preference vector: $vectorMap")

                user_ref.update(User::preference_vector.name, vectorMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "Preference vector updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update preference vector: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch liked animals: ${e.message}", e)
            }
    }.addOnFailureListener { e ->
        Log.e(TAG, "Failed to fetch user data: ${e.message}", e)
    }
}

fun getRecommendations(location: GeoPoint? = null, onResult: (List<Animal>) -> Unit) {
    val TAG = "RecommendationEngine"

    val user_ref = Firebase.firestore.collection(Strings.get(R.string.firebase_collection_users))
        .document(getCurrentUserId())

    user_ref.get().addOnSuccessListener { userSnap ->
        val user = userSnap.toObject(User::class.java) ?: return@addOnSuccessListener

        Log.d(TAG, "Fetched user: ${user.user_id}")
        Log.d(TAG, "Liked IDs: ${user.liked_animal_ids}")
        Log.d(TAG, "Viewed IDs: ${user.viewed_animals.keys}")
        Log.d(TAG, "Hosted IDs: ${user.hosted_animal_ids}")
        Log.d(TAG, "Adopting IDs: ${user.adopting_animal_ids}")

        val user_vector_map = user.preference_vector
        if (user_vector_map.isEmpty()) {
            Log.d(TAG, "User preference vector is empty, recalculating preference vector")
            recalculatePreferenceVector()
        }

        val user_vector = user_vector_map.toSortedMap().values.toList()
        Log.d(TAG, "User preference vector: $user_vector")

        val excluded_animal_ids = mutableSetOf<String>().apply {
            addAll(user.liked_animal_ids)
            addAll(user.hosted_animal_ids)
            addAll(user.adopting_animal_ids)

            //Exclude animals already shown to the user less than 30 days ago
            val thirty_days_millis = 30L * 24 * 60 * 60 * 1000
            val now = System.currentTimeMillis()

            user.viewed_animals.forEach { (animal_id, timestamp_str) ->
                val viewed_at = timestamp_str.toLongOrNull()
                if (viewed_at != null && now - viewed_at <= thirty_days_millis) {
                    add(animal_id)
                }
            }
        }
        Log.d(TAG, "Excluding animal IDs: $excluded_animal_ids")

        val effective_location = location ?: user.location
        if (effective_location == null) {
            Log.w(TAG, "No user location provided or stored – location filtering will be skipped.")
        } else {
            Log.d(TAG, "Using effective location for filtering: $effective_location")
        }

        Firebase.firestore.collection(Strings.get(R.string.firebase_collection_animals))
            .get()
            .addOnSuccessListener { animal_docs ->
                Log.d(TAG, "Fetched ${animal_docs.size()} animals from Firestore")

                val scored_animals = animal_docs.mapNotNull { doc ->
                    val animal = doc.toObject(Animal::class.java)
                    if (animal.animal_id in excluded_animal_ids) {
                        Log.d(TAG, "Skipping excluded animal: ${animal.animal_id}")
                        return@mapNotNull null
                    }

                    if (!animalWithinSearchParameters(user, animal, effective_location)) {
                        Log.d(
                            TAG,
                            "Skipping animal outside of search parameters: ${animal.animal_id}"
                        )
                        return@mapNotNull null
                    }

                    val animal_vector = VectorUtils.animalToVector(animal)
                    val score = VectorUtils.cosineSimilarity(user_vector, animal_vector)

                    Log.d(TAG, "Scored animal ${animal.animal_id}: $score")
                    animal to score
                }.sortedByDescending { it.second }

                val topAnimals = scored_animals.take(20).map { it.first }

                Log.d(
                    TAG,
                    "Top ${topAnimals.size} recommended animals: ${topAnimals.map { it.animal_id }}"
                )

                onResult(topAnimals)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch animals: ${e.message}", e)
            }
    }.addOnFailureListener { e ->
        Log.e(TAG, "Failed to fetch user data: ${e.message}", e)
    }
}

fun animalWithinSearchParameters(user: User, animal: Animal, user_location: GeoPoint?): Boolean {
    val radius_km = user.explore_preferences?.search_radius_miles ?: 0.0
    val allowed_sizes = user.explore_preferences?.animal_sizes
    val allowed_types = user.explore_preferences?.animal_types
    val min_age = user.explore_preferences?.min_animal_age
    val max_age = user.explore_preferences?.max_animal_age

    Log.d("search_prefs", "Checking animal ${animal.animal_id} against preferences")

    // Location filter
    if (user_location != null && animal.location != null) {
        val distance = haversineDistance(user_location, animal.location!!)
        Log.d("search_prefs", "Distance to animal: $distance km (max allowed: $radius_km)")
        if (distance > radius_km) {
            Log.d("search_prefs", "Filtered out by distance")
            return false
        }
    }

    // Size filter
    if (allowed_sizes != null) {
        Log.d("search_prefs", "Allowed sizes: $allowed_sizes, Animal size: ${animal.animal_size}")
        if (animal.animal_size != null && animal.animal_size !in allowed_sizes) {
            Log.d("search_prefs", "Filtered out by size")
            return false
        }
    }

    // Type filter
    if (allowed_types != null) {
        Log.d("search_prefs", "Allowed types: $allowed_types, Animal type: ${animal.animal_type}")
        if (animal.animal_type != null && animal.animal_type !in allowed_types) {
            Log.d("search_prefs", "Filtered out by type")
            return false
        }
    }

    // Minimum age filter
    if (min_age != null && animal.animal_age != null) {
        Log.d("search_prefs", "Minimum age: $min_age, Animal age: ${animal.animal_age}")
        if (animal.animal_age!! < min_age) {
            Log.d("search_prefs", "Filtered out by minimum age")
            return false
        }
    }

    // Maximum age filter
    if (max_age != null && animal.animal_age != null) {
        Log.d("search_prefs", "Maximum age: $max_age, Animal age: ${animal.animal_age}")
        if (animal.animal_age!! > max_age) {
            Log.d("search_prefs", "Filtered out by maximum age")
            return false
        }
    }

    Log.d("search_prefs", "Animal ${animal.animal_id} passed all filters")
    return true
}


fun haversineDistance(a: GeoPoint, b: GeoPoint): Double {
    val R = 3958.8 // Earth radius in miles
    val d_lat = Math.toRadians(b.latitude - a.latitude)
    val d_lon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)

    val a_calc = sin(d_lat / 2).pow(2) + sin(d_lon / 2).pow(2) * cos(lat1) * cos(lat2)
    val c = 2 * atan2(sqrt(a_calc), sqrt(1 - a_calc))
    return R * c
}

