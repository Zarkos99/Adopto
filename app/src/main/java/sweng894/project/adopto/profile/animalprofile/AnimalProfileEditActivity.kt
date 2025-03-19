package sweng894.project.adopto.profile.animalprofile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.removeAnimalFromDatabase
import sweng894.project.adopto.database.setDocumentData
import sweng894.project.adopto.databinding.AnimalProfileEditActivityBinding

class AnimalProfileEditActivity : AppCompatActivity() {

    private lateinit var m_name_input_field: EditText
    private lateinit var m_age_input_field: EditText
    private lateinit var m_health_input_field: EditText
    private lateinit var m_description_input_field: EditText
    private lateinit var m_type_input_field: Spinner
    private lateinit var m_size_input_field: Spinner
    private lateinit var m_breed_input_field: EditText
    private lateinit var m_save_button: Button
    private lateinit var m_delete_profile_button: Button

    lateinit var m_current_animal: Animal

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
        m_type_input_field = binding.animalTypeSpinner
        m_size_input_field = binding.animalSizeSpinner
        m_breed_input_field = binding.breedInputField
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
            val updated_animal = createAnimalFromInputs()

            if (updated_animal != null && m_current_animal != updated_animal) {
                setDocumentData(
                    Strings.get(R.string.firebase_collection_animals),
                    m_current_animal.animal_id,
                    updated_animal
                ) {
                    m_current_animal = updated_animal
                    disableButton(m_save_button)
                }
            } else if (updated_animal == null) {
                Log.d("TRACE", "Have empty fields when attempting to update animal.")
            }
        }

        m_delete_profile_button.setOnClickListener {
            removeAnimalFromDatabase(m_current_animal)
            val intent = Intent().apply {
                putExtra("updated_animal", m_current_animal)
            }
            setResult(RESULT_CANCELED, intent)
            finish()
        }
    }


    fun initializeInputFields() {
        m_name_input_field.setText(m_current_animal.animal_name)
        m_age_input_field.setText(m_current_animal.animal_age.toString())
        m_health_input_field.setText(m_current_animal.health_summary)
        m_description_input_field.setText(m_current_animal.biography)
        m_breed_input_field.setText(m_current_animal.animal_breed)

        var value_to_select = m_current_animal.animal_size
        if (value_to_select != null) {
            val adapter = m_size_input_field.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(value_to_select)
            m_size_input_field.setSelection(position)
        }

        value_to_select = m_current_animal.animal_type
        if (value_to_select != null) {
            val adapter = m_type_input_field.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(value_to_select)
            m_type_input_field.setSelection(position)
        }
    }

    fun createAnimalFromInputs(): Animal? {
        val new_name = m_name_input_field.text.toString()
        val new_age_str = m_age_input_field.text.toString() // Optional Field
        val new_health = m_health_input_field.text.toString() //Optional Field
        val new_description = m_description_input_field.text.toString() //Optional Field
        val new_size = m_size_input_field.selectedItem.toString()
        val new_type = m_type_input_field.selectedItem.toString()
        val new_breed = m_breed_input_field.text.toString() //Optional Field

        var error = false

        // Default to fake values if fields are empty
        if (new_name.isEmpty()) {
            Log.d("TRACE", "Animal Name cannot be empty.")
            error = true
        }
        if (new_size.isEmpty()) {
            Log.d("TRACE", "Animal Size cannot be empty.")
            error = true
        }
        if (new_type.isEmpty()) {
            Log.d("TRACE", "Animal Type cannot be empty.")
            error = true
        }

        var new_age = 0.0
        try {
            new_age = new_age_str.toDouble()
        } catch (e: Exception) {
            //Display error message
            Log.d("TRACE", "Animal Age is non-numeric.")
            Toast.makeText(
                this,
                "Invalid age input. Ensure age is numerical.",
                Toast.LENGTH_LONG
            ).show()
            error = true
        }

        if (error) {
            return null
        }

        val new_animal = m_current_animal.copy()
        new_animal.animal_name = new_name
        new_animal.animal_age = new_age
        new_animal.health_summary = new_health
        new_animal.biography = new_description
        new_animal.animal_size = new_size
        new_animal.animal_type = new_type
        new_animal.animal_breed = new_breed
        return new_animal
    }

    fun calculateSaveButtonClickability() {
        val updated_animal = createAnimalFromInputs()

        if (updated_animal != null && m_current_animal != updated_animal) {
            enableButton(m_save_button)
        } else {
            disableButton(m_save_button)
        }
    }

    fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        button.setTextColor(m_delete_profile_button.currentTextColor)
        button.background = m_delete_profile_button.background
    }

    fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        button.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
    }
}