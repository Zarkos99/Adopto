package sweng894.project.adopto

import android.app.Application
import androidx.annotation.StringRes

class App : Application() {
    companion object {
        private var _instance: App? = null
        val instance: App
            get() = _instance
                ?: throw IllegalStateException("App instance is not initialized. Ensure it is declared in AndroidManifest.xml.")

        fun isInitialized(): Boolean = _instance != null
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
    }
}

/**
 * Get string value from application's strings regardless of scope
 */
object Strings {
    fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
        if (!App.isInitialized()) {
            throw IllegalStateException("App.instance is not initialized. Ensure App is properly configured in the manifest.")
        }
        return App.instance.getString(stringRes, *formatArgs)
    }
}