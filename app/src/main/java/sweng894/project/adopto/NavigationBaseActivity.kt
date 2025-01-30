package sweng894.project.adopto

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import sweng894.project.adopto.databinding.NavigationBaseActivityBinding

class NavigationBaseActivity : AppCompatActivity() {

    private lateinit var binding: NavigationBaseActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = NavigationBaseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            val navView: BottomNavigationView = binding.navView

            val navController = findNavController(R.id.nav_host_fragment_activity_main)

            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_geo, R.id.navigation_explore, R.id.navigation_gallery
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
            supportActionBar?.hide()
        }
    }
}