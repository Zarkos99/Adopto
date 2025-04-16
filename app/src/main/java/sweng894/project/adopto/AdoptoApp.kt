package sweng894.project.adopto

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.libraries.places.api.Places
import com.google.firebase.messaging.FirebaseMessaging
import sweng894.project.adopto.data.VectorUtils

open class AdoptoApp : Application() {
    companion object {
        var _instance: AdoptoApp? = null
        val instance: AdoptoApp
            get() = _instance
                ?: throw IllegalStateException("App instance is not initialized. Ensure it is declared in AndroidManifest.xml.")

        fun isInitialized(): Boolean = _instance != null
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        // Initialize Google Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_places_key))
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "default_channel",
            "General Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Adopto alerts for messages and updates"
            enableLights(true)
            lightColor = resources.getColor(R.color.primary_button, theme)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

/**
 * Get string value from application's strings regardless of scope
 */
object Strings {
    fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
        if (!AdoptoApp.isInitialized()) {
            throw IllegalStateException("App.instance is not initialized. Ensure App is properly configured in the manifest.")
        }
        return AdoptoApp.instance.getString(stringRes, *formatArgs)
    }
}