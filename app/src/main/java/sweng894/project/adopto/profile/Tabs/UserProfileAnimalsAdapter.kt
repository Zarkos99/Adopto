package sweng894.project.adopto.profile.Tabs

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.profile.animalprofile.AnimalProfileViewingActivity


/**
 * The adaptor for a recyclerview of sunset posts with the capability to have selectable items or not
 */
class UserProfileAnimalsAdapter(
    private val context: Context,
) :
    RecyclerView.Adapter<UserProfileAnimalsAdapter.ViewHolder>() {

    // IMPORTANT: Must include only existing animals, as the viewholder binding is positionally based
    private val animal_list: MutableList<Animal> = mutableListOf() // Store animals

    fun updateAnimals(new_animals: List<Animal>) {
        animal_list.clear()
        animal_list.addAll(new_animals)
        notifyDataSetChanged() // Refresh UI when data updates
    }

    /**
     * Handles creation of the view holder for each item in the recyclerview
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create new view
        val view = LayoutInflater.from(context)
            .inflate(R.layout.profile_animals_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Handles binding of the view holder for each item in the recyclerview
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val animal = animal_list[position]
        if (!animal.profile_image_path.isNullOrEmpty()) {
            loadCloudStoredImageIntoImageView(
                context,
                animal.profile_image_path,
                holder.animal_image_view
            )
        }

        // Provides logic to track all selected products as the user selects them
        holder.setItemClickListener(object : ViewHolder.ItemClickListener {
            override fun onItemClick(v: View, pos: Int) {
                val current_animal_id = animal_list[holder.adapterPosition].animal_id

                // Go to animal page
                val intent = Intent(
                    context,
                    AnimalProfileViewingActivity::class.java
                )
                intent.putExtra("animal_id", current_animal_id)
                context.startActivity(intent)
            }
        })
    }

    /**
     * Gets all of the items in the recyclerview
     */
    override fun getItemCount(): Int = animal_list.size

    /**
     * Handles logic for a ViewHolder instance
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val animal_image_view: ImageView = view.findViewById(R.id.animal_image_view)

        lateinit var click_listener: ItemClickListener

        init {
            // Make the checkbox selectable
            view.setOnClickListener(this)
        }

        fun setItemClickListener(ic: ItemClickListener) {
            this.click_listener = ic
        }

        /**
         * Uses the View.OnClickListener inheritance to allow each list item to have clickable functionality
         */
        override fun onClick(v: View) {
            this.click_listener.onItemClick(v, layoutPosition)
        }

        interface ItemClickListener {
            fun onItemClick(v: View, pos: Int)
        }
    }
}
