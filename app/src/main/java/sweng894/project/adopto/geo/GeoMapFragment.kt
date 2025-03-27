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
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import sweng894.project.adopto.R
import sweng894.project.adopto.data.Animal
import sweng894.project.adopto.data.LocationUtilities
import sweng894.project.adopto.data.User
import sweng894.project.adopto.database.FirebaseDataServiceUsers
import sweng894.project.adopto.database.fetchAllAnimals
import sweng894.project.adopto.database.haversineDistance
import sweng894.project.adopto.databinding.GeoMapFragmentBinding
import sweng894.project.adopto.profile.animalprofile.AnimalProfileViewingActivity
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
        val root: View = binding.root

        val search_view = binding.geoSearchField

        // Bind to LocalService.
        Intent(requireContext(), FirebaseDataServiceUsers::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        instantiateSearchView()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val trimmed_query = query?.trim() ?: return false
                val new_geo_point = LocationUtilities.zipToGeoPoint(requireContext(), trimmed_query)

                if (new_geo_point != null) {
                    m_location = new_geo_point
                    moveCameraAndZoomForSearchRadius()
                    applyMarkersToMap()
                } else {
                    Toast.makeText(requireContext(), "Location not found!", Toast.LENGTH_SHORT)
                        .show()
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

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

    fun instantiateSearchView() {
        val search_view = binding.geoSearchField
        search_view.post {
            val search_edit_text =
                search_view.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            if (search_edit_text != null) {
                search_edit_text.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_text
                    )
                )
                search_edit_text.setHintTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_text
                    )
                )
            } else {
                Log.w("GeoMapFragment", "SearchView's EditText not found.")
            }
        }
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
            m_map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(m_location.latitude, m_location.longitude),
                    radiusToZoom(
                        user_data?.explore_preferences?.search_radius_miles
                            ?: M_DEFAULT_SEARCH_RADIUS_MILES
                    )
                )
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

            getNearbyAnimalsForMap { animals ->
                // Clear existing overlays and markers
                m_map.clear()

                for (animal in animals) {
                    val latLng = LatLng(animal.location!!.latitude, animal.location!!.longitude)
                    val marker = m_map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(animal.animal_name ?: "Unnamed Animal")
                    )
                    marker?.tag = animal.animal_id // Store ID for click callback
                }
            }
        }
    }

    fun getNearbyAnimalsForMap(
        onComplete: (List<Animal>) -> Unit
    ) {
        val user = m_firebase_data_service.current_user_data
        val search_radius = user?.explore_preferences?.search_radius_miles ?: 0.0
        val effective_location = m_location

        fetchAllAnimals { all_animals ->
            val nearby_animals = all_animals.filter { animal ->
                animal.location != null &&
                        haversineDistance(effective_location, animal.location!!) <= search_radius
            }

            Log.d("MapSearch", "Found ${nearby_animals.size} animals within $search_radius km")
            onComplete(nearby_animals)
        }
    }

    fun setupMapMarkerListeners() {
        m_map.setOnMarkerClickListener { marker ->
            val animalId = marker.tag as? String
            if (animalId != null) {
                val intent = Intent(requireContext(), AnimalProfileViewingActivity::class.java)
                intent.putExtra("animal_id", animalId)
                startActivity(intent)
            }
            true
        }

    }
}