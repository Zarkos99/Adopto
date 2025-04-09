package sweng894.project.adopto

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import sweng894.project.adopto.databinding.NavigationBaseActivityBinding
import sweng894.project.adopto.messages.UserMessagesFragment

class NavigationBaseActivity : AppCompatActivity() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    lateinit var binding: NavigationBaseActivityBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NavigationBaseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            val navView: BottomNavigationView = binding.navView

            val navController = findNavController(R.id.nav_host_fragment_activity_main)

            val initial_tab = intent.getIntExtra("initial_tab", R.id.navigation_explore)
            val open_chat_id = intent.getStringExtra("open_chat_id")

            navView.selectedItemId = initial_tab

            navView.setupWithNavController(navController)
            supportActionBar?.hide()

            // Delay chat loading until MessagesFragment is attached
            if (initial_tab == R.id.navigation_messages && open_chat_id != null) {
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.id == R.id.navigation_messages) {
                        val fragment =
                            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                                ?.childFragmentManager
                                ?.fragments
                                ?.firstOrNull()
                        if (fragment is UserMessagesFragment) {
                            fragment.setInitialChat(open_chat_id)
                        }
                    }
                }
            }
        }
    }
}