package sweng894.project.adopto.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.*
import sweng894.project.adopto.databinding.UserProfileFragmentBinding
import sweng894.project.adopto.profile.Tabs.ProfileTabType
import sweng894.project.adopto.profile.Tabs.ProfileTabAdapter
import sweng894.project.adopto.profile.Tabs.RefreshableTab
import sweng894.project.adopto.profile.animalprofile.AnimalProfileCreationActivity


class UserProfileFragment : Fragment() {
    private var _binding: UserProfileFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var m_select_profile_image_intent: ActivityResultLauncher<String>
    private lateinit var m_profile_image_view: ImageView
    private lateinit var m_add_animal_button_view: Button
    private var is_tab_layout_initialized = false
    private var user_biography: String = ""

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers
    private var is_firebase_service_bound = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            is_firebase_service_bound = true
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()

            populateProfileImage()
            setupBiographyAndAssociatedButtons()

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                initializeAddAnimalButton()
                initializeTabLayout()
                // Initial population of user info with existent data
                populateTextViewsWithUserInfo()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            is_firebase_service_bound = false
        }
    }

    private fun bindToFirebaseService() {
        Intent(activity, FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromFirebaseService() {
        requireActivity().unbindService(connection)
    }

    override fun onStart() {
        super.onStart()
        bindToFirebaseService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromFirebaseService()
    }

    /** End FirebaseDataService Setup **/

    override fun onResume() {
        super.onResume()

        if (is_firebase_service_bound) {
            initializeAddAnimalButton()
            initializeTabLayout()
            // Initial population of user info with existent data
            populateTextViewsWithUserInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToFirebaseService()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserProfileFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val preferences_button_view = binding.preferencesButton

        preferences_button_view.setOnClickListener {
            val intent = Intent(activity, UserProfilePreferencesActivity::class.java)
            startActivity(intent)
            // Not calling finish() here so that AnimalProfileCreationActivity will come back to this fragment)
        }
        // Create intent to open device storage for image selection
        m_select_profile_image_intent =
            registerForActivityResult(ActivityResultContracts.GetContent())
            { uri ->
                if (uri != null) {
                    uploadUserProfileImageAndUpdateUserImagePath(m_firebase_data_service, uri)
                    m_profile_image_view.setImageURI(uri)
                }
            }

        m_profile_image_view = binding.profilePictureView

        // Profile Image selection listener
        m_profile_image_view.setOnClickListener {
            m_select_profile_image_intent.launch("image/*")
        }

        return root
    }

    fun initializeAddAnimalButton() {
        val is_user_a_shelter = m_firebase_data_service.current_user_data?.is_shelter

        m_add_animal_button_view = binding.addAnimalButton

        // Ensure only shelters have the option to add animals
        m_add_animal_button_view.visibility =
            if (is_user_a_shelter == true) View.VISIBLE else View.GONE

        if (m_add_animal_button_view.visibility != View.GONE) {
            // Setup listener for image upload button
            m_add_animal_button_view.setOnClickListener {
                val intent = Intent(activity, AnimalProfileCreationActivity::class.java)
                startActivity(intent)
                // Not calling finish() here so that AnimalProfileCreationActivity will come back to this fragment
            }
        }
    }

    fun initializeTabLayout() {
        if (!is_firebase_service_bound || is_tab_layout_initialized) return // Ensure Firebase is ready and prevent re-initialization
        is_tab_layout_initialized = true

        val tab_layout = binding.tabLayout
        val view_pager = binding.viewPager

        val is_shelter = m_firebase_data_service.current_user_data?.is_shelter ?: false

        val visible_tabs = mutableListOf(
            ProfileTabType.LIKED,
            ProfileTabType.ADOPTING
        )
        if (is_shelter) {
            visible_tabs.add(ProfileTabType.HOSTED)
        }

        val tab_adapter = ProfileTabAdapter(requireActivity(), visible_tabs)
        view_pager.adapter = tab_adapter
        view_pager.offscreenPageLimit = 1 // Ensures fragments are refreshed when switched

        view_pager.adapter = tab_adapter

        // Sync TabLayout with ViewPager2
        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = when (tab_adapter.getTabTypeAt(position)) {
                ProfileTabType.LIKED -> Strings.get(R.string.liked_animals_tab_name)
                ProfileTabType.ADOPTING -> Strings.get(R.string.adopting_animals_tab_name)
                ProfileTabType.HOSTED -> Strings.get(R.string.hosted_animals_tab_name)
                else -> "Unknown"
            }
        }.attach()

        // Listen for tab selection changes
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                val fragment = tab_adapter.getFragmentAt(position)
                if (fragment is RefreshableTab) {
                    fragment.refreshTabContent()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                val fragment = tab_adapter.getFragmentAt(position)
                if (fragment is RefreshableTab) {
                    fragment.refreshTabContent()
                }
            }
        })
    }

    /**
     * Dynamically obtains stored drawable images by name.
     * Ensures that the activity is available and the image exists.
     */
    private fun getImage(image_name: String): Drawable? {
        val activity = activity ?: return null // Ensure activity is not null
        val resources = activity.resources

        val imageId = resources.getIdentifier(image_name, "drawable", activity.packageName)

        return if (imageId != 0) {
            ResourcesCompat.getDrawable(resources, imageId, activity.theme)
        } else {
            Log.e("getImage", "Drawable not found: $image_name, using default image")
            ResourcesCompat.getDrawable(resources, R.drawable.default_profile_image, activity.theme)
        }
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
        val public_username_text_view = binding.publicUsername

        public_username_text_view.text =
            if (!current_user?.displayName.isNullOrEmpty()) current_user?.displayName else current_user?.email
        public_username_text_view.isSelected = true // Required for marquee text to function
    }

    fun setupBiographyAndAssociatedButtons() {
        val bio_edit_text = binding.biographyField
        val edit_bio_button = binding.editBioButton
        val save_bio_button = binding.saveBioButton
        val cancel_bio_button = binding.cancelEditBioButton
        val button_group = binding.bioEditButtonGroup
        val current_database_user_info = m_firebase_data_service.current_user_data

        user_biography = current_database_user_info?.biography ?: ""

        bio_edit_text.setText(user_biography)
        bio_edit_text.isEnabled = false
        edit_bio_button.visibility = View.VISIBLE
        button_group.visibility = View.GONE

        edit_bio_button.setOnClickListener {
            bio_edit_text.isEnabled = true
            bio_edit_text.requestFocus()
            bio_edit_text.setSelection(bio_edit_text.length()) // Move cursor to end
            edit_bio_button.visibility = View.GONE
            button_group.visibility = View.VISIBLE
        }

        cancel_bio_button.setOnClickListener {
            bio_edit_text.setText(user_biography)
            bio_edit_text.isEnabled = false
            edit_bio_button.visibility = View.VISIBLE
            button_group.visibility = View.GONE
        }

        save_bio_button.setOnClickListener {
            val updated_bio = bio_edit_text.text.toString()

            val current_user_id = getCurrentUserId()
            if (!current_user_id.isNullOrEmpty()) {
                updateDataField(
                    FirebaseCollections.USERS,
                    current_user_id,
                    User::biography,
                    updated_bio
                )
            }

            user_biography = updated_bio

            bio_edit_text.isEnabled = false
            edit_bio_button.visibility = View.VISIBLE
            button_group.visibility = View.GONE
        }
    }
}