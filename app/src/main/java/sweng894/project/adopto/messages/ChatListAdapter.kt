package sweng894.project.adopto.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Chat
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.getUserData
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView
import java.time.Instant
import java.time.format.DateTimeFormatter

class ChatListAdapter(
    private val is_collapsed: () -> Boolean,
    private val onChatSelected: (Chat) -> Unit
) : ListAdapter<Chat, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_EXPANDED = 0
        private const val VIEW_TYPE_COLLAPSED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (is_collapsed()) VIEW_TYPE_COLLAPSED else VIEW_TYPE_EXPANDED
    }

    override fun onCreateViewHolder(parent: ViewGroup, view_type: Int): RecyclerView.ViewHolder {
        val layout_id = if (view_type == VIEW_TYPE_COLLAPSED)
            R.layout.item_chat_list_entry_collapsed else R.layout.item_chat_list_entry_expanded

        val view = LayoutInflater.from(parent.context).inflate(layout_id, parent, false)
        return if (view_type == VIEW_TYPE_COLLAPSED)
            CollapsedChatViewHolder(view) else ExpandedChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = getItem(position)
        if (holder is CollapsedChatViewHolder) {
            holder.bind(chat)
        } else if (holder is ExpandedChatViewHolder) {
            holder.bind(chat)
        }
    }

    inner class ExpandedChatViewHolder(val item_view: View) : RecyclerView.ViewHolder(item_view) {
        private val chat_name: TextView = item_view.findViewById(R.id.chat_name)
        private val last_updated: TextView = item_view.findViewById(R.id.last_updated)
        private val profile_image: ImageView = item_view.findViewById(R.id.other_user_image)

        fun bind(chat: Chat) {
            val current_user_id = getCurrentUserId()
            val other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }

            chat_name.text = "Loading..."

            if (other_user_id != null) {
                getUserData(other_user_id) { user ->
                    if (user != null) {
                        chat_name.text = user.display_name
                        if (!user.profile_image_path.isNullOrBlank()) {
                            loadCloudStoredImageIntoImageView(
                                item_view.context,
                                user.profile_image_path,
                                profile_image
                            )
                        } else {
                            profile_image.setImageResource(R.drawable.default_profile_image)
                        }
                    } else {
                        chat_name.text = "Unknown User"
                    }
                }
            } else {
                chat_name.text = chat.chat_name.ifBlank { "Unknown Chat" }
            }

            last_updated.text = try {
                val instant = Instant.parse(chat.last_updated)
                DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(instant)
            } catch (e: Exception) {
                ""
            }

            item_view.setOnClickListener { onChatSelected(chat) }
        }
    }

    inner class CollapsedChatViewHolder(val item_view: View) : RecyclerView.ViewHolder(item_view) {
        private val profile_image: ImageView = item_view.findViewById(R.id.other_user_image)

        fun bind(chat: Chat) {
            val current_user_id = getCurrentUserId()
            val other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }

            if (other_user_id != null) {
                getUserData(other_user_id) { user ->
                    if (user != null && !user.profile_image_path.isNullOrBlank()) {
                        loadCloudStoredImageIntoImageView(
                            item_view.context,
                            user.profile_image_path,
                            profile_image
                        )
                    } else {
                        profile_image.setImageResource(R.drawable.default_profile_image)
                    }
                }
            } else {
                profile_image.setImageResource(R.drawable.default_profile_image)
            }

            item_view.setOnClickListener { onChatSelected(chat) }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(old_item: Chat, new_item: Chat): Boolean =
            old_item.chat_id == new_item.chat_id

        override fun areContentsTheSame(old_item: Chat, new_item: Chat): Boolean =
            old_item == new_item
    }
}
