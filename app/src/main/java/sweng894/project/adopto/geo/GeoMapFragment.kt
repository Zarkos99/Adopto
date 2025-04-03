package sweng894.project.adopto.geo

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
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import sweng894.project.adopto.R
import sweng894.project.adopto.custom.PlacesAutocompleteHelper
import sweng894.project.adopto.custom.PlacesAutocompleteHelper.handleActivityResult
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.fetchAllShelters
import sweng894.project.adopto.database.haversineDistance
import sweng894.project.adopto.databinding.GeoMapFragmentBinding
import sweng894.project.adopto.profile.UserMiniProfileFragmentOverlay
import kotlin.math.log2

class GeoMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: GeoMapFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val M_DEFAULT_INITIAL_LAT = 0.0
    private val M_DEFAULT_INITIAL_LONG = 0.0
    private val M_DEFAULT_SEARCH_RADIUS_MILES = 25.0


    private lateinit var m_firebase_data_service: FirebaseDataServiceUsers
    private lateinit var m_map: GoogleMap
    private var m_is_firebase_service_bound: Boolean = false
    private var m_map_ready: Boolean = false
    private var m_location: GeoPoint = GeoPoint(M_DEFAULT_INITIAL_LAT, M_DEFAULT_INITIAL_LONG)
    private var m_prev_user_data: User? = null

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as FirebaseDataServiceUsers.LocalBinder
            m_firebase_data_service = binder.getService()
            m_is_firebase_service_bound = true

            if (m_map_ready) {
                instantiateDefaultMapLocation()
                applyMarkersToMap()
                setupMapMarkerListeners()
            }

            // Listen to user data updates
            m_firebase_data_service.registerCallback {
                if (m_prev_user_data?.explore_preferences?.search_radius_miles != m_firebase_data_service.current_user_data?.explore_preferences?.search_radius_miles) {
                    // Handle search radius update
                    moveCameraAndZoomForSearchRadius()
                }

                m_prev_user_data = m_firebase_data_service.current_user_data
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            m_is_firebase_service_bound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GeoMapFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.getRoot()


        // Bind to LocalService.
        Intent(requireContext(), FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        val search_button = binding.btnLocationSearch
        search_button.tooltipText = "Search for a city or ZIP code"
        search_button.setOnClickListener {
            PlacesAutocompleteHelper.launchFromFragment(this) { place ->
                val lat_lng = place.latLng
                Log.d("GeoMapFragment", "User picked: ${place.name} at $lat_lng")
                if (lat_lng != null) {
                    m_location = GeoPoint(lat_lng.latitude, lat_lng.longitude)
                    moveCameraAndZoomForSearchRadius()
                    applyMarkersToMap()
                }
            }
        }

        val map_fragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        map_fragment.getMapAsync(this)

        return root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        m_map = googleMap
        m_map_ready = true

        if (m_is_firebase_service_bound) {
            instantiateDefaultMapLocation()
            applyMarkersToMap()
            setupMapMarkerListeners()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unbindService(connection)
        m_is_firebase_service_bound = false
        m_map_ready = false
    }


    fun instantiateDefaultMapLocation() {
        if (m_is_firebase_service_bound && m_map_ready) {
            m_location =
                m_firebase_data_service.current_user_data?.location ?: m_location
            moveCameraAndZoomForSearchRadius()
        }
    }

    fun moveCameraAndZoomForSearchRadius() {
        if (m_is_firebase_service_bound && m_map_ready) {
            val user_data = m_firebase_data_service.current_user_data
            val zoom = radiusToZoom(
                user_data?.explore_preferences?.search_radius_miles
                    ?: M_DEFAULT_SEARCH_RADIUS_MILES
            )
            val latLng = LatLng(m_location.latitude, m_location.longitude)

            Log.d("GeoMapFragment", "Moving camera to $latLng with zoom $zoom")

            m_map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        } else {
            Log.w(
                "GeoMapFragment",
                "Camera move skipped – mapReady=$m_map_ready, serviceBound=$m_is_firebase_service_bound"
            )
        }
    }


    fun radiusToZoom(radius_in_miles: Double): Float {
        // Approximate: Earth's circumference ≈ 24,901 miles
        // 256 pixels at zoom level 0 equals full world map (≈ 156543 meters/pixel at equator)
        val scale = radius_in_miles / 1.0 // 1 mile per unit
        return (16 - log2(scale)).toFloat()
    }

    private fun applyMarkersToMap() {
        if (m_is_firebase_service_bound && m_map_ready) {
            getNearbySheltersForMap { shelters ->
                // Clear existing overlays and markers
                m_map.clear()

                for (shelter in shelters) {
                    val latLng = LatLng(shelter.location!!.latitude, shelter.location!!.longitude)
                    val marker = m_map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(shelter.display_name.ifEmpty { "Unnamed Shelter" })
                    )
                    marker?.tag = shelter.user_id // Store ID for click callback
                }
            }
        }
    }

    fun getNearbySheltersForMap(
        onComplete: (List<User>) -> Unit
    ) {
        val user = m_firebase_data_service.current_user_data
        val search_radius = user?.explore_preferences?.search_radius_miles ?: 0.0
        val effective_location = m_location

        fetchAllShelters { all_shelters ->
            val nearby_shelters = all_shelters.filter { shelter ->
                shelter.location != null &&
                        haversineDistance(
                            effective_location,
                            shelter.location!!
                        ) <= search_radius
            }

            Log.d("MapSearch", "Found ${nearby_shelters.size} shelters within $search_radius miles")
            onComplete(nearby_shelters)
        }
    }

    fun setupMapMarkerListeners() {
        m_map.setOnMarkerClickListener { marker ->
            val shelter_id = marker.tag as? String
            if (shelter_id != null) {
                // Launch a bottom sheet dialog or mini profile fragment
                showShelterMiniProfile(shelter_id)
            }
            true
        }
    }

    private fun showShelterMiniProfile(shelterId: String) {
        // You can launch a dialog or bottom sheet with the shelterId
        val dialog = UserMiniProfileFragmentOverlay.newInstance(shelterId)
        dialog.show(childFragmentManager, "shelter_mini_profile")
    }
}