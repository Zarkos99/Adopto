package sweng894.project.adopto.messages

import android.util.Log
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
import sweng894.project.adopto.data.User
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.getUserData
import sweng894.project.adopto.firebase.loadCloudStoredImageIntoImageView
import java.time.Instant
import java.time.format.DateTimeFormatter

class ChatListAdapter(
    private var selected_chat_id: String? = null,
    private val isCollapsed: () -> Boolean,
    private val onChatSelected: (Chat) -> Unit
) : ListAdapter<Chat, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private var unread_map: Map<String, Boolean> = emptyMap()
    private val user_cache = mutableMapOf<String, User?>()
    private val image_url_cache = mutableMapOf<String, String>()

    fun updateUnreadMap(new_map: Map<String, Boolean>) {
        Log.d("ChatListAdapter", "Unread map updated: $new_map")
        unread_map = new_map
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_EXPANDED = 0
        private const val VIEW_TYPE_COLLAPSED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCollapsed()) VIEW_TYPE_COLLAPSED else VIEW_TYPE_EXPANDED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_COLLAPSED)
            R.layout.item_chat_list_entry_collapsed else R.layout.item_chat_list_entry_expanded

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return if (viewType == VIEW_TYPE_COLLAPSED)
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

    inner class ExpandedChatViewHolder(private val item_view: View) :
        RecyclerView.ViewHolder(item_view) {

        private val chat_name_text_view: TextView = item_view.findViewById(R.id.chat_name)
        private val last_updated_text_view: TextView = item_view.findViewById(R.id.last_updated)
        private val profile_image: ImageView = item_view.findViewById(R.id.other_user_image)
        private val unread_badge: View = item_view.findViewById(R.id.unread_badge)

        fun bind(chat: Chat) {
            val current_user_id = getCurrentUserId()
            val other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }

            val is_selected = chat.chat_id == selected_chat_id
            unread_badge.visibility =
                if (!is_selected && unread_map[chat.chat_id] == true) View.VISIBLE else View.GONE


            if (other_user_id != null) {
                val cached_user = user_cache[other_user_id]
                if (cached_user != null) {
                    updateUserUI(cached_user)
                } else {
                    getUserData(other_user_id) { user ->
                        user_cache[other_user_id] = user
                        if (adapterPosition != RecyclerView.NO_POSITION &&
                            getItem(adapterPosition).chat_id == chat.chat_id
                        ) {
                            updateUserUI(user)
                        }
                    }
                }
            } else {
                chat_name_text_view.text = chat.chat_name.ifBlank { "Unknown Chat" }
                profile_image.setImageResource(R.drawable.default_profile_image)
                profile_image.tag = null
            }

            last_updated_text_view.text = try {
                val instant = Instant.parse(chat.last_updated)
                DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(instant)
            } catch (e: Exception) {
                ""
            }

            item_view.setOnClickListener { onChatSelected(chat) }
        }

        private fun updateUserUI(user: User?) {
            val new_name =
                if (user?.display_name.isNullOrEmpty()) "Unnamed User" else user?.display_name
            if (chat_name_text_view.text != new_name) {
                chat_name_text_view.text = new_name
            }

            val image_path = user?.profile_image_path
            val current_tag = profile_image.tag as? String
            if (!image_path.isNullOrBlank() && image_path != current_tag) {
                profile_image.tag = image_path
                loadCloudStoredImageIntoImageView(
                    item_view.context,
                    image_path,
                    profile_image,
                    image_url_cache
                )
            } else if (image_path.isNullOrBlank() && current_tag != null) {
                profile_image.setImageResource(R.drawable.default_profile_image)
                profile_image.tag = null
            }
        }
    }

    inner class CollapsedChatViewHolder(private val item_view: View) :
        RecyclerView.ViewHolder(item_view) {

        private val profile_image: ImageView = item_view.findViewById(R.id.other_user_image)
        private val unread_badge: View = item_view.findViewById(R.id.unread_badge)

        fun bind(chat: Chat) {
            val current_user_id = getCurrentUserId()
            val other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }

            val is_selected = chat.chat_id == selected_chat_id
            unread_badge.visibility =
                if (!is_selected && unread_map[chat.chat_id] == true) View.VISIBLE else View.GONE

            if (other_user_id != null) {
                val cached_user = user_cache[other_user_id]
                if (cached_user != null) {
                    updateUserUI(cached_user)
                } else {
                    getUserData(other_user_id) { user ->
                        user_cache[other_user_id] = user
                        if (adapterPosition != RecyclerView.NO_POSITION &&
                            getItem(adapterPosition).chat_id == chat.chat_id
                        ) {
                            updateUserUI(user)
                        }
                    }
                }
            } else {
                profile_image.setImageResource(R.drawable.default_profile_image)
                profile_image.tag = null
            }

            item_view.setOnClickListener { onChatSelected(chat) }
        }

        private fun updateUserUI(user: User?) {
            val image_path = user?.profile_image_path
            val current_tag = profile_image.tag as? String
            if (!image_path.isNullOrBlank() && image_path != current_tag) {
                profile_image.tag = image_path
                loadCloudStoredImageIntoImageView(
                    item_view.context,
                    image_path,
                    profile_image,
                    image_url_cache
                )
            } else if (image_path.isNullOrBlank() && current_tag != null) {
                profile_image.setImageResource(R.drawable.default_profile_image)
                profile_image.tag = null
            }
        }
    }

    fun setSelectedChatId(chat_id: String?) {
        selected_chat_id = chat_id
        notifyDataSetChanged() // rebind all items to update the badge visibility
    }


    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(old_item: Chat, new_item: Chat): Boolean =
            old_item.chat_id == new_item.chat_id

        override fun areContentsTheSame(old_item: Chat, new_item: Chat): Boolean =
            old_item == new_item
    }
}
