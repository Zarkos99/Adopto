package sweng894.project.adopto.profile

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.*
import sweng894.project.adopto.databinding.UserProfileViewingActivityBinding
import sweng894.project.adopto.profile.Tabs.ProfileTabAdapter
import sweng894.project.adopto.profile.Tabs.ProfileTabType
import sweng894.project.adopto.profile.Tabs.RefreshableTab


class UserProfileViewingActivity : AppCompatActivity() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: UserProfileViewingActivityBinding

    private var m_displayed_user: User? = null
    private var is_tab_layout_initialized = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        binding = UserProfileViewingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user_id = intent.getStringExtra(User::user_id.name)

        if (user_id.isNullOrEmpty()) {
            Log.e("UserProfileViewingActivity", "Invalid provided user id: $user_id")
            return
        }

        getUserData(user_id) { user ->
            m_displayed_user = user
            initializeTabLayout()
            populateProfileImage()
            populateTextViewsWithUserInfo()

            binding.chatButton.setOnClickListener {
                val current_user_id = getCurrentUserId()
                val other_user_id = m_displayed_user?.user_id!!

                val participant_ids = listOf(current_user_id, other_user_id).sorted()

                ChatRepository.createOrGetChat(
                    participant_ids = participant_ids,
                    onComplete = { chat_id ->
                        Log.d("UserProfileViewingActivity", "Found or created chat: $chat_id")
                        val intent =
                            Intent(this, NavigationBaseActivity::class.java).apply {
                                putExtra("initial_tab", R.id.navigation_messages)
                                putExtra("open_chat_id", chat_id)
                            }
                        startActivity(intent)
                    },
                    onError = {
                        Toast.makeText(this, "Failed to start chat", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            }
        }
    }

    fun initializeTabLayout() {
        if (is_tab_layout_initialized) return // Ensure Firebase is ready and prevent re-initialization
        is_tab_layout_initialized = true

        val tab_layout = binding.tabLayout
        val view_pager = binding.viewPager

        val is_shelter = m_displayed_user?.is_shelter ?: false

        val visible_tabs = mutableListOf(
            ProfileTabType.LIKED,
        )
        if (is_shelter) {
            visible_tabs.add(ProfileTabType.HOSTED)
        }

        val tab_adapter = ProfileTabAdapter(this, visible_tabs, m_displayed_user?.user_id)
        view_pager.adapter = tab_adapter
        view_pager.offscreenPageLimit = 1 // Ensures fragments are refreshed when switched

        view_pager.adapter = tab_adapter

        // Sync TabLayout with ViewPager2
        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = when (tab_adapter.getTabTypeAt(position)) {
                ProfileTabType.LIKED -> Strings.get(R.string.liked_animals_tab_name)
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


    fun populateProfileImage() {
        val profile_image_view = binding.profilePictureView
        if (m_displayed_user?.profile_image_path.isNullOrEmpty()) {
            profile_image_view.setImageDrawable(getImage("default_profile_pic"))
        } else {
            loadCloudStoredImageIntoImageView(
                this,
                m_displayed_user?.profile_image_path,
                profile_image_view
            )
        }
    }

    /**
     * Dynamically obtains stored drawable images by name.
     * Ensures that the activity is available and the image exists.
     */
    private fun getImage(image_name: String): Drawable? {
        val resources = this.resources

        val imageId = resources.getIdentifier(image_name, "drawable", this.packageName)

        return if (imageId != 0) {
            ResourcesCompat.getDrawable(resources, imageId, this.theme)
        } else {
            Log.e("getImage", "Drawable not found: $image_name, using default image")
            ResourcesCompat.getDrawable(resources, R.drawable.default_profile_image, this.theme)
        }
    }

    fun populateTextViewsWithUserInfo() {
        val public_username_text_view = binding.publicUsername
        val biography_text_view = binding.biographyField

        public_username_text_view.text =
            m_displayed_user?.display_name?.ifEmpty { "Unnamed User" }
        public_username_text_view.isSelected = true // Required for marquee text to function
        biography_text_view.text = m_displayed_user?.biography
    }
}