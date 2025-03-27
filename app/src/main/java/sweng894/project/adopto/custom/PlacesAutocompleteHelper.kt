package sweng894.project.adopto.custom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

object PlacesAutocompleteHelper {

    private const val TAG = "PlacesAutocompleteHelper"

    const val AUTOCOMPLETE_REQUEST_CODE = 1001
    private var locationCallback: ((Place) -> Unit)? = null

    fun launchFromActivity(activity: Activity, callback: (Place) -> Unit) {
        locationCallback = callback
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
        ).build(activity)
        activity.startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    fun launchFromFragment(fragment: Fragment, callback: (Place) -> Unit) {
        locationCallback = callback
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        ).build(fragment.requireContext())
        fragment.startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != AUTOCOMPLETE_REQUEST_CODE) return

        when (resultCode) {
            Activity.RESULT_OK -> {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                locationCallback?.invoke(place)
            }

            AutocompleteActivity.RESULT_ERROR -> {
                val status = Autocomplete.getStatusFromIntent(data!!)
                Log.e("PlacesAutocomplete", "Error: ${status.statusMessage}")
            }

            Activity.RESULT_CANCELED -> {
                Log.d("PlacesAutocomplete", "User canceled autocomplete")
            }
        }
        // Reset callback after use
        locationCallback = null
    }

    fun latLngToFormattedAddress(
        context: Context,
        latLng: com.google.android.gms.maps.model.LatLng
    ): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!results.isNullOrEmpty()) {
                val address = results[0]
                val street =
                    listOfNotNull(address.subThoroughfare, address.thoroughfare).joinToString(" ")
                val city = address.locality
                val state = address.adminArea
                val country = address.countryCode

                val formattedAddress =
                    listOfNotNull(street, city, state, country).joinToString(", ")

                Log.d("GeoUtils", "Geocoder result: $formattedAddress")
                Log.d("GeoUtils", "Raw address object: $address")

                formattedAddress
            } else {
                Log.w("GeoUtils", "Geocoder returned no results for: $latLng")
                null
            }
        } catch (e: Exception) {
            Log.e("GeoUtils", "Geocoder failed for: $latLng - ${e.message}", e)
            null
        }
    }


    fun geoPointToFormattedAddress(context: Context, geoPoint: GeoPoint): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val results = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
            if (!results.isNullOrEmpty()) {
                val address = results[0]
                val street =
                    listOfNotNull(address.subThoroughfare, address.thoroughfare).joinToString(" ")
                val city = address.locality
                val state = address.adminArea
                val country = address.countryCode
                listOfNotNull(street, city, state, country).joinToString(", ")
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
