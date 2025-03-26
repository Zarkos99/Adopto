package sweng894.project.adopto.profile.Tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProfileTabAdapter(fragment_activity: FragmentActivity, private val tab_count: Int) :
    FragmentStateAdapter(fragment_activity) {
    override fun getItemCount(): Int = tab_count

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SavedAnimalsFragment()
            1 -> AdoptingAnimalsFragment()
            2 -> MyAnimalsFragment()
            else -> SavedAnimalsFragment()
        }
    }
}
