package sweng894.project.adopto.profile.animalprofile

import android.R
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.custom.CustomSpinnerAdapter
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.AnimalGenders
import sweng894.project.adopto.data.AnimalSizes
import sweng894.project.adopto.data.AnimalTypes
import sweng894.project.adopto.firebase.FirebaseDataServiceUsers
import sweng894.project.adopto.firebase.addAnimalToDatabaseAndAssociateToShelter
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.uploadAnimalImageAndUpdateAnimal
import sweng894.project.adopto.databinding.AnimalProfileCreationLayoutBinding


class AnimalProfileCreationActivity : AppCompatActivity() {

    private lateinit var m_select_profile_image_intent: ActivityResultLauncher<String>
    private lateinit var m_select_additional_images_intent: ActivityResultLauncher<String>
    private lateinit var m_additional_images_adaptor: AnimalProfileCreationImagesAdapter
    private var m_new_profile_image_uri: Uri? = null

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

        val animal_profile_image_view = binding.profileImageView
        val additional_images_image_view = binding.additionalImagesButton
        val cancel_button_view = binding.cancelButton
        val create_button_view = binding.createButton

        val additional_images_recycler_view = binding.additionalImages

        populateSpinnerWithOptions(binding.animalGenderSpinner, AnimalGenders.all)
        populateSpinnerWithOptions(binding.animalTypeSpinner, AnimalTypes.all)
        populateSpinnerWithOptions(binding.animalSizeSpinner, AnimalSizes.all)

        // Initialize recyclerview adaptor
        m_additional_images_adaptor =
            AnimalProfileCreationImagesAdapter(this, 5)
        additional_images_recycler_view.adapter = m_additional_images_adaptor
        additional_images_recycler_view.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

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

    fun populateSpinnerWithOptions(spinner: Spinner, options: List<String>) {
        val adapter = CustomSpinnerAdapter(
            this,
            options
        ).also { it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item) }

        spinner.adapter = adapter
    }


    fun createAnimalFromInputs(): Animal? {
        val new_name = binding.animalNameInput.getInputText()
        val new_age_str = binding.animalAgeInput.getInputText() // Optional Field
        val new_health = binding.animalHealthInput.getInputText() //Optional Field
        val new_description = binding.animalDescriptionInput.getInputText() //Optional Field
        val new_breed = binding.animalBreedInput.getInputText() //Optional Field
        val new_gender = binding.animalGenderSpinner.selectedItem.toString()
        val new_size = binding.animalSizeSpinner.selectedItem.toString()
        val new_type = binding.animalTypeSpinner.selectedItem.toString()

        var error = false

        // Default to fake values if fields are empty
        if (new_name.isEmpty()) {
            error = true
        }
        if (new_gender.isEmpty()) {
            error = true
        }
        if (new_size.isEmpty()) {
            error = true
        }
        if (new_type.isEmpty()) {
            error = true
        }

        var new_age = 0.0
        try {
            new_age = new_age_str.toDouble()
        } catch (e: Exception) {
            //Display error message
            Toast.makeText(
                this,
                "Invalid age input. Ensure age is numerical.",
                Toast.LENGTH_LONG
            ).show()
            error = true
        }

        val current_user_id = getCurrentUserId()
        if (current_user_id.isNullOrEmpty()) {
            error = true
        }

        if (error) {
            return null
        }

        val new_animal = Animal(
            associated_shelter_id = current_user_id!!,
            animal_name = new_name,
            animal_age = new_age,
            health_summary = new_health,
            biography = new_description,
            animal_gender = new_gender,
            animal_size = new_size,
            animal_type = new_type,
            animal_breed = new_breed,
            location = m_firebase_data_service.current_user_data?.location
        )

        return new_animal
    }

    fun handleCreateAnimalProfile() {
        val current_user = m_firebase_data_service.current_user_data
        if (!current_user?.is_shelter!!) {
            Log.d("IMPROPER ACCESS", "Non-shelter account attempting to create animal profile.")
            return
        }

        if (m_new_profile_image_uri == null || m_new_profile_image_uri.toString().isEmpty()) {
            //Display error message
            Toast.makeText(
                this,
                "Please select an image for your animal",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val new_animal = createAnimalFromInputs()
        if (new_animal == null) {
            //Display error message
            Toast.makeText(
                this,
                "Please ensure all required fields are filled",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        addAnimalToDatabaseAndAssociateToShelter(new_animal)

        // For each image in recyclerview, upload to cloud storage, and obtain cloud storage image path.
        // Store those paths in ArrayList<String> and create animal with those paths
        m_additional_images_adaptor.getImages().forEach { image ->
            uploadAnimalImageAndUpdateAnimal(new_animal.animal_id, image)
        }

        // Send profile image to database and get cloud storage path, create animal with that path
        uploadAnimalImageAndUpdateAnimal(
            new_animal.animal_id,
            m_new_profile_image_uri!!,
            true
        ) {
            Log.d(
                "TRACE",
                "New animal ${new_animal.animal_name} profile image successfully uploaded!"
            )
            finish()
        }
    }
}