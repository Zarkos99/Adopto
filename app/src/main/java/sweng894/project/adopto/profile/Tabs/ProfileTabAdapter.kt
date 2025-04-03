package sweng894.project.adopto.profile.Tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import sweng894.project.adopto.profile.animalprofile.AnimalImagesFragment
import sweng894.project.adopto.profile.animalprofile.InterestedAdoptersFragment

enum class ProfileTabType {
    UNKNOWN,
    LIKED,
    ADOPTING,
    HOSTED,
    ANIMAL_IMAGES,
    INTERESTED_ADOPTERS
}

interface RefreshableTab {
    fun refreshTabContent()
}

class ProfileTabAdapter(
    fragment_activity: FragmentActivity,
    private val visible_tabs: List<ProfileTabType>,
    private val user_id: String? = null,
    private val animal_id: String? = null
) :
    FragmentStateAdapter(fragment_activity) {

    private val m_fragment_map = mutableMapOf<Int, Fragment>()

    override fun getItemCount(): Int = visible_tabs.size

    override fun createFragment(position: Int): Fragment {
        val fragment = when (visible_tabs[position]) {
            ProfileTabType.LIKED,
            ProfileTabType.ADOPTING,
            ProfileTabType.HOSTED -> {
                val f = UserProfileAnimalsFragment.newInstance(user_id, visible_tabs[position])
                m_fragment_map[position] = f
                f
            }

            ProfileTabType.ANIMAL_IMAGES -> AnimalImagesFragment.newInstance(animal_id)
            ProfileTabType.INTERESTED_ADOPTERS -> InterestedAdoptersFragment.newInstance(animal_id)

            else -> Fragment()
        }
        return fragment
    }

    fun getFragmentAt(position: Int): Fragment? {
        return when (visible_tabs[position]) {
            ProfileTabType.LIKED,
            ProfileTabType.ADOPTING,
            ProfileTabType.HOSTED -> m_fragment_map[position]

            ProfileTabType.ANIMAL_IMAGES -> AnimalImagesFragment.newInstance(animal_id)
            ProfileTabType.INTERESTED_ADOPTERS -> InterestedAdoptersFragment.newInstance(animal_id)
            else -> null
        }
    }

    fun getTabTypeAt(position: Int): ProfileTabType {
        return visible_tabs[position]
    }
}
