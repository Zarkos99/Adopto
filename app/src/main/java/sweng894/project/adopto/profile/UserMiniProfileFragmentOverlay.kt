package sweng894.project.adopto.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.getUserData
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.databinding.UserProfileViewingMiniActivityBinding

class UserMiniProfileFragmentOverlay : BottomSheetDialogFragment() {

    private var _binding: UserProfileViewingMiniActivityBinding? = null
    private val binding get() = _binding!!
    private var shelter_id: String? = null

    companion object {
        private const val ARG_SHELTER_ID = "shelter_id"

        fun newInstance(shelter_id: String): UserMiniProfileFragmentOverlay {
            val fragment = UserMiniProfileFragmentOverlay()
            val args = Bundle()
            args.putString(ARG_SHELTER_ID, shelter_id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shelter_id = arguments?.getString(ARG_SHELTER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserProfileViewingMiniActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shelter_id?.let { id ->
            getUserData(id) { user ->
                populateMiniProfile(user)
            }
        }

        binding.root.setOnClickListener {
            shelter_id?.let { id ->
                val intent = Intent(requireContext(), UserProfileViewingActivity::class.java)
                intent.putExtra(User::user_id.name, id)
                startActivity(intent)
                dismiss() // close the bottom sheet after click
            }
        }
    }

    private fun populateMiniProfile(user: User?) {
        binding.userDisplayName.text =
            if (user?.display_name.isNullOrEmpty()) "Unnamed User" else user?.display_name

        // Truncate bio if it's too long
        val truncated_bio = if ((user?.biography?.length ?: 0) > 100) {
            user?.biography?.substring(0, 100) + "..."
        } else if ((user?.biography?.length ?: 0) == 0) {
            "No biography"
        } else {
            user?.biography
        }
        binding.userBiography.text = truncated_bio ?: ""

        // Load image (if using a library like Picasso/Glide)
        loadCloudStoredImageIntoImageView(
            requireContext(),
            user?.profile_image_path,
            binding.userProfileImage
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
