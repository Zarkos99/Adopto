package sweng894.project.adopto

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User
import sweng894.project.adopto.databinding.NavigationBaseActivityBinding
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.updateDataField
import sweng894.project.adopto.messages.UserMessagesFragment

class NavigationBaseActivity : AppCompatActivity() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    lateinit var binding: NavigationBaseActivityBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NavigationBaseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Save FCM token once user is authenticated
        saveFcmTokenIfNeeded()

        binding.root.post {
            val nav_view: BottomNavigationView = binding.navView

            val nav_controller = findNavController(R.id.nav_host_fragment_activity_main)

            val initial_tab = intent.getIntExtra("initial_tab", R.id.navigation_explore)
            val open_chat_id = intent.getStringExtra("open_chat_id")

            nav_controller.navigate(
                initial_tab,
                null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .build()
            )

            nav_view.setupWithNavController(nav_controller)
            supportActionBar?.hide()

            // Delay chat loading until MessagesFragment is attached
            if (initial_tab == R.id.navigation_messages && open_chat_id != null) {
                nav_controller.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.id == R.id.navigation_messages) {
                        waitForFragmentAndSetChatId()
                    }
                }
            }
        }
    }

    private fun saveFcmTokenIfNeeded() {
        val user_id = getCurrentUserId()

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val prefs = getSharedPreferences("adopto_prefs", MODE_PRIVATE)
                val last_saved = prefs.getString("last_fcm_token", null)

                if (token != last_saved) {
                    Log.d("FCM", "Saving FCM token for user ${user_id}: $token")

                    updateDataField(
                        FirebaseCollections.USERS,
                        user_id,
                        User::fcm_token,
                        token, onUploadSuccess = {
                            Log.d("FCM", "Token successfully saved to Firestore")
                            prefs.edit().putString("last_fcm_token", token).apply()
                        }, onUploadFailure = {
                            Log.w("FCM", "Failed to save token")
                        })
                } else {
                    Log.d("FCM", "FCM token unchanged, skipping upload")
                }
            }
            .addOnFailureListener { e ->
                Log.w("FCM", "Failed to get token", e)
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