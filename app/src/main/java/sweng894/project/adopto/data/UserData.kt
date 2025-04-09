package sweng894.project.adopto.data

import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

object FirebaseCollections {
    const val USERS = "Users"
    const val ANIMALS = "Animals"
    const val ADOPTIONS = "Adoptions"
    const val CHATS = "Chats"

    val all = setOf(USERS, ANIMALS, ADOPTIONS, CHATS)
}

@Parcelize
data class User(
    var user_id: String = "",
    var display_name: String = "",
    @get:PropertyName("is_shelter")
    @set:PropertyName("is_shelter")
    var is_shelter: Boolean = false,
    var biography: String? = "",
    var profile_image_path: String? = "",
    var liked_animal_ids: MutableList<String> = mutableListOf(), // Animals saved by user for later
    var adopting_animal_ids: MutableList<String> = mutableListOf(), // Animals user is seeking to adopt
    var hosted_animal_ids: MutableList<String> = mutableListOf(), // Animals hosted by a shelter (SHELTER ONLY)
    var viewed_animals: Map<String, String> = mapOf(), //animal_id, timestamp
    var location: @RawValue GeoPoint? = null,
    var need_info: Boolean = false,
    @get:PropertyName("explore_preferences")
    @set:PropertyName("explore_preferences")
    var explore_preferences: ExplorationPreferences? = ExplorationPreferences(),
    var chat_ids: List<String> = listOf(),
    val preference_vector: Map<String, Double> = mapOf() // Vectorized average preferences based on liked animals
) : Parcelable

@Parcelize
data class ExplorationPreferences(
    var min_animal_age: Double? = 0.0,
    var max_animal_age: Double? = 30.0,
    var animal_sizes: MutableList<String> = mutableListOf(),
    var animal_types: MutableList<String> = mutableListOf(),
    var search_radius_miles: Double? = 20.0 // miles
) : Parcelable

@Parcelize
data class Animal(
    val animal_id: String = UUID.randomUUID().toString(), // Ensures unique ID
    var associated_shelter_id: String = "",
    var animal_name: String? = "",
    var animal_age: Double? = 0.0,
    var animal_size: String? = "",
    var animal_type: String? = "",
    var animal_breed: String? = "",
    var health_summary: String? = "",
    var biography: String? = "",
    var profile_image_path: String? = "",
    var supplementary_image_paths: MutableList<String> = mutableListOf(),
    var post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    var location: @RawValue GeoPoint? = null
) : Parcelable

@Parcelize
data class AnimalAdoptionInterest(
    var interested_users: MutableList<AnimalAdoptionInterestedUser> = mutableListOf(), // Animals saved by user for later
) : Parcelable

@Parcelize
data class AnimalAdoptionInterestedUser(
    var user_id: String = "",
    var post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
) : Parcelable

@Parcelize
data class Chat(
    var chat_id: String = UUID.randomUUID().toString(),
    var chat_name: String = "",
    var participant_ids: List<String> = listOf(), // exactly 2 for 1:1, more for group chat
    var last_updated: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    var messages: List<Message> = listOf()
) : Parcelable

@Parcelize
data class Message(
    var message_id: String = UUID.randomUUID().toString(),
    var sender_id: String = "",
    var timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    var content: String = "",
) : Parcelable
