package sweng894.project.adopto.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.AnimalProfileCreationActivity
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.*
import sweng894.project.adopto.databinding.ProfileFragmentBinding


class ProfileFragment : Fragment() {
    private var _binding: ProfileFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var m_sunset_list_adaptor: ProfileSavedAnimalsAdapter
    private lateinit var m_select_profile_image_intent: ActivityResultLauncher<String>
    private lateinit var m_profile_image_view: ImageView
    private lateinit var m_add_or_remove_sunset_button_view: Button

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataService

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataService.LocalBinder).getService()

            initializeRecyclerViewLayoutManager()
            initializeRecyclerViewAdapter()
            // Initial population of user info with existent data
            populateTextViewsWithUserInfo()
            populateProfileImage()

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                populateTextViewsWithUserInfo()
                m_sunset_list_adaptor.unselectDeletedAnimals()
                m_sunset_list_adaptor.notifyDataSetChanged()

                populateProfileImage()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private fun bindToFirebaseService() {
        Intent(activity, FirebaseDataService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromFirebaseService() {
        requireActivity().unbindService(connection)
    }

    /** End FirebaseDataService Setup **/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToFirebaseService()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Create intent to open device storage for image selection
        m_select_profile_image_intent =
            registerForActivityResult(ActivityResultContracts.GetContent())
            { uri ->
                if (uri != null) {
                    uploadUserProfileImage(m_firebase_data_service, uri)
                    m_profile_image_view.setImageURI(uri)
                }
            }

        m_profile_image_view = binding.profilePictureView
        val biography_input_field = binding.biographyInputField
        val save_biography_button = binding.saveBiographyButton
        disableButton(save_biography_button)

        // Profile Image selection listener
        m_profile_image_view.setOnClickListener {
            m_select_profile_image_intent.launch("image/*")
        }


        biography_input_field.doAfterTextChanged { new_biography_editable ->
            val new_biography = new_biography_editable.toString()

            if (::m_firebase_data_service.isInitialized) {
                if (m_firebase_data_service.current_user_data?.biography != new_biography) {
                    enableButton(save_biography_button)
                } else {
                    disableButton(save_biography_button)
                }
            }
        }

        save_biography_button.setOnClickListener {
            val new_biography = biography_input_field.text.toString()

            if (::m_firebase_data_service.isInitialized) {
                if (m_firebase_data_service.current_user_data?.biography != new_biography) {
                    updateDataField(
                        Strings.get(R.string.firebase_collection_users),
                        getCurrentUserId(),
                        User::biography,
                        new_biography
                    )
                }

                disableButton(save_biography_button)
            }
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        bindToFirebaseService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromFirebaseService()
    }

    fun initializeRecyclerViewAdapter() {
        m_add_or_remove_sunset_button_view = binding.addSunsetButton
        val sunsets_recycler_view = binding.savedAnimals
        // Initialize recyclerview adaptor
        m_sunset_list_adaptor =
            ProfileSavedAnimalsAdapter(requireContext(), m_firebase_data_service)
        m_sunset_list_adaptor.registerItemSelectedCallback {
            val selected_sunsets = m_sunset_list_adaptor.getSelectedAnimalIds()
            // If some sunsets are selected and button is pressed we want to show option to
            // delete them, else show option to add sunset
            if (selected_sunsets.size > 0) {
                m_add_or_remove_sunset_button_view.text = "-"
            } else {
                m_add_or_remove_sunset_button_view.text = "+"
            }
        }
        sunsets_recycler_view.adapter = m_sunset_list_adaptor


        // Setup listener for image upload button
        m_add_or_remove_sunset_button_view.setOnClickListener {
            val selected_sunsets = m_sunset_list_adaptor.getSelectedAnimalIds()
            if (selected_sunsets.size > 0) {
                //Change add sunset button to remove sunset functionality when sunsets are selected

//                removeFromDataFieldArray(
//                    Strings.get(R.string.firebase_collection_users),
//                    getCurrentUserId(),
//                    User::saved_animal_ids,
//                    selected_sunsets
//                )
            } else {
                val intent = Intent(activity, AnimalProfileCreationActivity::class.java)
                startActivity(intent)
                // Not calling finish() here so that SunsetPostCreationActivity will come back to this fragment
            }
        }
    }

    fun initializeRecyclerViewLayoutManager() {
        val sunsets_recycler_view = binding.savedAnimals
        // Initialize FlexBox Layout Manager for recyclerview to allow wrapping items to next line
        val layout_manager = FlexboxLayoutManager(requireContext())
        layout_manager.apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
            flexWrap = FlexWrap.WRAP
        }
        sunsets_recycler_view.layoutManager = layout_manager
    }

    /**
     * Dynamically obtains stored drawable images by name
     */
    private fun getImage(ImageName: String?): Drawable {
        return activity?.resources?.getDrawable(
            activity?.resources?.getIdentifier(
                ImageName,
                "drawable",
                activity?.packageName
            )!!
        )!!
    }

    fun populateProfileImage() {
        val current_user_data = m_firebase_data_service.current_user_data
        if (current_user_data?.profile_image_path.isNullOrEmpty()) {
            m_profile_image_view.setImageDrawable(getImage("default_profile_pic"))
        } else {
            if (context != null) {
                loadCloudStoredImageIntoImageView(
                    requireContext(),
                    current_user_data?.profile_image_path,
                    m_profile_image_view
                )
            }
        }
    }

    fun populateTextViewsWithUserInfo() {
        val current_user = FirebaseAuth.getInstance().currentUser
        val current_database_user_info = m_firebase_data_service.current_user_data
        val public_username_text_view = binding.publicUsername
        val biography_text_view = binding.biographyInputField

        public_username_text_view.text =
            if (!current_user?.displayName.isNullOrEmpty()) current_user?.displayName else current_user?.email
        biography_text_view.setText(current_database_user_info?.biography)
    }

    private fun enableButton(button: Button) {
        button.isEnabled = true
        button.isClickable = true
        if (context != null) {
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.background = m_add_or_remove_sunset_button_view.background
        }
    }

    private fun disableButton(button: Button) {
        button.isEnabled = false
        button.isClickable = false
        if (context != null) {
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grey))
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey))
        }
    }
}