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
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.fetchAllUserAnimals
import sweng894.project.adopto.databinding.ProfileSavedAnimalsFragmentBinding

class SavedAnimalsFragment : Fragment() {
    private var _binding: ProfileSavedAnimalsFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var m_animals_list_adaptor: ProfileAnimalsAdapter

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
            fetchAndDisplayUserSavedAnimals()

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                fetchAndDisplayUserSavedAnimals() // Refresh when Firebase updates
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private fun bindToFirebaseService() {
        Intent(activity, FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromFirebaseService() {
        m_service_connected = false
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
        if (m_service_connected) {
            fetchAndDisplayUserSavedAnimals() // Refresh when View is created
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProfileSavedAnimalsFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    fun initializeRecyclerViewAdapter() {
        val saved_animals_recycler_view = binding.savedAnimals
        // Initialize recyclerview adaptor
        m_animals_list_adaptor =
            ProfileAnimalsAdapter(requireContext(), m_firebase_data_service, false)
        saved_animals_recycler_view.adapter = m_animals_list_adaptor
    }

    fun initializeRecyclerViewLayoutManager() {
        val saved_animals_recycler_view = binding.savedAnimals
        // Initialize FlexBox Layout Manager for recyclerview to allow wrapping items to next line
        val layout_manager = FlexboxLayoutManager(requireContext())
        layout_manager.apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
            flexWrap = FlexWrap.WRAP
        }
        saved_animals_recycler_view.layoutManager = layout_manager
    }

    fun fetchAndDisplayUserSavedAnimals() {
        val user = m_firebase_data_service.current_user_data
        val user_animal_ids = user?.saved_animal_ids ?: emptyList()

        fetchAllUserAnimals(user_animal_ids) { animal_list ->
            activity?.runOnUiThread {
                m_animals_list_adaptor.updateAnimals(animal_list) // Update adapter with data
            }
        }
    }
}
