package sweng894.project.adopto.data

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Parcelize
data class User(
    var user_id: String = "",
    @get:PropertyName("is_shelter")
    @set:PropertyName("is_shelter")
    var is_shelter: Boolean = false,
    var biography: String? = "",
    var profile_image_path: String? = "",
    var saved_animal_ids: MutableList<String> = mutableListOf(),
    var hosted_animal_ids: MutableList<String> = mutableListOf(),
    var viewed_animals: Map<String, String> = mapOf(), //animal_id, timestamp
    var zip_code: String? = "",
    var need_info: Boolean = false,
    @get:PropertyName("explore_preferences")
    @set:PropertyName("explore_preferences")
    var explore_preferences: ExplorationPreferences? = ExplorationPreferences()
) : Parcelable

@Parcelize
data class ExplorationPreferences(
    var min_animal_age: Double? = 0.0,
    var max_animal_age: Double? = 30.0,
    var animal_sizes: MutableList<String> = mutableListOf(),
    var animal_types: MutableList<String> = mutableListOf(),
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
    var post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
) : Parcelable {
}