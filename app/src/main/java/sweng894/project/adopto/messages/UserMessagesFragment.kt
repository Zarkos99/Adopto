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
import sweng894.project.adopto.firebase.ChatRepository
import sweng894.project.adopto.firebase.ChatsListenerViewModel
import sweng894.project.adopto.firebase.MessagesListenerViewModel
import sweng894.project.adopto.firebase.getCurrentUserId
import sweng894.project.adopto.firebase.getUserData
import sweng894.project.adopto.firebase.loadCloudStoredImageIntoImageView
import sweng894.project.adopto.databinding.UserMessagesFragmentBinding
import sweng894.project.adopto.firebase.ChatState
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
    private var pending_scroll_to_bottom = false
    private var pending_new_chat_messages = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user_id = getCurrentUserId()
        if (!user_id.isNullOrEmpty()) {
            chats_listener_view_model.startListening(user_id)
        }
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

        // Clear the selected chat
        chats_listener_view_model.selectChat(null)
        ChatState.active_chat_id = null
    }

    private fun setupChatList() {
        chat_list_adapter = ChatListAdapter(
            isCollapsed = { !is_chat_list_expanded },
            onChatSelected = { selected_chat ->
                if (active_chat_id != selected_chat.chat_id) {
                    active_chat_id = selected_chat.chat_id
                    chats_listener_view_model.selectChat(selected_chat)
                    Log.d(
                        "UserMessagesFragment",
                        "Clearing messages list in preparation of new chat"
                    )
                    pending_scroll_to_bottom = true
                    pending_new_chat_messages = true
                }
                scrollToBottom()
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

            if (!sender_id.isNullOrEmpty()) {
                if (message_text.isNotEmpty()) {
                    ChatRepository.sendMessage(
                        chat_id = chat_id,
                        sender_id = sender_id,
                        receiver_id = other_user_id!!,
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
    }

    private fun observeChats() {
        chats_listener_view_model.user_chats.observe(viewLifecycleOwner) { chat_list ->
            chat_list_adapter.submitList(chat_list) {
                Log.d("UserMessagesFragment", "Received and updated chat list")
                // If a pending chat ID was set, wait for it to appear
                tryOpeningPendingChat(chat_list)
            }

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
        if (pending_chat_id != null) {
            val pending_chat = chat_list.find { it.chat_id == pending_chat_id }
            Log.d("UserMessagesFragment", "Attempting to open pending chat: $pending_chat")
            if (pending_chat != null) {
                ChatState.active_chat_id = pending_chat.chat_id
                chat_list_adapter.setSelectedChatId(pending_chat.chat_id)
                chats_listener_view_model.selectChat(pending_chat)
                displayMessagesForChat(pending_chat)
                pending_chat_id = null
            }
        }
    }

    fun observeSelectedChat() {
        chats_listener_view_model.selected_chat_id.observe(viewLifecycleOwner) { selected_chat ->
            if (selected_chat != null) {
                Log.d(
                    "UserMessagesFragment",
                    "Updating displayed messages for newly selected chat: $selected_chat"
                )
                ChatState.active_chat_id = selected_chat.chat_id
                chat_list_adapter.setSelectedChatId(selected_chat.chat_id)
                displayMessagesForChat(selected_chat)
            }
        }
    }

    private fun observeMessages() {
        messages_listener_view_model.messages.observe(viewLifecycleOwner) { messages ->
            Log.d("UserMessagesFragment", "Received and merged messages")

            val recycler_view = binding.messageRecycler
            val layout_manager = recycler_view.layoutManager as? LinearLayoutManager
            val is_at_bottom_scrollbar = layout_manager?.let {
                val last_visible = it.findLastCompletelyVisibleItemPosition()
                last_visible >= message_adapter.itemCount - 2 // a bit lenient buffer
            } ?: false

            if (pending_new_chat_messages) {
                message_adapter.setMessages(messages)
                pending_new_chat_messages = false
            } else {
                message_adapter.mergeMessages(messages)
            }

            if (pending_scroll_to_bottom || is_at_bottom_scrollbar) {
                scrollToBottom()
                pending_scroll_to_bottom = false
            }
        }
    }

    private fun displayMessagesForChat(chat: Chat) {
        active_chat_id = chat.chat_id
        updateInputState(true)

        // Switch the listener to this chat
        messages_listener_view_model.listenToMessages(chat.chat_id)

        // Mark as read in the database
        val current_user_id = getCurrentUserId()
        if (!current_user_id.isNullOrEmpty()) {
            ChatRepository.markChatAsReadDebounced(chat.chat_id, current_user_id)

            val new_other_user_id = chat.participant_ids.firstOrNull { it != current_user_id }
            other_user_id = new_other_user_id
            updateChatHeaderUser(other_user_id)
        }
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

    fun updateInputState(is_enabled: Boolean) {

        binding.messageInputField.isEnabled = is_enabled
        binding.sendButton.isEnabled = is_enabled

        val alpha = if (is_enabled) 1.0f else 0.5f
        binding.messageInputField.alpha = alpha
        binding.sendButton.alpha = alpha
    }

    fun setInitialChat(chat_id: String) {
        Log.d("UserMessagesFragment", "Received a pending_chat_id: $chat_id")
        pending_chat_id = chat_id

        // Force attempt if chats are already loaded
        val current_chats = chats_listener_view_model.user_chats.value
        if (!current_chats.isNullOrEmpty()) {
            tryOpeningPendingChat(current_chats)
        }
    }

    private fun scrollToBottom() {
        binding.messageRecycler.scrollToPosition(message_adapter.itemCount - 1)
    }
}
