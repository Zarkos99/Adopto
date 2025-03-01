package sweng894.project.adopto.profile

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.custom.MultiSelectDialogFragment
import sweng894.project.adopto.custom.MultiSelectView
import sweng894.project.adopto.data.ExplorationPreferences
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.updateDataField
import sweng894.project.adopto.databinding.ExplorePreferencesActivityBinding


class ExplorePreferencesActivity : AppCompatActivity(), MultiSelectView.OnSelectionChangeListener {

    private var m_user_explore_preferences: ExplorationPreferences? = null
    private var m_selected_sizes = arrayListOf<String>()
    private var m_selected_types = arrayListOf<String>()
    private lateinit var m_save_preferences_button: Button
    private lateinit var m_sizes_multi_select_view: MultiSelectView
    private lateinit var m_types_multi_select_view: MultiSelectView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: ExplorePreferencesActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExplorePreferencesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_user_explore_preferences = intent.getParcelableExtra("explore_preferences")!!

        initializeAgeSlider()
        setupMultiSelectListeners()

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

    override fun onSelectionChanged(selectedItems: List<String>) {
        Log.d("CALLBACK", "Selection Changed: $selectedItems")
        m_selected_sizes = ArrayList(m_sizes_multi_select_view.getSelectedItems())
        m_selected_types = ArrayList(m_types_multi_select_view.getSelectedItems())
        calculateSaveButtonClickability() // Update button state when selection changes
    }

    private fun setupMultiSelectListeners() {
        m_sizes_multi_select_view = binding.sizesMultiSelect
        m_types_multi_select_view = binding.typesMultiSelect

        // Set dropdown options
        val sizes_array = resources.getStringArray(R.array.animal_sizes).toList()
        val types_array = resources.getStringArray(R.array.animal_types).toList()
        m_sizes_multi_select_view.setOptions(sizes_array)
        m_types_multi_select_view.setOptions(types_array)

        // Set pre-selected preferences
        val selected_sizes = m_user_explore_preferences?.animal_sizes ?: emptyList()
        val selected_types = m_user_explore_preferences?.animal_types ?: emptyList()
        m_sizes_multi_select_view.setSelectedItems(selected_sizes)
        m_types_multi_select_view.setSelectedItems(selected_types)
        m_selected_sizes = ArrayList(selected_sizes)
        m_selected_types = ArrayList(selected_types)


        // Set the listener to receive callbacks when selections change
        m_sizes_multi_select_view.setOnSelectionChangeListener(this)
        m_types_multi_select_view.setOnSelectionChangeListener(this)
    }


    fun initializeAgeSlider() {
        val age_slider = binding.ageRangeSlider
        age_slider.setValues(
            m_user_explore_preferences?.min_animal_age?.toFloat(),
            m_user_explore_preferences?.max_animal_age?.toFloat()
        )
        age_slider.addOnChangeListener { _, _, _ -> calculateSaveButtonClickability() }
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