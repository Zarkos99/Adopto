package sweng894.project.adopto.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.database.FirebaseDataService
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView

/**
 * The adaptor for a recyclerview of sunset posts with the capability to have selectable items or not
 */
class ProfileSavedAnimalsAdapter(
    private val context: Context,
    private val firebase_data_service: FirebaseDataService
) :
    RecyclerView.Adapter<ProfileSavedAnimalsAdapter.ViewHolder>() {

    /**
     * Handles creation of the view holder for each item in the recyclerview
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create new view
        val view = LayoutInflater.from(context)
            .inflate(R.layout.profile_saved_animals_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Handles binding of the view holder for each item in the recyclerview
     */
    override fun onBindViewHolder(holder: ViewHolder, dont_use: Int) {
        val user = firebase_data_service.current_user_data
        val animal = user?.saved_animal_ids?.get(holder.adapterPosition)

        // TODO: Find cloud-stored animal profile picture path
        val animal_profile_pic = animal

        loadCloudStoredImageIntoImageView(
            context,
            animal_profile_pic,
            holder.saved_animal_image_view
        )

        // Provides logic to track all selected products as the user selects them
        holder.setItemClickListener(object : ViewHolder.ItemClickListener {
            override fun onItemClick(v: View, pos: Int) {
                val current_animal =
                    firebase_data_service.current_user_data?.saved_animal_ids?.get(holder.adapterPosition)

                //TODO: Go to animal page
            }
        })
    }

    /**
     * Gets all of the items in the recyclerview
     */
    override fun getItemCount(): Int {
        // Returns 0 if posts array is null else returns current size of posts array
        return firebase_data_service.current_user_data?.saved_animal_ids?.size ?: return 0
    }

    /**
     * Handles logic for a ViewHolder instance
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val saved_animal_image_view: ImageView = view.findViewById(R.id.saved_animal_image_view)

        lateinit var sunset_click_listener: ItemClickListener

        init {
            // Make the checkbox selectable
            view.setOnClickListener(this)
        }

        fun setItemClickListener(ic: ItemClickListener) {
            this.sunset_click_listener = ic
        }

        /**
         * Uses the View.OnClickListener inheritance to allow each list item to have clickable functionality
         */
        override fun onClick(v: View) {
            this.sunset_click_listener.onItemClick(v, layoutPosition)
        }

        interface ItemClickListener {
            fun onItemClick(v: View, pos: Int)
        }
    }
}
