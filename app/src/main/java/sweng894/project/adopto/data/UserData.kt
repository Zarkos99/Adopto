package sweng894.project.adopto.data

import com.google.firebase.firestore.PropertyName
import java.time.Instant
import java.time.format.DateTimeFormatter

data class User(
    var user_id: String = "",
    @get:PropertyName("is_shelter")
    @set:PropertyName("is_shelter")
    var is_shelter: Boolean = false,
    var biography: String? = "",
    var profile_image_path: String? = "",
    var saved_animal_ids: ArrayList<String> = ArrayList(),
    var hosted_animal_ids: ArrayList<String> = ArrayList(),
    var zip_code: String? = "",
    var need_info: Boolean = false
)

data class Animal(
    var associated_shelter_id: String = "",
    var animal_name: String? = "",
    var animal_age: Double? = 0.0,
    var health_summary: String? = "",
    var biography: String? = "",
    var profile_image_path: String? = "",
    var supplementary_image_paths: ArrayList<String> = ArrayList(),
    var post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
) {
    val animal_id: String get() = Int.hashCode().toString()
}