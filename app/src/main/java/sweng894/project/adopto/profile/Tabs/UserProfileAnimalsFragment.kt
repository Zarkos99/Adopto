package sweng894.project.adopto.profile.Tabs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.fetchAnimals
import sweng894.project.adopto.database.getUserData
import sweng894.project.adopto.databinding.UserProfileAnimalsListFragmentBinding

class UserProfileAnimalsFragment : Fragment(), RefreshableTab {

    companion object {
        private val USER_INPUT = "user_id"
        private val FRAGMENT_TYPE_INPUT = "fragment_type"

        fun newInstance(
            user_id: String?, // Optional argument, when provided, use this over the firebase user service
            fragment_list_type: ProfileTabType //Non-optional argument
        ): UserProfileAnimalsFragment {
            val fragment = UserProfileAnimalsFragment()
            val args = Bundle()
            if (!user_id.isNullOrEmpty()) {
                args.putString(USER_INPUT, user_id)
            }
            args.putString(FRAGMENT_TYPE_INPUT, fragment_list_type.name)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: UserProfileAnimalsListFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var m_animals_list_adaptor: UserProfileAnimalsAdapter
    private var m_user: User? = null
    private var m_user_provided = false
    private lateinit var m_fragment_list_type: ProfileTabType

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers
    private var m_service_connected = false;

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            m_service_connected = true
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()

            initializeRecyclerViewAdapter()
            initializeRecyclerViewLayoutManager()

            // Fetch and display animals when data is ready
            fetchAndDisplayUserAnimals()

            if (!m_user_provided) {
                Log.d(
                    "ProfileAnimalsFragment",
                    "Viewing user: ${m_firebase_data_service.current_user_data?.user_id}"
                )
            }

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                fetchAndDisplayUserAnimals() // Refresh when Firebase updates
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private fun bindToFirebaseService() {
        if (m_user == null) {
            Intent(activity, FirebaseDataServiceUsers::class.java).also { intent ->
                requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun unbindFromFirebaseService() {
        if (m_user == null) {
            m_service_connected = false
            requireActivity().unbindService(connection)
        }
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
        if (m_service_connected) {
            fetchAndDisplayUserAnimals() // Refresh when View is created
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserProfileAnimalsListFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val user_id = arguments?.getString(USER_INPUT)
        m_user_provided = arguments?.containsKey(USER_INPUT) == true

        val type_name = arguments?.getString(FRAGMENT_TYPE_INPUT)
        m_fragment_list_type = ProfileTabType.valueOf(type_name ?: "UNKNOWN")

        if (m_fragment_list_type == ProfileTabType.UNKNOWN) {
            Log.e(
                "ProfileAnimalsFragment",
                "Invalid Fragment List Type provided: $type_name. Returning early."
            )
            return root
        }

        if (m_user_provided) {
            getUserData(user_id!!) { user ->
                m_user = user
                // Will not be initialized after firebase service has connected so initialize it here
                initializeRecyclerViewAdapter()
                initializeRecyclerViewLayoutManager()

                // Fetch and display animals when data is ready
                fetchAndDisplayUserAnimals()

                Log.d("ProfileAnimalsFragment", "Viewing user: ${m_user?.user_id}")
            }

        }

        return root
    }

    override fun refreshTabContent() {
        fetchAndDisplayUserAnimals()
    }

    fun initializeRecyclerViewAdapter() {
        val animals_recycler_view = binding.animalsList
        // Initialize recyclerview adaptor
        m_animals_list_adaptor = UserProfileAnimalsAdapter(requireContext())
        animals_recycler_view.adapter = m_animals_list_adaptor
    }

    fun initializeRecyclerViewLayoutManager() {
        val animals_recycler_view = binding.animalsList
        // Initialize FlexBox Layout Manager for recyclerview to allow wrapping items to next line
        val layout_manager = FlexboxLayoutManager(requireContext())
        layout_manager.apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
            flexWrap = FlexWrap.WRAP
        }
        animals_recycler_view.layoutManager = layout_manager
    }

    fun fetchAndDisplayUserAnimals() {
        val user: User?

        if (m_user_provided) {
            user = m_user
        } else {
            user = m_firebase_data_service.current_user_data
        }

        val user_animal_ids: List<String>

        when (m_fragment_list_type) {
            ProfileTabType.LIKED -> {
                user_animal_ids = (user?.liked_animal_ids ?: emptyList())
            }

            ProfileTabType.ADOPTING -> {
                user_animal_ids = (user?.adopting_animal_ids ?: emptyList())
            }

            ProfileTabType.HOSTED -> {
                user_animal_ids = (user?.hosted_animal_ids ?: emptyList())
            }

            else -> {
                user_animal_ids = emptyList()
            }
        }

        fetchAnimals(user_animal_ids) { animal_list ->
            activity?.runOnUiThread {
                if (animal_list.isEmpty()) {
                    binding.emptyListTextView.visibility = View.VISIBLE
                } else {
                    binding.emptyListTextView.visibility = View.GONE
                }
                m_animals_list_adaptor.updateAnimals(animal_list) // Update adapter with data
            }
        }
    }
}
