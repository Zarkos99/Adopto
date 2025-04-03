package sweng894.project.adopto.profile.animalprofile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.getAnimalData
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.databinding.AnimalProfileImagesFragmentBinding
import sweng894.project.adopto.profile.Tabs.AdapterClickability
import sweng894.project.adopto.profile.Tabs.AnimalProfileViewingImagesAdapter
import sweng894.project.adopto.profile.Tabs.RefreshableTab

class AnimalImagesFragment : Fragment(), RefreshableTab {

    private val log_tag = AnimalImagesFragment::class.simpleName
    private var _binding: AnimalProfileImagesFragmentBinding? = null
    private val binding get() = _binding!!
    private var m_animal_id: String? = null
    private var adapter: AnimalProfileViewingImagesAdapter? = null

    companion object {
        private const val ARG_ANIMAL_ID = "animal_id"

        fun newInstance(animal_id: String?): AnimalImagesFragment {
            val fragment = AnimalImagesFragment()
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
        _binding = AnimalProfileImagesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (m_animal_id.isNullOrEmpty()) {
            Log.e(log_tag, "Animal ID is null or empty")
            return
        }

        getAnimalDataAndExecuteCallback { animal -> initializeRecyclerViewAdapter(animal) }
    }

    override fun refreshTabContent() {
        getAnimalDataAndExecuteCallback { animal -> loadContentIntoRecyclerView(animal) }
    }

    fun getAnimalDataAndExecuteCallback(onGetDataSuccess: ((Animal) -> Unit)?) {
        m_animal_id?.let { id ->
            getAnimalData(id, onComplete = { animal ->
                if (animal != null) {
                    onGetDataSuccess?.invoke(animal)
                } else {
                    Log.e(log_tag, "Fetched animal is null.")
                }
            })
        }
    }

    fun initializeRecyclerViewAdapter(animal: Animal) {
        val animal_images_recycler_view = binding.animalImagesRecyclerView

        // Ensure smooth scrolling
        animal_images_recycler_view.setHasFixedSize(true)

        // Initialize recyclerview adaptor
        val clickability =
            if (animal.associated_shelter_id == getCurrentUserId()) AdapterClickability.DOUBLE_CLICKABLE else AdapterClickability.NOT_CLICKABLE
        adapter =
            AnimalProfileViewingImagesAdapter(requireContext(), animal, clickability)
        animal_images_recycler_view.adapter = adapter
        animal_images_recycler_view.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        loadContentIntoRecyclerView(animal)
    }

    fun loadContentIntoRecyclerView(animal: Animal) {
        val image_uris =
            ArrayList(animal.supplementary_image_paths.map {
                Uri.parse(it)
            })

        if (image_uris.isEmpty()) {
            binding.emptyListTextView.visibility = View.VISIBLE
        } else {
            binding.emptyListTextView.visibility = View.GONE
        }

        adapter?.setItems(image_uris)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
