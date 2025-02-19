package sweng894.project.adopto.profile.animalprofile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.coroutines.launch
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.*
import sweng894.project.adopto.databinding.AnimalProfileViewingLayoutBinding


class AnimalProfileViewingActivity : AppCompatActivity() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private lateinit var binding: AnimalProfileViewingLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnimalProfileViewingLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data using the same key
        val animal_id = intent.getStringExtra("animal_id")

        if (animal_id == null) {
            //Display error message
            Toast.makeText(
                this,
                "Cannot find animal",
                Toast.LENGTH_LONG
            ).show()
            Log.e("AnimalProfileViewingActivity", "Activity provided null animal_id")
            finish()
        }

        lifecycleScope.launch {
            try {
                val animal = getAnimalData(animal_id!!)
                if (animal == null) {
                    Log.e("AnimalProfileViewingActivity", "Database queried animal returned null.")
                    //Display error message
                    Toast.makeText(
                        this@AnimalProfileViewingActivity,
                        "Cannot find animal: $animal_id",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    initializeRecyclerViewAdapter(animal)
                    populateTextViewsWithAnimalInfo(animal)
                    populateProfileImage(animal)
                }
            } catch (e: Exception) {
                Log.w(
                    "AnimalProfileViewingActivity",
                    "Error fetching animal $animal_id: ${e.message}"
                )
                // If db query fails, display a message to the user
                Toast.makeText(
                    this@AnimalProfileViewingActivity,
                    "Database Query error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val adopt_button = binding.adoptButton
        adopt_button.setOnClickListener {
            // TODO: Initiate adoption
        }
    }

    fun populateProfileImage(current_animal: Animal) {
        loadCloudStoredImageIntoImageView(
            this,
            current_animal.profile_image_path,
            binding.profileImageView
        )
    }

    fun populateTextViewsWithAnimalInfo(current_animal: Animal) {
        val animal_name_view = binding.animalName
        val animal_age_view = binding.animalAge
        val animal_health_view = binding.animalHealth
        val animal_description_view = binding.animalDescription

        animal_name_view.text = current_animal.animal_name
        animal_age_view.text = current_animal.animal_age.toString()
        animal_health_view.text = current_animal.health_summary
        animal_description_view.text = current_animal.biography
    }


    fun initializeRecyclerViewAdapter(current_animal: Animal) {
        val animal_images_recycler_view = binding.additionalImages
        // Initialize recyclerview adaptor
        val animals_list_adaptor =
            AnimalProfileAdditionalImagesAdapter(this, false, null)
        animal_images_recycler_view.adapter = animals_list_adaptor
        initializeRecyclerViewLayoutManager()
        
        current_animal.supplementary_image_paths.forEach { image_path ->
            animals_list_adaptor.addItem(
                Uri.parse(image_path)
            )
        }
    }

    fun initializeRecyclerViewLayoutManager() {
        val animal_images_recycler_view = binding.additionalImages
        // Initialize FlexBox Layout Manager for recyclerview to allow wrapping items to next line
        val layout_manager = FlexboxLayoutManager(this)
        layout_manager.apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
            flexWrap = FlexWrap.WRAP
        }
        animal_images_recycler_view.layoutManager = layout_manager
    }
}