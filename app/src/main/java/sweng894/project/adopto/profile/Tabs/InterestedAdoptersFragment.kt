package sweng894.project.adopto.profile.animalprofile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.firebase.fetchInterestedAdoptersForAnimal
import sweng894.project.adopto.databinding.AnimalProfileInterestedAdoptersFragmentBinding
import sweng894.project.adopto.profile.Tabs.InterestedAdoptersAdapter
import sweng894.project.adopto.profile.Tabs.RefreshableTab

class InterestedAdoptersFragment : Fragment(), RefreshableTab {

    private val log_tag = InterestedAdoptersFragment::class.simpleName
    private var _binding: AnimalProfileInterestedAdoptersFragmentBinding? = null
    private val binding get() = _binding!!
    private var m_animal_id: String? = null
    private var adapter: InterestedAdoptersAdapter? = null

    companion object {
        private const val ARG_ANIMAL_ID = "animal_id"

        fun newInstance(animal_id: String?): InterestedAdoptersFragment {
            val fragment = InterestedAdoptersFragment()
            val args = Bundle()
            if (!animal_id.isNullOrEmpty()) {
                args.putString(ARG_ANIMAL_ID, animal_id)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_animal_id = arguments?.getString(ARG_ANIMAL_ID)
        val animal_provided = arguments?.containsKey(ARG_ANIMAL_ID) == true

        if (!animal_provided) {
            Log.e(
                log_tag,
                "Invalid animal_id provided, cannot view profile: $m_animal_id"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            AnimalProfileInterestedAdoptersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (m_animal_id.isNullOrEmpty()) {
            Log.e(log_tag, "Animal ID is null or empty")
            return
        }

        initializeRecyclerViewAdapter()
    }

    override fun refreshTabContent() {
        loadContentIntoRecyclerView()
    }

    fun initializeRecyclerViewAdapter() {
        val recycler_view = binding.interestedAdoptersRecyclerView

        // Ensure smooth scrolling
        recycler_view.setHasFixedSize(true)

        // Initialize recyclerview adaptor
        adapter =
            InterestedAdoptersAdapter(requireContext())
        recycler_view.adapter = adapter
        recycler_view.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        loadContentIntoRecyclerView()
    }

    fun loadContentIntoRecyclerView() {
        if (!m_animal_id.isNullOrEmpty()) {
            fetchInterestedAdoptersForAnimal(m_animal_id!!) { interested_adopters ->
                adapter?.updateUsers(interested_adopters)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
