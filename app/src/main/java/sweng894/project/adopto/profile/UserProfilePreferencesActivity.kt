package sweng894.project.adopto.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import sweng894.project.adopto.R
import sweng894.project.adopto.authentication.AuthenticationActivity
import sweng894.project.adopto.custom.PlacesAutocompleteHelper
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.geoPointToFormattedAddress
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.handleActivityResult
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.latLngToFormattedAddress
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.FirebaseDataServiceUsers
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.updateDataField
import sweng894.project.adopto.firebase.updateUserDisplayName
import sweng894.project.adopto.databinding.UserProfilePreferencesActivityBinding

class UserProfilePreferencesActivity(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) :
    AppCompatActivity() {

    private var m_location: GeoPoint? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: UserProfilePreferencesActivityBinding

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()

            initializeLocationTextView()
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
        binding = UserProfilePreferencesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        disableButton(binding.savePreferencesButton)

        initializeInputFields()

        binding.userEmailInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }
        binding.userDisplayNameInput.doAfterTextChanged { _ ->
            calculateSaveButtonClickability()
        }

        binding.savePreferencesButton.setOnClickListener {
            val new_display_name = binding.userDisplayNameInput.getInputText()
            val new_email = binding.userEmailInput.getInputText()

            val current_auth_user = auth.currentUser
            if (current_auth_user?.displayName != new_display_name) {
                updateUserDisplayName(new_display_name)
            }
            if (current_auth_user?.email != new_email) {
                current_auth_user?.verifyBeforeUpdateEmail(new_email)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("UserProfileCreationActivity", "New email successfully verified")
                        } else {
                            Log.e("UserProfileCreationActivity", "New email failed to verify")
                        }
                    }
            }

            updateDataField(
                FirebaseCollections.USERS,
                getCurrentUserId(),
                User::location,
                m_location
            )


            disableButton(binding.savePreferencesButton)
        }

        binding.logoutButton.setOnClickListener {
            // Log out user and open authentication activity
            auth.signOut()
            val intent =
                Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
            finishAffinity(); // Closes all activities in the current task
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivityResult(requestCode, resultCode, data)
    }

    fun initializeInputFields() {
        val current_auth_user = auth.currentUser
        binding.userDisplayNameInput.setInputText(current_auth_user?.displayName ?: "")
        binding.userEmailInput.setInputText(current_auth_user?.email ?: "")
    }


    fun initializeLocationTextView() {
        m_location =
            m_firebase_data_service.current_user_data?.location
        if (m_location != null) {
            binding.userLocationInput.setInputText(
                geoPointToFormattedAddress(this, m_location!!) ?: ""
            )
        }

        val location_text_field = binding.userLocationInput
        location_text_field.setOnClickListener {
            PlacesAutocompleteHelper.launchFromActivity(this) { place ->
                val lat_lng = place.latLng
                if (lat_lng != null) {
                    val formatted_location = latLngToFormattedAddress(this, lat_lng)

                    if (!formatted_location.isNullOrEmpty()) {
                        location_text_field.setInputText(formatted_location)
                        m_location = GeoPoint(lat_lng.latitude, lat_lng.longitude)
                        calculateSaveButtonClickability()
                    }
                }
            }
        }
    }

    fun calculateSaveButtonClickability() {
        val current_user_location = m_firebase_data_service.current_user_data?.location
        val current_auth_user = auth.currentUser
        val new_display_name = binding.userDisplayNameInput.getInputText()
        val new_email = binding.userEmailInput.getInputText()

        val display_name_changed = current_auth_user?.displayName != new_display_name
                || m_firebase_data_service.current_user_data?.display_name != new_display_name
        val email_changed = current_auth_user?.email != new_email
        val location_changed = current_user_location != m_location

        if (display_name_changed || email_changed || location_changed) {
            enableButton(binding.savePreferencesButton)
        } else {
            disableButton(binding.savePreferencesButton)
        }

    }

    fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        button.setTextColor(ContextCompat.getColor(this, R.color.black))
        button.background = binding.logoutButton.background
    }

    fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        button.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
    }
}