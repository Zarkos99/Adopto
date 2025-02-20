package sweng894.project.adopto.database

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import java.util.UUID

fun uploadUserProfileImageAndUpdateUserImagePath(
    firebaseDataService: FirebaseDataServiceUsers, file: Uri
) {
    if (file.path == null) {
        Log.w("uploadProfileImage Failure", "Local image uri is null.")
        return
    }

    val m_firebase_storage = Firebase.storage
    val current_user = firebaseDataService.current_user_data

    if (!current_user?.profile_image_path.isNullOrEmpty()) {
        // Delete the previous profile image
        deleteImagesFromCloudStorage(arrayOf(current_user?.profile_image_path!!))
    }

    // Create a storage reference from our app
    val storage_ref = m_firebase_storage.reference

    val storage_path_to_image =
        "${Strings.get(R.string.firebase_collection_users)}/${getCurrentUserId()}/profile_image"
    val image_ref = storage_ref.child(storage_path_to_image)
    val upload_task = image_ref.putFile(file)

    // Register observers to listen for when the download is done or if it fails
    upload_task.addOnFailureListener {
        // Handle unsuccessful uploads
        Log.e("Image Storage Upload Error", "Unable to upload image: $it")
    }.addOnSuccessListener {
        // Set the image path to where it exists on the cloud storage
        updateDataField(
            Strings.get(R.string.firebase_collection_users),
            getCurrentUserId(),
            User::profile_image_path,
            storage_path_to_image
        )
    }
}

fun uploadAnimalImageAndUpdateAnimal(
    animal_id: String,
    file: Uri,
    is_profile_image: Boolean = false,
    onUploadSuccess: (() -> Unit)? = null
) {
    val m_firebase_storage = Firebase.storage
    if (file.path == null) {
        Log.w("uploadImage Failure", "Local image uri is null.")
        return
    }

    // Create a storage reference from our app
    val storage_ref = m_firebase_storage.reference

    val unique_id = UUID.randomUUID().toString()

    val storage_path_to_image = if (is_profile_image) {
        "${Strings.get(R.string.firebase_collection_animals)}/Animal_$animal_id/profile_image"
    } else {
        val image_name = "image_$unique_id"
        "${Strings.get(R.string.firebase_collection_animals)}/Animal_${animal_id}/supplementary_images/${image_name}"
    }
    val image_ref = storage_ref.child(storage_path_to_image)
    val upload_task = image_ref.putFile(file)

    // Register observers to listen for when the download is done or if it fails
    upload_task.addOnFailureListener {
        // Handle unsuccessful uploads
        Log.e("Image Storage Upload Error", "Unable to upload image: $it")
    }.addOnSuccessListener {
        // Set the image path to where it exists on the cloud storage
        if (is_profile_image) {
            updateDataField(
                Strings.get(R.string.firebase_collection_animals),
                animal_id,
                Animal::profile_image_path,
                storage_path_to_image,
                onUploadSuccess
            )
        } else {
            appendToDataFieldArray(
                Strings.get(R.string.firebase_collection_animals),
                animal_id,
                Animal::supplementary_image_paths,
                storage_path_to_image,
                onUploadSuccess
            )
        }
    }
}

fun deleteImagesFromCloudStorage(image_paths_to_remove: Array<String>) {
    val m_firebase_storage = Firebase.storage
    // Create a storage reference from our app
    val storage_ref = m_firebase_storage.reference

    // Delete images from cloud storage
    image_paths_to_remove.forEach { image_path_to_remove ->
        if (image_path_to_remove.isNotEmpty()) {
            val image_ref = storage_ref.child(image_path_to_remove)
            image_ref.delete().addOnFailureListener {
                // Handle unsuccessful deletion
                Log.e("Image Storage Deletion Error", "Unable to delete image: $it")
            }
        }
    }
}

fun loadCloudStoredImageIntoImageView(context: Context, image_path: String?, view: ImageView) {
    if (!image_path.isNullOrEmpty()) {
        val storage_ref = Firebase.storage.reference.child(image_path)
        storage_ref.downloadUrl.addOnSuccessListener {
            try {
                Glide.with(context).load(it).into(view)
            } catch (exception: Exception) {
                Log.e("Glide Image Download Error", exception.toString())
            }
        }
    }
}