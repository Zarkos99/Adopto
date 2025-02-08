package sweng894.project.adopto.profile

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import sweng894.project.adopto.R
import sweng894.project.adopto.databinding.AnimalProfileCreationLayoutBinding
import sweng894.project.adopto.databinding.AuthNewAccountInfoLayoutBinding


class AnimalProfileCreationActivity : AppCompatActivity() {

    private lateinit var m_name_input_edit_text: EditText
    private lateinit var m_age_input_edit_text: EditText
    private lateinit var m_health_input_edit_text: EditText
    private lateinit var m_description_input_edit_text: EditText
    private lateinit var m_select_image_intent: ActivityResultLauncher<String>
    private lateinit var m_new_image_uri: Uri

    private var m_image_updated = false

    private val FAKE_NAME = "Tofu"
    private val FAKE_AGE = "1 Year"
    private val FAKE_HEALTH = "Healthy"
    private val FAKE_DESCRIPTION = "What a cute little animal!"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileCreationLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnimalProfileCreationLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_name_input_edit_text = binding.animalNameInputField
        m_age_input_edit_text = binding.animalAgeInputField
        m_health_input_edit_text = binding.animalHealthInputField
        m_description_input_edit_text = binding.descriptionInputField
        val animal_profile_image_view = binding.profileImageView
        val cancel_button_view = binding.cancelButton
        val create_button_view = binding.createButton

        initializeEditTexts()

        m_select_image_intent = registerForActivityResult(ActivityResultContracts.GetContent())
        { uri ->
            if (uri != null) {
                animal_profile_image_view.setImageURI(uri)
                m_new_image_uri = uri
                m_image_updated = true
            }
        }

        animal_profile_image_view.setOnClickListener {
            m_select_image_intent.launch("image/*")
        }

        cancel_button_view.setOnClickListener {
            finish()
        }

        create_button_view.setOnClickListener {
            handleCreate()
        }
    }

    fun handleCreate() {
        var new_name = m_name_input_edit_text.text.toString().trim()
        var new_age = m_age_input_edit_text.text.toString().trim()
        var new_health = m_health_input_edit_text.text.toString().trim()
        var new_description = m_description_input_edit_text.text.toString().trim()

        var error = false

        // Default to fake values if fields are empty
        if (new_name.isEmpty()) {
            new_name = FAKE_NAME
        }
        if (new_age.isEmpty()) {
            new_age = FAKE_AGE
        }
        if (new_health.isEmpty()) {
            new_health = FAKE_HEALTH
        }
        if (new_description.isEmpty()) {
            new_description = FAKE_DESCRIPTION
        }

        if (!m_image_updated) {
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

        //Create SunsetPost with real or fake data if empty
//        val sunset_data = SunsetData(
//            title = new_title,
//            latitude = new_latitude,
//            longitude = new_longitude,
//            post_time = DateTimeFormatter.ISO_INSTANT.format(
//                Instant.now()
//            ),
//            description = new_description
//        )
//
//        uploadImageAndCreateNewPost(sunset_data, m_new_image_uri)
        finish()
    }

    fun initializeEditTexts() {
        m_name_input_edit_text.hint = FAKE_NAME
        m_age_input_edit_text.hint = FAKE_AGE
        m_health_input_edit_text.hint = FAKE_HEALTH
        m_description_input_edit_text.hint = FAKE_DESCRIPTION
    }
}