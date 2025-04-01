package sweng894.project.adopto.profile.Tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

enum class AnimalFragmentListType {
    UNKNOWN,
    LIKED,
    ADOPTING,
    HOSTED
}

class ProfileTabAdapter(
    fragment_activity: FragmentActivity,
    private val visible_tabs: List<AnimalFragmentListType>,
    private val user_id: String? = null
) :
    FragmentStateAdapter(fragment_activity) {

    private val m_fragment_map = mutableMapOf<Int, ProfileAnimalsFragment>()

    override fun getItemCount(): Int = visible_tabs.size

    override fun createFragment(position: Int): Fragment {
        val type = visible_tabs[position]
        val fragment = ProfileAnimalsFragment.newInstance(user_id, type)
        m_fragment_map[position] = fragment
        return fragment
    }

    fun getFragmentAt(position: Int): ProfileAnimalsFragment? {
        return m_fragment_map[position]
    }

    fun getTabTypeAt(position: Int): AnimalFragmentListType {
        return visible_tabs[position]
    }
}
