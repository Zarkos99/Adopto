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

object AnimalSizes {
    const val TINY = "Tiny (< 5 lbs)"
    const val SMALL = "Small (5 - 20 lbs)"
    const val MEDIUM = "Medium (21 - 50 lbs)"
    const val LARGE = "Large (51 - 99 lbs)"
    const val XL = "XL (100+ lbs)"

    val all = listOf(TINY, SMALL, MEDIUM, LARGE, XL)
}

object AnimalTypes {
    const val DOG = "Dog"
    const val CAT = "Cat"
    const val BIRD = "Bird"
    const val RABBIT = "Rabbit"
    const val REPTILE = "Reptile"
    const val FISH = "Fish"
    const val OTHER = "Other"

    val all = listOf(DOG, CAT, BIRD, RABBIT, REPTILE, FISH, OTHER)
}

object AnimalGenders {
    const val UNKNOWN = "Unknown"
    const val FEMALE = "Female"
    const val MALE = "Male"

    val all = listOf(UNKNOWN, FEMALE, MALE)
}

@Parcelize
data class User(
    var user_id: String = "",
    var fcm_token: String = "",
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
    var animal_gender: String? = "",
    var health_summary: String? = "",
    var biography: String? = "",
    var profile_image_path: String? = "",
    var supplementary_image_paths: MutableList<String> = mutableListOf(),
    var post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    var location: @RawValue GeoPoint? = null
) : Parcelable {

    val normalized_size: String
        get() = normalizeAnimalSize(animal_size)

    val normalized_type: String
        get() = normalizeAnimalType(animal_type)
}

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
    var last_read_timestamps: Map<String, String> = mapOf() // user_id -> timestamp
) : Parcelable

//Messages are stored as individual subcollections of a Chat
@Parcelize
data class Message(
    var message_id: String = UUID.randomUUID().toString(),
    var sender_id: String = "",
    var timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    var content: String = "",
) : Parcelable

fun normalizeAnimalSize(size: String?): String {
    return when (size?.trim()?.lowercase()) {
        "tiny", "tiny (< 5 lbs)" -> AnimalSizes.TINY
        "small", "small (5 - 20 lbs)" -> AnimalSizes.SMALL
        "medium", "medium (21 - 50 lbs)" -> AnimalSizes.MEDIUM
        "large", "large (51 - 99 lbs)" -> AnimalSizes.LARGE
        "xl", "xl (100+ lbs)" -> AnimalSizes.XL
        else -> AnimalSizes.MEDIUM // default fallback
    }
}

fun normalizeAnimalType(type: String?): String {
    return when (type?.trim()?.lowercase()) {
        "dog" -> AnimalTypes.DOG
        "cat" -> AnimalTypes.CAT
        "bird" -> AnimalTypes.BIRD
        "rabbit" -> AnimalTypes.RABBIT
        "reptile" -> AnimalTypes.REPTILE
        "fish" -> AnimalTypes.FISH
        else -> AnimalTypes.OTHER
    }
}

