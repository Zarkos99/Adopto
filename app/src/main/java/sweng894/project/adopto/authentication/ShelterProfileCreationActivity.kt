package sweng894.project.adopto.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R


class ShelterProfileCreationActivity : AppCompatActivity() {

    private lateinit var m_biography_edit_text: EditText
    private lateinit var m_zip_code_edit_text: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.auth_shelter_profile_creation_activity)

        m_biography_edit_text = findViewById(R.id.biography_field)
        m_zip_code_edit_text = findViewById(R.id.zip_code_field)
        val done_button_view = findViewById<Button>(R.id.done_button)

        done_button_view.setOnClickListener {

            // TODO: save biography to shelter database
            // TODO: save zip code to shelter database

            val intent =
                Intent(this@ShelterProfileCreationActivity, NavigationBaseActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}