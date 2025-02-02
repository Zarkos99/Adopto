package sweng894.project.adopto.data

import java.time.Instant
import java.time.format.DateTimeFormatter

data class User(
    val user_id: String = "",
    var is_shelter: Boolean = false,
    val biography: String? = "",
    val profile_image_path: String? = "",
    val saved_animal_ids: ArrayList<String> = ArrayList(),
    val zip_code: String? = "",
    val need_info: Boolean = false
)

data class Animal(
    var associated_shelter_id: String = "",
    var animal_name: String? = "",
    var health_summary: String? = "",
    var biography: String? = "",
    var profile_image_path: String? = "",
    var supplementary_image_paths: ArrayList<String> = ArrayList(),
    val post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
) {
    val animal_id: String get() = Int.hashCode().toString()
}