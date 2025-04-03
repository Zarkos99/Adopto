package sweng894.project.adopto.profile.Tabs

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.database.deleteImagesFromCloudStorage
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.database.removeFromDataFieldList
import sweng894.project.adopto.database.syncDatabaseForRemovedImages

enum class AdapterClickability {
    NOT_CLICKABLE,
    DOUBLE_CLICKABLE
}


/**
 * The adaptor for a recyclerview of sunset posts with the capability to have selectable items or not
 */
class AnimalProfileViewingImagesAdapter(
    private val context: Context,
    private val current_animal: Animal,
    private val clickability: AdapterClickability = AdapterClickability.NOT_CLICKABLE
) :
    RecyclerView.Adapter<AnimalProfileViewingImagesAdapter.ClickableViewHolder>() {

    private var m_image_uris: MutableList<Uri> = mutableListOf()// Stores the image URIs

    /**
     * Handles creation of the view holder for each item in the recyclerview
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClickableViewHolder {
        // create new view
        val view = LayoutInflater.from(context)
            .inflate(R.layout.animal_profile_viewing_images_item, parent, false)
        return ClickableViewHolder(view, clickability)
    }

    /**
     * Handles binding of the view holder for each item in the recyclerview
     */
    override fun onBindViewHolder(holder: ClickableViewHolder, position: Int) {
        if (m_image_uris.isNotEmpty()) {
            val imageUri = m_image_uris[position]

            // Pull image from database
            loadCloudStoredImageIntoImageView(
                context,
                imageUri.path,
                holder.additional_animal_image_view
            )
        }


        if (clickability != AdapterClickability.NOT_CLICKABLE) {
            //If the delete item is selected, remove the item from the list
            holder.delete_image_button.setOnClickListener {
                // Remove image from cloud storage and databased animal's additional images field
                val images_to_delete = arrayOf(m_image_uris[position].path!!)
                deleteImagesFromCloudStorage(images_to_delete)
                removeFromDataFieldList(
                    FirebaseCollections.ANIMALS,
                    current_animal.animal_id,
                    Animal::supplementary_image_paths,
                    images_to_delete
                )
                {
                    syncDatabaseForRemovedImages(images_to_delete)
                }
                removeItem(position)

            }
        }

        holder.delete_image_button.visibility = View.GONE

        if (clickability != AdapterClickability.NOT_CLICKABLE) {
            holder.setItemClickListener(object : ClickableViewHolder.ItemClickListener {
                override fun onItemClick(v: View, pos: Int) {
                    // Toggle delete button visibility
                    holder.delete_image_button.visibility =
                        if (holder.delete_image_button.visibility == View.GONE)
                            View.VISIBLE
                        else
                            View.GONE
                }
            })
        }
    }

    /**
     * Adds a new image to the list and updates the RecyclerView.
     */
    fun setItems(new_image_uris: ArrayList<Uri>) {
        m_image_uris.clear()
        m_image_uris.addAll(new_image_uris)
        notifyDataSetChanged()
    }

    /**
     * Adds a new image to the list and updates the RecyclerView.
     */
    fun addItem(new_image_uri: Uri) {
        Log.d("TRACE", "Added animal image to adapter $new_image_uri")
        m_image_uris.add(new_image_uri) // Add new image URI to list
        notifyItemInserted(m_image_uris.size - 1) // Notify RecyclerView about new item
    }

    /**
     * Removes an image from the list and updates the RecyclerView.
     */
    private fun removeItem(position: Int) {
        if (position in m_image_uris.indices) {
            m_image_uris.removeAt(position) // Remove image URI from list
            notifyItemRemoved(position) // Notify adapter about removal
            notifyItemRangeChanged(position, m_image_uris.size) // Refresh list positions
        }
    }

    fun getImages(): ArrayList<Uri> {
        return m_image_uris as ArrayList<Uri>
    }

    /**
     * Returns the total number of images in the dataset.
     */
    override fun getItemCount(): Int = m_image_uris.size

    /**
     * Handles logic for a ViewHolder instance
     */
    class ClickableViewHolder(view: View, clickable: AdapterClickability) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val additional_animal_image_view: ImageView =
            view.findViewById(R.id.additional_animal_image_view)
        val delete_image_button: FloatingActionButton =
            view.findViewById(R.id.delete_image_button)

        lateinit var click_listener: ItemClickListener

        init {
            if (clickable != AdapterClickability.NOT_CLICKABLE) {
                // Make the checkbox selectable
                view.setOnClickListener(this)
            }
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
