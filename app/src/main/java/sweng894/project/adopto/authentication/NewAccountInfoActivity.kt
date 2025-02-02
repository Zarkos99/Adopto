package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.databinding.AuthNewAccountInfoLayoutBinding

class NewAccountInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AuthNewAccountInfoLayoutBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthNewAccountInfoLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val yes_button_view = binding.yesButton
        val no_button_view = binding.noButton

        yes_button_view.setOnClickListener {
            openActivity(ShelterProfileCreationActivity::class.java)
        }

        no_button_view.setOnClickListener {
            openActivity(UserProfileCreationActivity::class.java)
        }
    }

    private fun openActivity(activity_class: Class<out AppCompatActivity>) {
        val intent = Intent(this, activity_class)
        startActivity(intent)
        finish()
    }
}

