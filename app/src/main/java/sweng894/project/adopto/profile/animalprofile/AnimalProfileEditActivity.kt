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
import sweng894.project.adopto.custom.CustomSpinnerAdapter
import sweng894.project.adopto.custom.StringInputView
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.AnimalSizes
import sweng894.project.adopto.data.AnimalTypes
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.database.removeAnimalFromDatabase
import sweng894.project.adopto.database.setDocumentData
import sweng894.project.adopto.databinding.AnimalProfileEditActivityBinding

class AnimalProfileEditActivity : AppCompatActivity() {

    lateinit var m_current_animal: Animal

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileEditActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnimalProfileEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        disableButton(binding.savePreferencesButton)

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

        binding.animalNameInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        binding.animalAgeInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        binding.animalHealthInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        binding.animalDescriptionInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }

        binding.savePreferencesButton.setOnClickListener {
            val updated_animal = createAnimalFromInputs()

            if (updated_animal != null && m_current_animal != updated_animal) {
                setDocumentData(
                    FirebaseCollections.ANIMALS,
                    m_current_animal.animal_id,
                    updated_animal
                ) {
                    m_current_animal = updated_animal
                    disableButton(binding.savePreferencesButton)
                }
            } else if (updated_animal == null) {
                Log.d("TRACE", "Have empty fields when attempting to update animal.")
            }
        }

        binding.deleteProfileButton.setOnClickListener {
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            dialogBuilder.setTitle("Confirm Deletion")
            dialogBuilder.setMessage("Are you sure you want to delete this animal profile? This action cannot be undone.")
            dialogBuilder.setPositiveButton("Delete") { _, _ ->
                removeAnimalFromDatabase(m_current_animal)
                val intent = Intent().apply {
                    putExtra("updated_animal", m_current_animal)
                }
                setResult(RESULT_CANCELED, intent)
                finish()
            }
            dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            dialogBuilder.create().show()
        }
    }


    fun initializeInputFields() {
        binding.animalNameInput.setInputText(m_current_animal.animal_name ?: "")
        binding.animalAgeInput.setInputText(m_current_animal.animal_age.toString())
        binding.animalHealthInput.setInputText(m_current_animal.health_summary ?: "")
        binding.animalDescriptionInput.setInputText(m_current_animal.biography ?: "")
        binding.animalBreedInput.setInputText(m_current_animal.animal_breed ?: "")

        val type_adapter = populateSpinnerWithOptions(binding.animalTypeSpinner, AnimalTypes.all)
        val size_adapter = populateSpinnerWithOptions(binding.animalSizeSpinner, AnimalSizes.all)

        // Set selections using the saved values
        m_current_animal.animal_size?.let {
            val position = size_adapter.getPosition(it)
            binding.animalSizeSpinner.setSelection(position)
        }

        m_current_animal.animal_type?.let {
            val position = type_adapter.getPosition(it)
            binding.animalTypeSpinner.setSelection(position)
        }
    }

    fun populateSpinnerWithOptions(spinner: Spinner, options: List<String>): CustomSpinnerAdapter {
        val adapter = CustomSpinnerAdapter(
            this,
            options
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinner.adapter = adapter
        return adapter
    }

    fun createAnimalFromInputs(): Animal? {
        val new_name = binding.animalNameInput.getInputText()
        val new_age_str = binding.animalAgeInput.getInputText() // Optional Field
        val new_health = binding.animalHealthInput.getInputText() //Optional Field
        val new_breed = binding.animalBreedInput.getInputText() //Optional Field
        val new_description = binding.animalDescriptionInput.getInputText() //Optional Field
        val new_size = binding.animalSizeSpinner.selectedItem.toString()
        val new_type = binding.animalTypeSpinner.selectedItem.toString()

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
            enableButton(binding.savePreferencesButton)
        } else {
            disableButton(binding.savePreferencesButton)
        }
    }

    fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        button.setTextColor(binding.deleteProfileButton.currentTextColor)
        button.background = binding.deleteProfileButton.background
    }

    fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        button.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
    }
}