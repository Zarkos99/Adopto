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

    // Array of product names
    var selected_animal_ids = ArrayList<String>()
    var item_selected_callbacks = ArrayList<(() -> Unit)>()

    /**
     * Handles creation of the view holder for each item in the recyclerview
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create new view
        val view = LayoutInflater.from(context)
            .inflate(R.layout.profile_saved_animals_item, parent, false)

        unselectDeletedAnimals()
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
            holder.sunset_image_view
        )

        // Below line fixes a bug where deleted sunset checkboxes would positionally
        // associate to next undeleted sunsets at same position by defaulting onBind checkbox
        // to not visible
        holder.sunset_checkbox.visibility = View.GONE

        // Provides logic to track all selected products as the user selects them
        holder.setItemClickListener(object : ViewHolder.ItemClickListener {
            override fun onItemClick(v: View, pos: Int) {
                val current_animal =
                    firebase_data_service.current_user_data?.saved_animal_ids?.get(holder.adapterPosition)

                if (selected_animal_ids.contains(current_animal)) {
                    holder.sunset_checkbox.visibility = View.GONE
                    selected_animal_ids.remove(current_animal)
                } else {
                    if (current_animal != null) {
                        selected_animal_ids.add(current_animal)
                    }
                    holder.sunset_checkbox.visibility = View.VISIBLE
                }

                callItemSelectedCallbacks()
            }
        })
    }

    fun registerItemSelectedCallback(callback: (() -> Unit)) {
        item_selected_callbacks.add(callback)
    }

    fun callItemSelectedCallbacks() {
        if (item_selected_callbacks.size > 0) {
            for (callback in item_selected_callbacks) {
                callback()
            }
        }
    }

    /**
     * Gets all of the selected items
     */
    fun getSelectedAnimalIds(): ArrayList<String> {
        return selected_animal_ids
    }

    fun unselectDeletedAnimals() {
        val user = firebase_data_service.current_user_data
        val sunset_posts = user?.saved_animal_ids

        if (!sunset_posts.isNullOrEmpty()) {
            selected_animal_ids.removeIf {
                val deleted_animal_id = it;
                !sunset_posts.any { obj -> obj == deleted_animal_id }
            }
        } else {
            selected_animal_ids.clear()
        }
        callItemSelectedCallbacks()
    }

    /**
     * Gets all of the items in the recyclerview
     */
    fun getSelectedItemCount(): Int {
        return selected_animal_ids.size
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
        val sunset_image_view: ImageView = view.findViewById(R.id.saved_animal_image_view)
        val sunset_checkbox: FloatingActionButton = view.findViewById(R.id.checkbox_button)

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
