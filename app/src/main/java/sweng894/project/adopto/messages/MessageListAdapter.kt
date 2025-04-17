package sweng894.project.adopto.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

    fun mergeMessages(newMessages: List<Message>) {
        if (newMessages.isEmpty()) return

        val sorted_new = newMessages.sortedBy { it.timestamp }

        // Quick equality check by message ID order
        val old_ids = messages.map { it.message_id }
        val new_ids = sorted_new.map { it.message_id }

        // If the messages are identical, skip updating
        if (old_ids == new_ids) return

        // If old messages are a prefix of the new list, do a minimal append
        if (old_ids.size < new_ids.size && new_ids.subList(0, old_ids.size) == old_ids) {
            val new_ones = sorted_new.subList(old_ids.size, new_ids.size)
            val insert_start = messages.size
            messages.addAll(new_ones)
            notifyItemRangeInserted(insert_start, new_ones.size)
        } else {
            // Fallback: update whole list (but avoid full rebind unless truly different)
            messages.clear()
            messages.addAll(sorted_new)
            notifyDataSetChanged()
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

