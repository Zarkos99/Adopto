package sweng894.project.adopto.explore

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.firebase.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.profile.animalprofile.AnimalProfileViewingActivity

class AnimalCardStackAdapter(private val animals: MutableList<Animal>) :
    RecyclerView.Adapter<AnimalCardStackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.animal_image)
        val name: TextView = view.findViewById(R.id.animal_name)
        val description: TextView = view.findViewById(R.id.animal_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.explore_animal_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val animal = animals[position]
        holder.name.text = animal.animal_name
        holder.description.text = animal.biography

        // Load image using Glide or Picasso
        loadCloudStoredImageIntoImageView(
            holder.image.context,
            animal.profile_image_path,
            holder.image
        )

        // Click to open detailed profile
        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, AnimalProfileViewingActivity::class.java)
            intent.putExtra("animal_id", animal.animal_id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = animals.size
}
