package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import sweng894.project.adopto.R

class NewAccountInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.auth_new_account_info_layout)

        auth = FirebaseAuth.getInstance()

        val yesButtonView = findViewById<Button>(R.id.yes_button)
        val noButtonView = findViewById<Button>(R.id.no_button)

        yesButtonView.setOnClickListener {
            val intent =
                Intent(this@NewAccountInfoActivity, ShelterProfileCreationActivity::class.java)
            startActivity(intent)
            finish()
        }

        noButtonView.setOnClickListener {
            val intent =
                Intent(this@NewAccountInfoActivity, UserProfileCreationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
