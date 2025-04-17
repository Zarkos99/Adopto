package sweng894.project.adopto.firebase

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import sweng894.project.adopto.NavigationBaseActivity
import sweng894.project.adopto.R
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User

//Service for app notifications
class AdoptoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM Service", "New token: $token, uploading to Firestore")

        updateDataField(
            FirebaseCollections.USERS,
            getCurrentUserId(),
            User::fcm_token,
            token, onUploadSuccess = {
                Log.d("FCM Service", "New Token successfully saved to Firestore")
            }, onUploadFailure = {
                Log.w("FCM Service", "Failed to save token")
            })
    }

    override fun onMessageReceived(remote_message: RemoteMessage) {
        val data = remote_message.data
        Log.d("FCM", "Notification received: $data")

        if (data.isNotEmpty()) {
            val incoming_chat_id = remote_message.data["chat_id"]

            val is_in_active_chat = incoming_chat_id == ChatState.active_chat_id
            if (is_in_active_chat) {
                Log.d("FCM", "Skipping notification: user is already in chat $incoming_chat_id")
                return
            }

            val intent = Intent(this, NavigationBaseActivity::class.java).apply {
                putExtra("initial_tab", R.id.navigation_messages)
                putExtra("open_chat_id", incoming_chat_id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pending_intent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remote_message.notification?.let {
                sendLocalNotification(it.title, it.body, pending_intent)
            }
        }
    }

    private fun sendLocalNotification(
        title: String?,
        body: String?,
        pending_intent: PendingIntent?
    ) {
        val notificationManager = NotificationManagerCompat.from(this)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val builder = NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.ic_adopto_outline)
                .setContentTitle(title ?: "Notification")
                .setContentText(body ?: "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending_intent)
                .setColor(ContextCompat.getColor(this, R.color.card_background))
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
//        TODO: .setSound(Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/raw/custom_sound"))
//        TODO: .setVibrate(longArrayOf(0, 200, 100, 200))
//        TODO: .addAction( //Quick action for message reply
//                    R.drawable.ic_reply,
//                    "Reply",
//                    pendingIntentToReply
//                )

            try {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                Log.e("FCM", "Notification permission denied", e)
            }
        } else {
            Log.w("FCM", "Notifications are disabled or permission not granted.")
            // TODO: show UI to request permission
        }
    }
}
