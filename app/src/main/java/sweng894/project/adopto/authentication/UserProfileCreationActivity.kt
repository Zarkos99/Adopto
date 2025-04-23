package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.GeoPoint
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.custom.PlacesAutocompleteHelper
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.handleActivityResult
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.latLngToFormattedAddress
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.AnimalSizes
import sweng894.project.adopto.data.AnimalTypes
import sweng894.project.adopto.data.ExplorationPreferences
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.fetchAnimalsByShelter
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.updateDataField
import sweng894.project.adopto.firebase.updateExplorePreferencesField
import sweng894.project.adopto.databinding.AuthUserProfileCreationActivityBinding


class UserProfileCreationActivity : AppCompatActivity() {

    private var m_is_shelter = false
    private var m_location: GeoPoint? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AuthUserProfileCreationActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthUserProfileCreationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_is_shelter = intent.getBooleanExtra("is_shelter", false)

        val done_button_view = binding.doneButton
        val description_input_field = binding.userDescriptionInput
        // TODO: Add a displayName input field

        initializeLocationTextView()

        done_button_view.setOnClickListener {
            updateDataField(
                FirebaseCollections.USERS,
                getCurrentUserId(),
                User::is_shelter,
                m_is_shelter
            )

            // Save biography to database
            updateDataField(
                FirebaseCollections.USERS,
                getCurrentUserId(),
                User::biography,
                description_input_field.getInputText()
            )
            // Save zip code to database
            updateDataField(
                FirebaseCollections.USERS,
                getCurrentUserId(),
                User::location,
                m_location
            )

            // No longer require additional info
            updateDataField(
                FirebaseCollections.USERS,
                getCurrentUserId(),
                User::need_info,
                false
            )

            //Set as lists because Parcelable doesn't support arrays
            updateExplorePreferencesField(ExplorationPreferences::animal_types, AnimalTypes.all)
            updateExplorePreferencesField(ExplorationPreferences::animal_sizes, AnimalSizes.all)

            // Reassociate animals with the current shelter's id if the shelter's data ever accidentally gets cleared
            reassociateUnlinkedAnimalsToShelter()

            openActivity(NavigationBaseActivity::class.java)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivityResult(requestCode, resultCode, data)
    }

    fun initializeLocationTextView() {
        val location_input_field = binding.userLocationInput
        location_input_field.setOnClickListener {
            PlacesAutocompleteHelper.launchFromActivity(this) { place ->
                val lat_lng = place.latLng
                if (lat_lng != null) {
                    val formatted_location = latLngToFormattedAddress(this, lat_lng)

                    if (!formatted_location.isNullOrEmpty()) {
                        location_input_field.setInputText(formatted_location)
                        m_location = GeoPoint(lat_lng.latitude, lat_lng.longitude)
                    }
                }
            }
        }
    }

    private fun reassociateUnlinkedAnimalsToShelter() {
        fetchAnimalsByShelter(
            getCurrentUserId(),
            onSuccess = { animal_list: List<Animal> ->
                val hosted_animal_ids = animal_list.map { it.animal_id }

                updateDataField(
                    FirebaseCollections.USERS,
                    getCurrentUserId(),
                    User::hosted_animal_ids,
                    hosted_animal_ids
                )
            })
    }

    private fun openActivity(activity_class: Class<out AppCompatActivity>) {
        val intent = Intent(this, activity_class)
        startActivity(intent)
        finish()
    }
}