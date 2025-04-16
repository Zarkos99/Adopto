package sweng894.project.adopto.firebase

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import sweng894.project.adopto.R
import sweng894.project.adopto.data.FirebaseCollections
import sweng894.project.adopto.data.User

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

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendLocalNotification(it.title, it.body)
        }
    }

    private fun sendLocalNotification(title: String?, body: String?) {
        val notificationManager = NotificationManagerCompat.from(this)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val builder = NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.ic_adopto_outline)
                .setContentTitle(title ?: "Notification")
                .setContentText(body ?: "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
