package sweng894.project.adopto.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sweng894.project.adopto.R

/**
 * The adaptor for a recyclerview of sunset posts with the capability to have selectable items or not
 */
class AnimalProfileAdditionalImagesAdapter(
    private val context: Context,
    private val max_images: Int = 5 // Set a maximum limit
) :
    RecyclerView.Adapter<AnimalProfileAdditionalImagesAdapter.ViewHolder>() {

    private var m_image_uris: MutableList<Uri> = mutableListOf()// Stores the image URIs

    /**
     * Handles creation of the view holder for each item in the recyclerview
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create new view
        val view = LayoutInflater.from(context)
            .inflate(R.layout.animal_profile_additional_animal_images_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Handles binding of the view holder for each item in the recyclerview
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (m_image_uris.isNotEmpty()) {
            val imageUri = m_image_uris[position]
            holder.additional_animal_image_view.setImageURI(imageUri)
        }

        // Provides logic to track all selected products as the user selects them
        holder.setItemClickListener(object : ViewHolder.ItemClickListener {
            override fun onItemClick(v: View, pos: Int) {
                removeItem(pos)
            }
        })
    }

    /**
     * Adds a new image to the list and updates the RecyclerView.
     */
    fun addItem(new_image_uri: Uri) {
        if (m_image_uris.size < max_images) {
            Log.d("TRACE", "Added animal image to adapter $new_image_uri")
            m_image_uris.add(new_image_uri) // Add new image URI to list
            notifyItemInserted(m_image_uris.size - 1) // Notify RecyclerView about new item
        } else {
            Log.d("TRACE", "Error adding animal image $new_image_uri")
            //Display error message
            Toast.makeText(
                context,
                "Max image limit reached ($max_images). Cannot add more images.",
                Toast.LENGTH_LONG
            ).show()
        }
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
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val additional_animal_image_view: ImageView =
            view.findViewById(R.id.additional_animal_image_view)

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
