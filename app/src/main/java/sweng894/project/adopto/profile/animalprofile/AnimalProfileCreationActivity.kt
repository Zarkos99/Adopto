package sweng894.project.adopto.profile.animalprofile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.addAnimalToDatabase
import sweng894.project.adopto.database.uploadAnimalImageAndUpdateAnimal
import sweng894.project.adopto.databinding.AnimalProfileCreationLayoutBinding


class AnimalProfileCreationActivity : AppCompatActivity() {

    private lateinit var m_name_input_edit_text: EditText
    private lateinit var m_age_input_edit_text: EditText
    private lateinit var m_health_input_edit_text: EditText
    private lateinit var m_description_input_edit_text: EditText
    private lateinit var m_select_profile_image_intent: ActivityResultLauncher<String>
    private lateinit var m_select_additional_images_intent: ActivityResultLauncher<String>
    private lateinit var m_additional_images_adaptor: AnimalProfileAdditionalImagesAdapter
    private lateinit var m_new_profile_image_uri: Uri

    private val FAKE_NAME = "Tofu"
    private val FAKE_AGE = 1
    private val FAKE_HEALTH = "Healthy"
    private val FAKE_DESCRIPTION = "What a cute little animal!"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileCreationLayoutBinding

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private fun bindToFirebaseService() {
        Intent(this, FirebaseDataServiceUsers::class.java).also { intent ->
            this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromFirebaseService() {
        this.unbindService(connection)
    }

    /** End FirebaseDataService Setup **/

    override fun onStart() {
        super.onStart()
        bindToFirebaseService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromFirebaseService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToFirebaseService()
        binding = AnimalProfileCreationLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_name_input_edit_text = binding.animalNameInputField
        m_age_input_edit_text = binding.animalAgeInputField
        m_health_input_edit_text = binding.animalHealthInputField
        m_description_input_edit_text = binding.descriptionInputField
        val animal_profile_image_view = binding.profileImageView
        val additional_images_image_view = binding.additionalImagesButton
        val cancel_button_view = binding.cancelButton
        val create_button_view = binding.createButton

        val additional_images_recycler_view = binding.additionalImages

        // Initialize recyclerview adaptor
        m_additional_images_adaptor =
            AnimalProfileAdditionalImagesAdapter(this, true)
        additional_images_recycler_view.adapter = m_additional_images_adaptor
        additional_images_recycler_view.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        initializeEditTexts()

        m_select_profile_image_intent =
            registerForActivityResult(ActivityResultContracts.GetContent())
            { uri ->
                if (uri != null) {
                    animal_profile_image_view.setImageURI(uri)
                    m_new_profile_image_uri = uri
                }
            }

        m_select_additional_images_intent =
            registerForActivityResult(ActivityResultContracts.GetMultipleContents())
            { uris ->
                uris.forEach { uri -> m_additional_images_adaptor.addItem(uri) }
            }

        animal_profile_image_view.setOnClickListener {
            m_select_profile_image_intent.launch("image/*")
        }

        additional_images_image_view.setOnClickListener {
            m_select_additional_images_intent.launch("image/*")
        }

        cancel_button_view.setOnClickListener {
            finish()
        }

        create_button_view.setOnClickListener {
            handleCreateAnimalProfile()
        }
    }

    fun handleCreateAnimalProfile() {
        val current_user = m_firebase_data_service.current_user_data
        if (!current_user?.is_shelter!!) {
            Log.d("IMPROPER ACCESS", "Non-shelter account attempting to create animal profile.")
            return
        }

        var new_name = m_name_input_edit_text.text.toString().trim()
        var new_age_str = m_age_input_edit_text.text.toString().trim()
        var new_health = m_health_input_edit_text.text.toString().trim()
        var new_description = m_description_input_edit_text.text.toString().trim()

        var error = false

        // Default to fake values if fields are empty
        if (new_name.isEmpty()) {
            new_name = FAKE_NAME
        }
        if (new_age_str.isEmpty()) {
            new_age_str = FAKE_AGE.toString()
        }
        if (new_health.isEmpty()) {
            new_health = FAKE_HEALTH
        }
        if (new_description.isEmpty()) {
            new_description = FAKE_DESCRIPTION
        }

        var new_age = 0.0
        try {
            new_age = new_age_str.toDouble()
        } catch (e: Exception) {
            //Display error message
            Toast.makeText(
                this,
                "Invalid age. Please input a decimal number (ex. 5, 5.5, or 5.0)",
                Toast.LENGTH_LONG
            ).show()
            error = true
        }

        if (m_new_profile_image_uri.toString().isEmpty()) {
            //Display error message
            Toast.makeText(
                this,
                "Please select an image for your post",
                Toast.LENGTH_LONG
            ).show()
            error = true
        }

        if (error) {
            return
        }

        val new_animal = Animal(
            associated_shelter_id = current_user.user_id,
            animal_name = new_name,
            animal_age = new_age,
            health_summary = new_health,
            biography = new_description
        )
        addAnimalToDatabase(new_animal)

        // Send profile image to database and get cloud storage path, create animal with that path
        uploadAnimalImageAndUpdateAnimal(new_animal.animal_id, m_new_profile_image_uri, true)

        // For each image in recyclerview, upload to cloud storage, and obtain cloud storage image path.
        // Store those paths in ArrayList<String> and create animal with those paths
        m_additional_images_adaptor.getImages().forEach { image ->
            uploadAnimalImageAndUpdateAnimal(new_animal.animal_id, image)
        }

        finish()
    }

    fun initializeEditTexts() {
        m_name_input_edit_text.hint = FAKE_NAME
        m_age_input_edit_text.hint = FAKE_AGE.toString()
        m_health_input_edit_text.hint = FAKE_HEALTH
        m_description_input_edit_text.hint = FAKE_DESCRIPTION
    }
}