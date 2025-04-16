package sweng894.project.adopto.authentication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.addUserToDatabase
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.getUserData
import sweng894.project.adopto.databinding.AuthAuthenticationLayoutBinding

// First visible activity
class AuthenticationActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AuthAuthenticationLayoutBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthAuthenticationLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        requestNotificationsPermissions()

        val usernameEditTextView = binding.usernameField
        val passwordEditTextView = binding.passwordField
        val loginButtonView = binding.loginButton
        val registerButtonView = binding.registerButton

        loginButtonView.setOnClickListener {
            val username = usernameEditTextView.text.toString()
            val password = passwordEditTextView.text.toString()

            // Error handling for empty username
            if (username.isEmpty()) {
                // Display error message
                Toast.makeText(
                    this@AuthenticationActivity,
                    "Please input your username",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Error handling for empty password
            if (password.isEmpty()) {
                // Display error message
                Toast.makeText(
                    this@AuthenticationActivity,
                    "Please input your password",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Firebase Authentication for user login
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        getUserData(getCurrentUserId()) { user_data ->
                            if (user_data != null) {
                                Log.d(
                                    "AuthenticationActivity",
                                    "User fetched: ${user_data.user_id}"
                                )
                                if (user_data.need_info) {
                                    // Sign in success, need additional account info, display account information View
                                    openActivity(IsShelterActivity::class.java)
                                } else {
                                    // Sign in successful and no need for additional info, display navigation default view
                                    openActivity(NavigationBaseActivity::class.java)
                                }
                            } else {
                                Log.w("AuthenticationActivity", "User not found")
                                // Sign in successful but no user data
                                addUserToDatabase(User(need_info = true))
                                openActivity(IsShelterActivity::class.java)
                            }
                        }
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(
                            this@AuthenticationActivity,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        registerButtonView.setOnClickListener {
            val username = usernameEditTextView.text.toString()
            val password = passwordEditTextView.text.toString()

            // Error handling for empty username
            if (username.isEmpty()) {
                // Display error message
                Toast.makeText(
                    this@AuthenticationActivity,
                    "Please input your username",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Error handling for empty password
            if (password.isEmpty()) {
                // Display error message
                Toast.makeText(
                    this@AuthenticationActivity,
                    "Please input your password",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Firebase Authentication for user registration
            auth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) { // Registration success
                        Log.d(
                            "AuthenticationActivity",
                            "Registration successful, adding user to database."
                        )
                        // Display navigation default View - enlarged map
                        addUserToDatabase(User(need_info = true))

                        openActivity(IsShelterActivity::class.java)
                    } else { // Failed Registration
                        // Display a message to the user
                        Toast.makeText(
                            this@AuthenticationActivity,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun openActivity(activity_class: Class<out AppCompatActivity>) {
        val intent = Intent(this, activity_class)
        startActivity(intent)
        finish()
    }

    fun requestNotificationsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}
