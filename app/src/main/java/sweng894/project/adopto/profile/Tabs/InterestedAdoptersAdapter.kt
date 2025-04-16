package sweng894.project.adopto.profile.Tabs

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sweng894.project.adopto.R
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.profile.UserProfileViewingActivity

class InterestedAdoptersAdapter(
    private val context: Context,
    private var adopters: List<User> = listOf()
) : RecyclerView.Adapter<InterestedAdoptersAdapter.UserMiniProfileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserMiniProfileViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.user_profile_viewing_mini_activity, parent, false)
        return UserMiniProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserMiniProfileViewHolder, position: Int) {
        val user = adopters[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = adopters.size

    fun updateUsers(newList: List<User>) {
        adopters = newList
        notifyDataSetChanged()
    }

    inner class UserMiniProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profile_image: ImageView = itemView.findViewById(R.id.user_profile_image)
        private val display_name: TextView = itemView.findViewById(R.id.user_display_name)
        private val biography: TextView = itemView.findViewById(R.id.user_biography)

        fun bind(user: User) {
            display_name.text =
                user.display_name.ifEmpty { "Unnamed User" }
            biography.text =
                if (user.biography.isNullOrEmpty()) "No biography." else user.biography
            loadCloudStoredImageIntoImageView(context, user.profile_image_path, profile_image)

            itemView.setOnClickListener {
                val intent = Intent(context, UserProfileViewingActivity::class.java)
                intent.putExtra(User::user_id.name, user.user_id)
                context.startActivity(intent)
            }
        }
    }
}
