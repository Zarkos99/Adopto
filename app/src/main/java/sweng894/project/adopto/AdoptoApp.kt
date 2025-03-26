package sweng894.project.adopto

import android.app.Application
import androidx.annotation.StringRes
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
        VectorUtils.initializeTypeEncoding(this)
        _instance = this
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