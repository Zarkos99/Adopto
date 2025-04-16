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

class MessageListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender_id == getCurrentUserId()) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = inflater.inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(messages[position])
    }

    fun mergeMessages(new_messages: List<Message>) {
        // Track current IDs for deduplication
        val existing_ids = messages.map { it.message_id }.toSet()

        // Filter and sort new messages
        val new_unique_messages = new_messages
            .filter { it.message_id !in existing_ids }
            .sortedBy { it.timestamp }

        if (new_unique_messages.isNotEmpty()) {
            val insertStart = messages.size
            messages.addAll(new_unique_messages)
            notifyItemRangeInserted(insertStart, new_unique_messages.size)
        }
    }


    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged() // Only used when switching chats
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(message: Message) {
            messageText.text = message.content
        }
    }
}

