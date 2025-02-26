package sweng894.project.adopto.explore

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.Direction
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.appendToDataFieldArray
import sweng894.project.adopto.database.appendToDataFieldMap
import sweng894.project.adopto.database.fetchAllAnimals
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.databinding.ExploreFragmentBinding
import java.time.Instant
import java.time.format.DateTimeFormatter

class ExploreFragment : Fragment(), CardStackListener {

    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: AnimalCardAdapter
    private val animal_list = mutableListOf<Animal>()


    // This property is only valid between onCreateView and
    // onDestroyView.
    private var _binding: ExploreFragmentBinding? = null
    private val binding get() = _binding!!

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers
    private var is_firebase_service_bound = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            is_firebase_service_bound = true
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                loadAnimals()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ExploreFragmentBinding.inflate(inflater, container, false)
        loadAnimals()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeCardHeight()
        setupCardStackView()
    }

    private fun initializeCardHeight() {
        // Get screen height and calculate 2/3 of the screen
        val screen_height = Resources.getSystem().displayMetrics.heightPixels
        val card_stack_height = (screen_height * 2) / 3

        // Apply the new height dynamically
        val card_stack_view = binding.cardStackView

        val layoutParams = card_stack_view.layoutParams
        layoutParams.height = card_stack_height
        card_stack_view.layoutParams = layoutParams
    }

    private fun setupCardStackView() {
        manager = CardStackLayoutManager(requireContext(), this).apply {
            setStackFrom(StackFrom.Top)
            setTranslationInterval(8.0f)
            setSwipeThreshold(0.3f)
            setDirections(listOf(Direction.Left, Direction.Right, Direction.Top, Direction.Bottom))
            setCanScrollHorizontal(true)
            setCanScrollVertical(true)
        }

        adapter = AnimalCardAdapter(animal_list)
        binding.cardStackView.layoutManager = manager
        binding.cardStackView.adapter = adapter
        binding.cardStackView.itemAnimator = DefaultItemAnimator()
    }

    private fun loadAnimals() {
        if (!is_firebase_service_bound) {
            Log.d(
                "TRACE",
                "Firebase service not bound. Need user data to determine animal exploration filters."
            )
            return
        }

        fetchAllAnimals { all_animals ->
            val current_user = m_firebase_data_service.current_user_data
            val viewed_animals =
                current_user?.viewed_animals ?: emptyMap() // Animals the user has seen

            // Define time threshold to "re-show" animals (e.g., 7 days)
            val current_time = Instant.now()
            val time_threshold = current_time.minusSeconds(7 * 24 * 60 * 60) // 7 days ago

            // Filter out animals that are:
            // 1. Already favorited (saved)
            // 2. Hosted by the user
            // 3. Recently viewed (within time_threshold)
            val filtered_animals = all_animals.filter { animal ->
                val viewed_time_str = viewed_animals[animal.animal_id]
                val viewed_time =
                    viewed_time_str?.let { Instant.parse(it) } // Convert ISO string to Instant

                current_user?.saved_animal_ids?.contains(animal.animal_id) != true
                        && current_user?.hosted_animal_ids?.contains(animal.animal_id) != true
                        && (viewed_time == null || viewed_time.isBefore(time_threshold))
            }

            if (animal_list.isEmpty()) { // Fetch data only if the list is empty
                animal_list.addAll(filtered_animals)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCardSwiped(direction: Direction) {
        val animal = animal_list[manager.topPosition - 1] // Current swiped animal
        appendToDataFieldMap(
            Strings.get(R.string.firebase_collection_users),
            getCurrentUserId(),
            User::viewed_animals,
            animal.animal_id,
            DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )

        when (direction) {
            Direction.Right -> {
                saveAnimalMatch(animal)
                Toast.makeText(
                    requireContext(),
                    "Matched with ${animal.animal_name}!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            Direction.Left, Direction.Bottom -> {
                Log.d("TRACE", "Skipped ${animal.animal_name}")
            }

            Direction.Top -> {
                // Go back to previous animal
                if (manager.topPosition > 0) {
                    binding.cardStackView.smoothScrollToPosition(manager.topPosition - 1)
                }
            }

            else -> {}
        }
    }

    private fun saveAnimalMatch(animal: Animal) {
        // Save animal
        appendToDataFieldArray(
            Strings.get(
                R.string.firebase_collection_users
            ),
            getCurrentUserId(),
            User::saved_animal_ids,
            animal.animal_id
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onCardRewound() {}
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}