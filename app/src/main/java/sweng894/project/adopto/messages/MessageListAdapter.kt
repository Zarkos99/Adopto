package sweng894.project.adopto.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Message
import sweng894.project.adopto.firebase.getCurrentUserId

class MessageListAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.sender_id == getCurrentUserId()) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, view_type: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout_id = if (view_type == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = inflater.inflate(layout_id, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(getItem(position))
    }

    fun addMessage(message: Message) {
        val new_list = currentList.toMutableList().apply { add(message) }
        submitList(new_list)
    }

    class MessageViewHolder(item_view: View) : RecyclerView.ViewHolder(item_view) {
        private val message_text: TextView = item_view.findViewById(R.id.message_text)

        fun bind(message: Message) {
            message_text.text = message.content
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(old_item: Message, new_item: Message): Boolean =
            old_item.message_id == new_item.message_id

        override fun areContentsTheSame(old_item: Message, new_item: Message): Boolean =
            old_item == new_item
    }
}
