package sweng894.project.adopto.messages

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Chat
import sweng894.project.adopto.database.ChatRepository
import sweng894.project.adopto.database.FirebaseDataServiceUsers
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

    private var active_chat_id: String? = null
    private var other_user_id: String? = null
    private var is_chat_list_expanded = true
    private var pending_chat_id: String? = null

    private lateinit var firebase_data_service: FirebaseDataServiceUsers
    private var is_firebase_service_bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(class_name: ComponentName, service: IBinder) {
            firebase_data_service = (service as FirebaseDataServiceUsers.LocalBinder).getService()
            is_firebase_service_bound = true
            loadUserChats()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            is_firebase_service_bound = false
        }
    }

    override fun onStart() {
        super.onStart()
        bindToFirebaseService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromFirebaseService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        pending_chat_id?.let {
            loadMessagesForChat(it)
            pending_chat_id = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindToFirebaseService() {
        Intent(activity, FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromFirebaseService() {
        requireActivity().unbindService(connection)
    }

    private fun setupChatHeader() {
        binding.chatHeader.setOnClickListener {
            other_user_id?.let { user_id ->
                val intent = Intent(requireContext(), UserProfileViewingActivity::class.java)
                intent.putExtra("user_id", user_id)
                startActivity(intent)
            }
        }
    }

    fun setInitialChat(chat_id: String) {
        if (_binding != null) {
            loadMessagesForChat(chat_id)
        } else {
            pending_chat_id = chat_id
        }
    }

    private fun setupChatList() {
        chat_list_adapter =
            ChatListAdapter(is_collapsed = { !is_chat_list_expanded }) { selected_chat: Chat ->
                active_chat_id = selected_chat.chat_id
                updateInputState(true)
                loadMessagesForChat(selected_chat.chat_id)
            }
        binding.chatListRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecycler.adapter = chat_list_adapter
        updateInputState(false)
    }

    private fun setupToggleChatList() {
        val toggle_button: ImageButton = binding.expandCollapseButton
        toggle_button.setOnClickListener {
            is_chat_list_expanded = !is_chat_list_expanded

            val layout_params = binding.chatListContainer.layoutParams
            layout_params.width = if (is_chat_list_expanded) {
                // Expanded: full width for chat list
                resources.getDimensionPixelSize(R.dimen.chat_list_expanded_width)
            } else {
                // Collapsed: width equal to profile image (e.g., 48dp)
                resources.getDimensionPixelSize(R.dimen.chat_list_collapsed_width)
            }
            binding.chatListContainer.layoutParams = layout_params

            toggle_button.setImageResource(
                if (is_chat_list_expanded) R.drawable.ic_collapse else R.drawable.ic_expand
            )

            // Notify the adapter to rebind the views with the new layout
            chat_list_adapter.notifyItemRangeChanged(0, chat_list_adapter.itemCount)
        }
    }

    private fun updateInputState(is_enabled: Boolean) {
        binding.messageInputField.isEnabled = is_enabled
        binding.sendButton.isEnabled = is_enabled

        val alpha = if (is_enabled) 1.0f else 0.5f
        binding.messageInputField.alpha = alpha
        binding.sendButton.alpha = alpha
    }

    private fun setupMessageList() {
        message_adapter = MessageListAdapter()
        binding.messageRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.messageRecycler.adapter = message_adapter
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val message_text = binding.messageInputField.text.toString().trim()
            val chat_id = active_chat_id
            val sender_id = getCurrentUserId()

            if (message_text.isNotEmpty() && chat_id != null) {
                ChatRepository.sendMessage(
                    chat_id = chat_id,
                    sender_id = sender_id,
                    content = message_text,
                    onSuccess = {
                        binding.messageInputField.setText("")
                        loadMessagesForChat(chat_id)
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

    private fun loadUserChats() {
        val user_id = getCurrentUserId()
        ChatRepository.getUserChats(
            user_id = user_id,
            onResult = { chats ->
                chat_list_adapter.submitList(chats)
            },
            onError = {
                Log.e("UserMessagesFragment", "Failed to fetch chats", it)
            }
        )
    }

    private fun loadMessagesForChat(chat_id: String) {
        ChatRepository.getMessages(
            chat_id = chat_id,
            onResult = { messages ->
                message_adapter.submitList(messages)
            },
            onError = {
                Log.e("UserMessagesFragment", "Failed to load messages", it)
            }
        )

        val chat = chat_list_adapter.currentList.find { it.chat_id == chat_id }
        val current_user_id = getCurrentUserId()
        other_user_id = chat?.participant_ids?.firstOrNull { it != current_user_id }

        other_user_id?.let { user_id ->
            getUserData(user_id) { user ->
                binding.otherUserDisplayName.text = user?.display_name ?: "Unknown User"
                loadCloudStoredImageIntoImageView(
                    requireContext(),
                    user?.profile_image_path,
                    binding.otherUserProfileImage
                )
            }
        }
    }
}