package sweng894.project.adopto.data

import java.time.Instant
import java.time.format.DateTimeFormatter

data class User(
    val user_id: String = "",
    val biography: String? = "",
    val profile_image_path: String? = "",
    val saved_animal_ids: ArrayList<String> = ArrayList(),
    val zip_code: String? = ""
) {
    val is_shelter: Boolean get() = false
}

data class Shelter(
    val shelter_id: String = "",
    val biography: String? = "",
    val profile_image_path: String? = "",
    val hosted_animal_ids: ArrayList<String> = ArrayList(),
    val zip_code: String? = ""
) {
    val is_shelter: Boolean get() = true
}

data class Animal(
    val associated_shelter_id: String = "",
    val animal_name: String? = "",
    val health_description: String? = "",
    val biography: String? = "",
    var profile_image_path: String? = "",
    var supplementary_image_paths: ArrayList<String> = ArrayList(),
    val post_time: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
) {
    val animal_id: Int get() = hashCode()
}