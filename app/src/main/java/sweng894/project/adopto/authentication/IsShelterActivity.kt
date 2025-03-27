package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.databinding.AuthNewAccountInfoLayoutBinding

class IsShelterActivity : AppCompatActivity() {

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
            openActivity(true)
        }

        no_button_view.setOnClickListener {
            openActivity(false)
        }
    }

    private fun openActivity(is_shelter: Boolean) {
        val intent = Intent(this, UserProfileCreationActivity::class.java)
        intent.putExtra("is_shelter", is_shelter)
        startActivity(intent)
        finish()
    }
}

