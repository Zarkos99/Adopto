package sweng894.project.adopto.profile

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.androidbuts.multispinnerfilter.KeyPairBoolData
import com.androidbuts.multispinnerfilter.MultiSpinnerSearch
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.ExplorationPreferences
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.updateDataField
import sweng894.project.adopto.databinding.ExplorePreferencesActivityBinding


class ExplorePreferencesActivity : AppCompatActivity() {

    private var m_user_explore_preferences: ExplorationPreferences? = null
    private var m_selected_sizes = arrayListOf<String>()
    private var m_selected_types = arrayListOf<String>()
    private lateinit var m_save_preferences_button: Button

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: ExplorePreferencesActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePreferencesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_user_explore_preferences = intent.getParcelableExtra("explore_preferences")!!

        initializeAgeSlider()
        initializeSpinners()

        m_save_preferences_button = binding.savePreferencesButton
        disableButton(m_save_preferences_button)
        m_save_preferences_button.setOnClickListener {
            updateDataField(
                Strings.get(R.string.firebase_collection_users),
                getCurrentUserId(),
                User::explore_preferences,
                getExplorationPreferencesFromInputs()
            )
            disableButton(m_save_preferences_button)
            finish()
        }
    }

    fun initializeAgeSlider() {
        val age_slider = binding.ageRangeSlider
        age_slider.setValues(
            m_user_explore_preferences?.min_animal_age?.toFloat(),
            m_user_explore_preferences?.max_animal_age?.toFloat()
        )
        age_slider.addOnChangeListener { _, _, _ -> calculateSaveButtonClickability() }
    }

    fun initializeSpinners() {
        val sizes_multi_select_spinner = binding.sizesMultiSelectSpinner
        val types_multi_select_spinner = binding.typesMultiSelectSpinner

        setupMultiSelectSpinner(
            sizes_multi_select_spinner,
            resources.getStringArray(R.array.animal_sizes).asList(),
            m_selected_sizes,
            m_user_explore_preferences?.animal_sizes
        )

        setupMultiSelectSpinner(
            types_multi_select_spinner,
            resources.getStringArray(R.array.animal_types).asList(),
            m_selected_types,
            m_user_explore_preferences?.animal_types
        )
    }

    private fun setupMultiSelectSpinner(
        spinner: MultiSpinnerSearch,
        items: List<String>,
        selected_items: MutableList<String>,
        user_preferences: List<String>?
    ) {
        val itemsList = items.mapIndexed { index, item ->
            KeyPairBoolData().apply {
                id = index.toLong() + 1
                name = item
                isSelected = user_preferences?.contains(item) == true
            }
        }

        spinner.setItems(itemsList) { selected ->
            selected_items.clear()
            selected_items.addAll(selected.filter { it.isSelected }.map { it.name })
            calculateSaveButtonClickability()
        }
    }


    fun getExplorationPreferencesFromInputs(): ExplorationPreferences {
        val explore_preferences =
            ExplorationPreferences(
                min_animal_age = binding.ageRangeSlider.values[0].toDouble(),
                max_animal_age = binding.ageRangeSlider.values[1].toDouble(),
                animal_sizes = m_selected_sizes,
                animal_types = m_selected_types
            )
        return explore_preferences
    }

    fun calculateSaveButtonClickability() {
        val new_explore_preferences = getExplorationPreferencesFromInputs()

        if (new_explore_preferences != m_user_explore_preferences) {
            enableButton(m_save_preferences_button)
        } else {
            disableButton(m_save_preferences_button)
        }
    }

    fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        button.setTextColor(ContextCompat.getColor(this, R.color.secondary_text))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_button))
    }

    fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        button.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
    }
}