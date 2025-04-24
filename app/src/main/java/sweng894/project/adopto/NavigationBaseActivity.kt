package sweng894.project.adopto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
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
        supportActionBar?.hide()

        // Save FCM token once user is authenticated
        saveFcmTokenIfNeeded()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null) {
            binding.navView.setupWithNavController(navController)

            Log.d("NavigationBaseActivity", "intent: $intent")
            binding.root.post { handleIntent(intent) }
        } else {
            Log.e("NavigationBaseActivity", "NavController was null in onCreate")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Clear old extras
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val nav_controller = findNavController(R.id.nav_host_fragment_activity_main)

        val initial_tab = intent.getIntExtra("initial_tab", R.id.navigation_explore)
        val open_chat_id = intent.getStringExtra("open_chat_id")

        Log.d("NavigationBaseActivity", "handleIntent() -> initialTab: $initial_tab")
        Log.d("NavigationBaseActivity", "handleIntent() -> openChatId: $open_chat_id")
        Log.d(
            "NavigationBaseActivity",
            "Current destination: ${nav_controller.currentDestination?.label}"
        )

        binding.navView.selectedItemId = initial_tab


        if (initial_tab == R.id.navigation_messages && !open_chat_id.isNullOrEmpty()) {
            nav_controller.addOnDestinationChangedListener(object :
                androidx.navigation.NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: androidx.navigation.NavController,
                    destination: androidx.navigation.NavDestination,
                    arguments: Bundle?
                ) {
                    Log.d(
                        "NavigationBaseActivity",
                        "Destination changed: $destination. \n arguments: $arguments"
                    )
                    if (destination.id == R.id.navigation_messages) {
                        controller.removeOnDestinationChangedListener(this)
                        waitForFragmentAndSetChatId(open_chat_id)
                    }
                }
            })
        }

        // Reset extras so this logic doesn't repeat
        intent.replaceExtras(Bundle())
    }

    private fun saveFcmTokenIfNeeded() {
        val user_id = getCurrentUserId()
        if (!user_id.isNullOrEmpty()) {
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
    }


    private fun waitForFragmentAndSetChatId(open_chat_id: String, retry_count: Int = 10) {
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()

        if (fragment is UserMessagesFragment) {
            Log.d("NavigationBaseActivity", "UserMessagesFragment found, setting initial chat")
            fragment.setInitialChat(open_chat_id)
        } else if (retry_count > 0) {
            Log.d("NavigationBaseActivity", "Retrying... attempts left: $retry_count")
            binding.root.postDelayed({
                waitForFragmentAndSetChatId(open_chat_id, retry_count - 1)
            }, 100)
        } else {
            Log.w("NavigationBaseActivity", "Failed to find UserMessagesFragment after retries")
        }
    }
}