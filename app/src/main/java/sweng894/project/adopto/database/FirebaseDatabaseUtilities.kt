package sweng894.project.adopto.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.Shelter
import sweng894.project.adopto.data.User

fun getCurrentUserId(): String {
    var user_id: String? = ""
    val firebase_user = Firebase.auth.currentUser
    firebase_user?.let {
        user_id = it.uid
    }

    return user_id.toString()
}

// TODO: Figure out how to instantiate this information on authentication and persist it
fun isCurrentUserAShelter(): Boolean {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val shelter_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)

    var is_shelter = false;

    shelter_ref.get().addOnSuccessListener { document ->
        if (document != null) {
            is_shelter = true
        }
    }

    return is_shelter
}


fun addUser() {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val new_user = User(user_id = user_id)

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_users)
    ).document(user_id).set(new_user)
}

fun addShelter() {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val new_shelter = Shelter(shelter_id = user_id)

    firebase_database.collection(
        Strings.get(R.string.firebase_collection_shelters)
    ).document(user_id).set(new_shelter)
}

fun updateUserDataField(field_name: String, field_value: Any) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_users)
        ).document(user_id)

    user_ref.update(field_name, field_value).addOnFailureListener {
        Log.w(
            "updateUserDataField Error",
            "Failed to update $field_name to $field_value"
        )
    }
}

fun updateShelterDataField(field_name: String, field_value: Any) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)

    user_ref.update(field_name, field_value).addOnFailureListener {
        Log.w(
            "updateShelterDataField Error",
            "Failed to update $field_name to $field_value"
        )
    }
}

fun updateAnimalDataField(animal_id: String, field_name: String, field_value: Any) {
    val firebase_database = Firebase.firestore

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_animals)
        ).document(animal_id)

    user_ref.update(field_name, field_value).addOnFailureListener {
        Log.w(
            "updateAnimalDataField Error",
            "Failed to update $field_name to $field_value"
        )
    }
}

fun addAnimalToShelter(animal: Animal) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)
    user_ref.update(
        Strings.get(R.string.shelter_animals_field_name),
        FieldValue.arrayUnion(animal.animal_id)
    )
}

fun addAnimalToUser(animal: Animal) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)
    user_ref.update(
        Strings.get(R.string.shelter_animals_field_name),
        FieldValue.arrayUnion(animal.animal_id)
    )
}

fun _addAnimal(animal: Animal) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)
    user_ref.update(
        Strings.get(R.string.shelter_animals_field_name),
        FieldValue.arrayUnion(animal.animal_id)
    )
}

fun removeAnimalFromShelter(animals_to_remove: ArrayList<Animal>) {
    val firebase_database = Firebase.firestore
    val user_id = getCurrentUserId()

    // Delete in database
    val user_ref =
        firebase_database.collection(
            Strings.get(R.string.firebase_collection_shelters)
        ).document(user_id)

    user_ref.get().addOnSuccessListener { document ->
        if (document != null) {
            val shelter = document.toObject<Shelter>()
            if (shelter != null) {
                for (animal_to_remove in animals_to_remove) {
                    shelter.hosted_animals.removeIf { it.animal_id == animal_to_remove.animal_id }
                }
                user_ref.set(shelter)
            }
        }
    }
}

