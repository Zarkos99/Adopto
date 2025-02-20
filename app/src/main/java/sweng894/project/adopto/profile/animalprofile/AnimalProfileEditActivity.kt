package sweng894.project.adopto.profile.animalprofile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.removeAnimalFromDatabase
import sweng894.project.adopto.database.updateDataField
import sweng894.project.adopto.databinding.AnimalProfileEditActivityBinding

class AnimalProfileEditActivity : AppCompatActivity() {

    lateinit var m_name_input_field: EditText
    lateinit var m_age_input_field: EditText
    lateinit var m_health_input_field: EditText
    lateinit var m_description_input_field: EditText
    lateinit var m_save_button: Button
    lateinit var m_delete_profile_button: Button

    lateinit var m_current_animal: Animal
    private var m_is_hosting_shelter: Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileEditActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnimalProfileEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_name_input_field = binding.nameInputField
        m_age_input_field = binding.ageInputField
        m_health_input_field = binding.healthInputField
        m_description_input_field = binding.descriptionInputField
        m_delete_profile_button = binding.deleteProfileButton
        m_save_button = binding.savePreferencesButton
        disableButton(m_save_button)

        m_current_animal = intent.getParcelableExtra("current_animal")!!
        initializeInputFields()

        // Register a back press callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent().apply {
                    putExtra("updated_animal", m_current_animal)
                }
                setResult(RESULT_OK, intent) // Send result
                finish()
            }
        })

        m_name_input_field.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        m_age_input_field.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        m_health_input_field.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        m_description_input_field.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }

        m_save_button.setOnClickListener {
            val new_name = m_name_input_field.text.toString()
            var new_age = 0.0
            try {
                new_age = m_age_input_field.text.toString().toDouble()
            } catch (e: Exception) {
                //Display error message
                Toast.makeText(
                    this,
                    "Invalid age input. Ensure age is numerical.",
                    Toast.LENGTH_LONG
                ).show()
            }
            val new_health = m_health_input_field.text.toString()
            val new_description = m_description_input_field.text.toString()

            var new_animal = m_current_animal

            if (m_current_animal.animal_name != new_name) {
                updateDataField(
                    Strings.get(R.string.firebase_collection_animals),
                    m_current_animal.animal_id,
                    Animal::animal_name,
                    new_name
                )
                m_current_animal.animal_name = new_name
            }
            if (m_current_animal.animal_age != new_age) {
                updateDataField(
                    Strings.get(R.string.firebase_collection_animals),
                    m_current_animal.animal_id,
                    Animal::animal_age,
                    new_age
                )
                m_current_animal.animal_age = new_age
            }
            if (m_current_animal.health_summary != new_health) {
                updateDataField(
                    Strings.get(R.string.firebase_collection_animals),
                    m_current_animal.animal_id,
                    Animal::health_summary,
                    new_health
                )
                m_current_animal.health_summary = new_health
            }
            if (m_current_animal.biography != new_description) {
                updateDataField(
                    Strings.get(R.string.firebase_collection_animals),
                    m_current_animal.animal_id,
                    Animal::biography,
                    new_description
                )
                m_current_animal.biography = new_description
            }

            disableButton(m_save_button)
        }

        m_delete_profile_button.setOnClickListener {
            removeAnimalFromDatabase(m_current_animal)
            val intent = Intent().apply {
                putExtra("updated_animal", m_current_animal)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }


    fun initializeInputFields() {
        m_name_input_field.setText(m_current_animal.animal_name)
        m_age_input_field.setText(m_current_animal.animal_age.toString())
        m_health_input_field.setText(m_current_animal.health_summary)
        m_description_input_field.setText(m_current_animal.biography)
    }

    fun calculateSaveButtonClickability() {
        val new_name = m_name_input_field.text.toString()
        var new_age = 0.0
        try {
            new_age = m_age_input_field.text.toString().toDouble()
        } catch (e: Exception) {
            //Display error message
            Toast.makeText(
                this,
                "Invalid age input. Ensure age is numerical.",
                Toast.LENGTH_LONG
            ).show()
        }
        val new_health = m_health_input_field.text.toString()
        val new_description = m_description_input_field.text.toString()

        if (m_current_animal.animal_name != new_name
            || m_current_animal.animal_age != new_age
            || m_current_animal.health_summary != new_health
            || m_current_animal.biography != new_description
        ) {
            enableButton(m_save_button)
        } else {
            disableButton(m_save_button)
        }
    }

    fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        button.setTextColor(ContextCompat.getColor(this, R.color.black))
        button.background = m_delete_profile_button.background
    }

    fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        button.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
    }
}