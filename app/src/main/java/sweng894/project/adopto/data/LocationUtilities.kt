package sweng894.project.adopto.data

import android.content.Context
import android.util.Log
import android.location.Geocoder
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

object LocationUtilities {

    private const val TAG = "LocationUtilities"

    fun zipToGeoPoint(context: Context, zip_code: String): GeoPoint? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results = geocoder.getFromLocationName(zip_code, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                GeoPoint(loc.latitude, loc.longitude)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
