package sweng894.project.adopto.explore

import android.content.res.Resources
import android.os.Bundle
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
import sweng894.project.adopto.database.appendToDataFieldArray
import sweng894.project.adopto.database.fetchAllAnimals
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.databinding.ExploreFragmentBinding

class ExploreFragment : Fragment(), CardStackListener {

    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: AnimalCardAdapter
    private val animal_list = mutableListOf<Animal>()


    // This property is only valid between onCreateView and
    // onDestroyView.
    private var _binding: ExploreFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ExploreFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeCardHeight()
        setupCardStackView()
        loadAnimals()
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
        fetchAllAnimals { animals ->
            animal_list.addAll(animals)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCardSwiped(direction: Direction) {
        val animal = animal_list[manager.topPosition - 1] // Current swiped animal

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