package sweng894.project.adopto.profile.animalprofile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.*
import sweng894.project.adopto.databinding.AnimalProfileViewingLayoutBinding


class AnimalProfileViewingActivity : AppCompatActivity() {

    private var m_current_user: User? = null
    private var m_animal_id: String? = null
    private var m_selected_animal: Animal? = null
    private var m_adapter: AnimalProfileViewingImagesAdapter? = null

    private val select_additional_images_intent =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                m_selected_animal?.let { animal ->
                    uploadAnimalImageAndUpdateAnimal(
                        animal.animal_id,
                        uri
                    ) {
                        getAnimalAndExecuteCallback(animal.animal_id) {
                            val image_uris =
                                ArrayList(m_selected_animal?.supplementary_image_paths?.map {
                                    Uri.parse(it)
                                } ?: emptyList())
                            m_adapter?.setItems(image_uris)
                        }
                    }
                }
            }
        }

    private val edit_animal_launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updated_animal =
                    result.data?.getParcelableExtra<Animal>("updated_animal")
                updated_animal?.let {
                    m_selected_animal = it
                    populateTextViewsWithAnimalInfo(it)
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                finish()
            }
        }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileViewingLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnimalProfileViewingLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data using the same key
        m_animal_id = intent.getStringExtra("animal_id")

        getUserData(getCurrentUserId()) { user ->
            m_current_user = user

            val error_str_prefix = "Cannot find "
            var error_str = ""

            error_str += if (m_animal_id == null) "animal" else ""
            error_str += if (m_current_user == null && m_animal_id == null) "and user" else if (m_current_user == null) "user" else ""

            if (error_str.isNotEmpty()) {
                //Display error message
                Toast.makeText(
                    this,
                    error_str_prefix + error_str,
                    Toast.LENGTH_LONG
                ).show()
                Log.e("AnimalProfileViewingActivity", error_str_prefix + error_str)
                finish()
            }

            getAnimalAndExecuteCallback(m_animal_id) {
                initializeRecyclerViewAdapter(m_selected_animal!!)
                populateTextViewsWithAnimalInfo(m_selected_animal!!)
                populateProfileImage(m_selected_animal!!)

                val edit_profile_button = binding.editProfileButton
                edit_profile_button.setOnClickListener {
                    val intent = Intent(
                        this@AnimalProfileViewingActivity,
                        AnimalProfileEditActivity::class.java
                    )
                    intent.putExtra("current_animal", m_selected_animal)
                    edit_animal_launcher.launch(intent)
                    // Not calling finish() here so that AnimalProfileEditActivity will come back to this activity)
                }

                if (m_selected_animal!!.associated_shelter_id == getCurrentUserId()) {
                    edit_profile_button.visibility = View.VISIBLE
                    binding.addImageButton.visibility = View.VISIBLE
                }

                val save_animal_button = binding.likeAnimalButton
                // Ensure hosting shelter cannot save their own animals
                save_animal_button.visibility =
                    if (m_selected_animal?.associated_shelter_id != getCurrentUserId()) View.VISIBLE else View.GONE
                instantiateSaveAnimalButton()
                instantiateAdoptButton()

                save_animal_button.setOnClickListener {
                    if (m_current_user?.liked_animal_ids?.contains(m_selected_animal?.animal_id) == true) {
                        removeFromDataFieldList(
                            Strings.get(R.string.firebase_collection_users),
                            getCurrentUserId(),
                            User::liked_animal_ids,
                            arrayOf(m_selected_animal!!.animal_id)
                        ) {
                            m_current_user?.liked_animal_ids?.remove(m_selected_animal!!.animal_id)
                            instantiateSaveAnimalButton()
                        }
                    } else {
                        appendToDataFieldArray(
                            Strings.get(R.string.firebase_collection_users),
                            getCurrentUserId(),
                            User::liked_animal_ids,
                            m_selected_animal!!.animal_id
                        ) {
                            m_current_user?.liked_animal_ids?.add(m_selected_animal!!.animal_id)
                            instantiateSaveAnimalButton()
                        }
                    }
                }
            }

            val adopt_button = binding.adoptButton
            adopt_button.setOnClickListener {

                if (m_current_user?.adopting_animal_ids?.contains(m_selected_animal?.animal_id) == true) {
                    removeFromDataFieldList(
                        Strings.get(R.string.firebase_collection_users),
                        getCurrentUserId(),
                        User::adopting_animal_ids,
                        arrayOf(m_selected_animal!!.animal_id)
                    ) {
                        m_current_user?.adopting_animal_ids?.remove(m_selected_animal!!.animal_id)
                        instantiateAdoptButton()


                        Toast.makeText(
                            this@AnimalProfileViewingActivity,
                            "Withdrawing adoption interest. :(",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    appendToDataFieldArray(
                        Strings.get(R.string.firebase_collection_users),
                        getCurrentUserId(),
                        User::adopting_animal_ids,
                        m_selected_animal!!.animal_id
                    ) {
                        m_current_user?.adopting_animal_ids?.add(m_selected_animal!!.animal_id)
                        instantiateAdoptButton()

                        Toast.makeText(
                            this@AnimalProfileViewingActivity,
                            "Sending adoption interest! :)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun getAnimalAndExecuteCallback(animal_id: String?, onGetDataSuccess: (() -> Unit)? = null) {
        lifecycleScope.launch {
            try {
                m_selected_animal = getAnimalData(animal_id!!)
                if (m_selected_animal == null) {
                    Log.e("AnimalProfileViewingActivity", "Database queried animal returned null.")
                    //Display error message
                    Toast.makeText(
                        this@AnimalProfileViewingActivity,
                        "Cannot find animal: $animal_id",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    onGetDataSuccess?.invoke()
                }
            } catch (e: Exception) {
                Log.w(
                    "AnimalProfileViewingActivity",
                    "Error fetching animal $animal_id: ${e.message}"
                )
                // If db query fails, display a message to the user
                Toast.makeText(
                    this@AnimalProfileViewingActivity,
                    "Database Query error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    fun populateProfileImage(current_animal: Animal) {
        loadCloudStoredImageIntoImageView(
            this,
            current_animal.profile_image_path,
            binding.profileImageView
        )
    }

    fun populateTextViewsWithAnimalInfo(current_animal: Animal) {
        val animal_name_view = binding.animalName
        val animal_age_view = binding.animalAge
        val animal_health_view = binding.animalHealth
        val animal_description_view = binding.animalDescription

        animal_name_view.text = current_animal.animal_name

        animal_age_view.text = convertDoubleToYearsMonths(current_animal.animal_age!!)
        animal_health_view.text = current_animal.health_summary
        animal_description_view.text = current_animal.biography
    }

    fun convertDoubleToYearsMonths(age_in_years: Double): String {
        val years = age_in_years.toInt() // Extract whole years
        val months = ((age_in_years - years) * 12).toInt() // Convert decimal part to months

        val years_val_str = if (years != 0) years.toString() else ""
        val months_val_str =
            if (years_val_str != "" && months != 0) " $months" else if (months != 0) months.toString() else ""
        val years_str = if (years > 1) " years" else if (years == 1) " year" else ""
        val months_str = if (months > 1) " months" else if (months == 1) " month" else ""

        return years_val_str + years_str + months_val_str + months_str
    }

    fun initializeRecyclerViewAdapter(current_animal: Animal) {
        val animal_images_recycler_view = binding.additionalImages

        // Ensure smooth scrolling
        animal_images_recycler_view.setHasFixedSize(true)

        // Initialize recyclerview adaptor
        val clickability =
            if (m_selected_animal!!.associated_shelter_id == getCurrentUserId()) AdapterClickability.DOUBLE_CLICKABLE else AdapterClickability.NOT_CLICKABLE
        m_adapter =
            AnimalProfileViewingImagesAdapter(this, current_animal, clickability)
        animal_images_recycler_view.adapter = m_adapter
        animal_images_recycler_view.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val image_uris =
            ArrayList(m_selected_animal?.supplementary_image_paths?.map {
                Uri.parse(it)
            } ?: emptyList())
        m_adapter?.setItems(image_uris)

        initializeAddImageButton()
    }

    fun instantiateSaveAnimalButton() {
        val save_animal_button = binding.likeAnimalButton
        // If user already has animal saved, show heart with check mark, otherwise heart with plus
        if (m_current_user?.liked_animal_ids?.contains(m_selected_animal?.animal_id) == true) {
            save_animal_button.setImageResource(R.drawable.ic_heart_check)
        } else {
            save_animal_button.setImageResource(R.drawable.ic_heart_plus)
        }
    }

    fun instantiateAdoptButton() {
        val adopt_button = binding.adoptButton
        // If user already has animal saved, show heart with check mark, otherwise heart with plus
        if (m_current_user?.adopting_animal_ids?.contains(m_selected_animal?.animal_id) == true) {
            adopt_button.text = "Adopting..."
        } else {
            adopt_button.text = "Adopt Me!"
        }
    }

    fun initializeAddImageButton() {
        val add_image_button = binding.addImageButton

        add_image_button.setOnClickListener {
            select_additional_images_intent.launch("image/*")
        }
    }
}