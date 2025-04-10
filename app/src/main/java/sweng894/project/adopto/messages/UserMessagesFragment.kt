package sweng894.project.adopto.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Chat
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.ChatRepository
import sweng894.project.adopto.database.ChatsListenerViewModel
import sweng894.project.adopto.database.MessagesListenerViewModel
import sweng894.project.adopto.database.getCurrentUserId
import sweng894.project.adopto.database.getUserData
import sweng894.project.adopto.database.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.databinding.UserMessagesFragmentBinding
import sweng894.project.adopto.profile.UserProfileViewingActivity

class UserMessagesFragment : Fragment() {

    private var _binding: UserMessagesFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var chat_list_adapter: ChatListAdapter
    private lateinit var message_adapter: MessageListAdapter

    private val chats_listener_view_model: ChatsListenerViewModel by activityViewModels()
    private val messages_listener_view_model: MessagesListenerViewModel by activityViewModels()

    private var active_chat_id: String? = null
    private var other_user_id: String? = null
    private var is_chat_list_expanded = true
    private var pending_chat_id: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chats_listener_view_model.startListening(getCurrentUserId())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserMessagesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChatList()
        setupMessageList()
        setupToggleChatList()
        setupSendButton()
        setupChatHeader()
        observeChats()
        observeMessages()
        observeSelectedChat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupChatList() {
        chat_list_adapter = ChatListAdapter(
            isCollapsed = { !is_chat_list_expanded },
            onChatSelected = { selected_chat ->
                active_chat_id = selected_chat.chat_id
                chats_listener_view_model.selectChat(selected_chat)
            }
        )
        binding.chatListRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecycler.adapter = chat_list_adapter
        updateInputState(false)
    }

    private fun setupMessageList() {
        message_adapter = MessageListAdapter()
        binding.messageRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.messageRecycler.adapter = message_adapter
    }

    private fun setupToggleChatList() {
        binding.expandCollapseButton.setOnClickListener {
            is_chat_list_expanded = !is_chat_list_expanded
            val layout_params = binding.chatListContainer.layoutParams
            layout_params.width = if (is_chat_list_expanded) {
                resources.getDimensionPixelSize(R.dimen.chat_list_expanded_width)
            } else {
                resources.getDimensionPixelSize(R.dimen.chat_list_collapsed_width)
            }
            binding.chatListContainer.layoutParams = layout_params

            binding.expandCollapseButton.setImageResource(
                if (is_chat_list_expanded) R.drawable.ic_collapse else R.drawable.ic_expand
            )

            chat_list_adapter.notifyItemRangeChanged(0, chat_list_adapter.itemCount)
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val message_text = binding.messageInputField.text.toString().trim()
            val chat_id = active_chat_id ?: return@setOnClickListener
            val sender_id = getCurrentUserId()

            if (message_text.isNotEmpty()) {
                ChatRepository.sendMessage(
                    chat_id = chat_id,
                    sender_id = sender_id,
                    content = message_text,
                    onSuccess = {
                        binding.messageInputField.setText("")
                        scrollToBottom() //Scroll after send
                        // No need to update messages — observer will handle it
                    },
                    onError = {
                        Toast.makeText(
                            requireContext(),
                            "Failed to send message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun observeChats() {
        chats_listener_view_model.user_chats.observe(viewLifecycleOwner) { chat_list ->
            chat_list_adapter.submitList(chat_list)

            // If a pending chat ID was set, wait for it to appear
            tryOpeningPendingChat(chat_list)

            // If already viewing a chat, keep it open
            val selected_chat = chat_list.find { it.chat_id == active_chat_id }
            if (selected_chat != null) {
                displayMessagesForChat(selected_chat)
            }

            // Listen for updates to the unread state
            chats_listener_view_model.unread_map.observe(viewLifecycleOwner) { unread_map ->
                chat_list_adapter.updateUnreadMap(unread_map)
            }
        }
    }

    private fun tryOpeningPendingChat(chat_list: List<Chat>) {
        Log.d("DEBUG", "pending_chat_id: $pending_chat_id")
        if (pending_chat_id != null) {
            val pending_chat = chat_list.find { it.chat_id == pending_chat_id }
            Log.d("DEBUG", "Attempting to open pending chat: $pending_chat")
            if (pending_chat != null) {
                chats_listener_view_model.selectChat(pending_chat)
                pending_chat_id = null
            }
        }
    }

    fun observeSelectedChat() {
        chats_listener_view_model.selected_chat_id.observe(viewLifecycleOwner) { selected_chat ->

            if (selected_chat != null) {
                displayMessagesForChat(selected_chat)
            }
        }
    }

    private fun observeMessages() {
        messages_listener_view_model.messages.observe(viewLifecycleOwner) { messages ->
            message_adapter.submitList(messages) {
                if (active_chat_id != null) {
                    // Scroll after list is updated
                    binding.messageRecycler.post {
                        scrollToBottom() //Scroll after send
                    }
                }
            }
        }
    }

    private fun displayMessagesForChat(chat: Chat) {
        active_chat_id = chat.chat_id
        updateInputState(true)

        // Switch the listener to this chat
        messages_listener_view_model.listenToMessages(chat.chat_id)

        // Mark as read in the database
        ChatRepository.markChatAsReadDebounced(chat.chat_id, getCurrentUserId())

        val current_user_id = getCurrentUserId()
        val new_other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }
        other_user_id = new_other_user_id

        updateChatHeaderUser(other_user_id)
    }


    private fun setupChatHeader() {
        binding.chatHeader.setOnClickListener {
            other_user_id?.let { user_id ->
                val intent = Intent(requireContext(), UserProfileViewingActivity::class.java)
                intent.putExtra(User::user_id.name, user_id)
                startActivity(intent)
            }
        }
    }

    private fun updateChatHeaderUser(user_id: String?) {
        if (user_id.isNullOrEmpty()) {
            binding.otherUserDisplayName.text = "Unnamed User"
            binding.otherUserProfileImage.setImageResource(R.drawable.default_profile_image)
            return
        }

        getUserData(user_id) { user ->
            binding.otherUserDisplayName.text =
                if (user?.display_name.isNullOrEmpty()) "Unnamed User" else user?.display_name
            loadCloudStoredImageIntoImageView(
                requireContext(),
                user?.profile_image_path,
                binding.otherUserProfileImage
            )
        }
    }

    private fun updateInputState(is_enabled: Boolean) {

        binding.messageInputField.isEnabled = is_enabled
        binding.sendButton.isEnabled = is_enabled

        val alpha = if (is_enabled) 1.0f else 0.5f
        binding.messageInputField.alpha = alpha
        binding.sendButton.alpha = alpha
    }

    fun setInitialChat(chat_id: String) {
        pending_chat_id = chat_id
    }

    private fun scrollToBottom() {
        binding.messageRecycler.scrollToPosition(message_adapter.itemCount - 1)
    }
}
