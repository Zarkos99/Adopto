package sweng894.project.adopto

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
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

            navController.navigate(
                initial_tab,
                null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .build()
            )

            navView.setupWithNavController(navController)
            supportActionBar?.hide()

            // Delay chat loading until MessagesFragment is attached
            if (initial_tab == R.id.navigation_messages && open_chat_id != null) {
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.id == R.id.navigation_messages) {
                        waitForFragmentAndSetChatId()
                    }
                }
            }
        }
    }

    private fun waitForFragmentAndSetChatId(retry_count: Int = 10) {
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()

        if (fragment is UserMessagesFragment) {
            Log.d("NavigationBaseActivity", "UserMessagesFragment found, setting initial chat")
            fragment.setInitialChat(intent.getStringExtra("open_chat_id") ?: return)
        } else if (retry_count > 0) {
            Log.d("NavigationBaseActivity", "Retrying... attempts left: $retry_count")
            binding.root.postDelayed({
                waitForFragmentAndSetChatId(retry_count - 1)
            }, 100)
        } else {
            Log.w("NavigationBaseActivity", "Failed to find UserMessagesFragment after retries")
        }
    }
}