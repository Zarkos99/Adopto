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
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeableMethod
import sweng894.project.adopto.R
import sweng894.project.adopto.Strings
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.appendToDataFieldArray
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.getRecommendations
import sweng894.project.adopto.database.recalculatePreferenceVector
import sweng894.project.adopto.databinding.ExploreFragmentBinding
import sweng894.project.adopto.profile.ExplorePreferencesActivity
import java.time.Instant

/**
 * A fragment allowing users to explore databased animals using a CardStackView architecture.
 * CardStackView source: https://github.com/yuyakaido/CardStackView/tree/master
 */
class ExploreFragment : Fragment(), CardStackListener {

    private lateinit var m_skip_button: FloatingActionButton
    private lateinit var m_rewind_button: FloatingActionButton
    private lateinit var m_like_animal_button: FloatingActionButton

    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: AnimalCardAdapter
    private val m_animal_list = mutableListOf<Animal>()


    // This property is only valid between onCreateView and
    // onDestroyView.
    private var _binding: ExploreFragmentBinding? = null
    private val binding get() = _binding!!

    /** Start FirebaseDataService Setup **/
    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers
    private var m_is_firebase_service_bound = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            m_is_firebase_service_bound = true
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            m_firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()

            recalculatePreferenceVector()
            loadAnimals() // execute on service connection

            // Populate user info on future updates
            m_firebase_data_service.registerCallback {
                Log.d("ExploreFragment", "Firebase callback update received")
                loadAnimals()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            m_is_firebase_service_bound = false
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
        initializePreferencesButton()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeScrollControls()
        initializeCardHeight()
        setupCardStackView()
        m_animal_list.clear()
    }


    private fun initializePreferencesButton() {
        val explore_preferences_button = binding.explorePreferencesButton

        explore_preferences_button.setOnClickListener {
            val intent = Intent(activity, ExplorePreferencesActivity::class.java)
            intent.putExtra(
                "explore_preferences",
                m_firebase_data_service.current_user_data?.explore_preferences
            )
            startActivity(intent)
            // Not calling finish() here so that the Activity will come back to this fragment
        }
    }

    private fun initializeScrollControls() {
        m_skip_button = binding.skipButton
        m_rewind_button = binding.rewindButton
        m_like_animal_button = binding.likeAnimalButton

        m_skip_button.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .build()

            manager.setSwipeAnimationSetting(setting)
            binding.cardStackView.swipe()
        }
        m_rewind_button.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Top)
                .setDuration(Duration.Normal.duration)
                .build()

            manager.setSwipeAnimationSetting(setting)
            binding.cardStackView.swipe()
        }
        m_like_animal_button.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .build()

            manager.setSwipeAnimationSetting(setting)
            binding.cardStackView.swipe()
        }
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

    private fun initializeManager(): CardStackLayoutManager {
        return CardStackLayoutManager(requireContext(), this).apply {
            setStackFrom(StackFrom.Top)
            setTranslationInterval(8.0f)
            setSwipeThreshold(0.3f)
            setDirections(listOf(Direction.Left, Direction.Right, Direction.Top, Direction.Bottom))
            setCanScrollHorizontal(true)
            setCanScrollVertical(true)
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }
    }

    private fun setupCardStackView() {
        manager = initializeManager()

        adapter = AnimalCardAdapter(m_animal_list)
        binding.cardStackView.layoutManager = manager
        binding.cardStackView.adapter = adapter
        binding.cardStackView.itemAnimator = DefaultItemAnimator()
    }

    private fun loadAnimals() {
        if (!m_is_firebase_service_bound) {
            Log.d(
                "TRACE",
                "Firebase service not bound. Need user data to determine animal exploration filters."
            )
            return
        }

        getRecommendations { recommended_animals ->
            m_animal_list.clear()
            m_animal_list.addAll(recommended_animals)
            adapter.notifyDataSetChanged()

        }
    }

    override fun onCardSwiped(direction: Direction) {
        Log.d("TRACE", "Card swiped $direction ")
        val animal = m_animal_list[manager.topPosition - 1] // Current swiped animal
        var is_animal_viewed = false

        when (direction) {

            Direction.Top -> {
                rewind()
            }

            Direction.Left, Direction.Bottom -> {
                skip(animal)
                is_animal_viewed = true
            }

            Direction.Right -> {
                likeAnimal(animal)
                is_animal_viewed = true
            }

            else -> {}
        }

        if (is_animal_viewed) {
            //TODO: uncomment this to disallow users from seeing the same animal multiple times after debug is finished
//            appendToDataFieldMap(
//                FirebaseCollections.USERS,
//                getCurrentUserId(),
//                User::viewed_animals,
//                animal.animal_id,
//                DateTimeFormatter.ISO_INSTANT.format(Instant.now())
//            )
        }
    }

    private fun skip(animal: Animal) {
        Log.d("TRACE", "Skipped ${animal.animal_name}")
    }

    private fun rewind() {
        Log.d("TRACE", "Attempting to rewind card")
        val setting = RewindAnimationSetting.Builder()
            .setDirection(Direction.Bottom)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(DecelerateInterpolator())
            .build()

        val new_manager = initializeManager()
        new_manager.setRewindAnimationSetting(setting)
        manager = new_manager
        binding.cardStackView.setLayoutManager(manager)
        binding.cardStackView.smoothScrollToPosition(manager.topPosition - 1)
        binding.cardStackView.rewind()
    }

    private fun likeAnimal(animal: Animal) {
        // Like animal
        appendToDataFieldArray(
            FirebaseCollections.USERS,
            getCurrentUserId(),
            User::liked_animal_ids,
            animal.animal_id
        )

        // Recalculate and store preference vector
        recalculatePreferenceVector()
        loadAnimals()

        Toast.makeText(
            requireContext(),
            "Liked ${animal.animal_name}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCardRewound() {
        Log.d("CardStackView", "onCardRewound: ${manager.topPosition}")
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}