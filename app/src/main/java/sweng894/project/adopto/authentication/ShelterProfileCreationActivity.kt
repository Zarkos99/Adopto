package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.ExplorationPreferences
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.updateDataField
import sweng894.project.adopto.database.updateExplorePreferencesField
import sweng894.project.adopto.databinding.AuthShelterProfileCreationActivityBinding


class ShelterProfileCreationActivity : AppCompatActivity() {

    private lateinit var m_biography_edit_text: EditText
    private lateinit var m_zip_code_edit_text: EditText

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AuthShelterProfileCreationActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthShelterProfileCreationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_biography_edit_text = binding.biographyField
        m_zip_code_edit_text = binding.zipCodeField
        val done_button_view = binding.doneButton

        done_button_view.setOnClickListener {
            updateDataField(
                Strings.get(R.string.firebase_collection_users),
                getCurrentUserId(),
                User::is_shelter,
                true
            )

            // Save biography to database
            updateDataField(
                Strings.get(R.string.firebase_collection_users),
                getCurrentUserId(),
                User::biography,
                m_biography_edit_text.text.toString()
            )
            // Save zip code to database
            updateDataField(
                Strings.get(R.string.firebase_collection_users),
                getCurrentUserId(),
                User::zip_code,
                m_zip_code_edit_text.text.toString()
            )

            // No longer require additional info
            updateDataField(
                Strings.get(R.string.firebase_collection_users),
                getCurrentUserId(),
                User::need_info,
                false
            )

            val animal_types = resources.getStringArray(R.array.animal_types)
            val animal_sizes = resources.getStringArray(R.array.animal_sizes)
            updateExplorePreferencesField(ExplorationPreferences::animal_types, animal_types)
            updateExplorePreferencesField(ExplorationPreferences::animal_sizes, animal_sizes)

            openActivity(NavigationBaseActivity::class.java)
        }
    }

    private fun openActivity(activity_class: Class<out AppCompatActivity>) {
        val intent = Intent(this, activity_class)
        startActivity(intent)
        finish()
    }
}