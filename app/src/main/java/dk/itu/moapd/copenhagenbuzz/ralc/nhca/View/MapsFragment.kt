package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.Manifest
import android.content.SharedPreferences

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.LocationBroadcastReceiver
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.LocationService
import io.github.cdimascio.dotenv.dotenv
import java.text.SimpleDateFormat
import java.util.*


/**
 * A fragment that displays a map and handles location-based functionality.
 *
 * This fragment integrates with Google Maps to display a map, show the user's current location,
 * and load event markers from Firebase. It also listens for location updates and handles user
 * interactions with map markers.
 */
class MapsFragment : Fragment(), LocationBroadcastReceiver.LocationListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var rootView: View
    private var locationReceiver: LocationBroadcastReceiver? = null
    private var currentLocation: Location? = null
    private var googleMap: GoogleMap? = null
    private var doneInitialZoom = false
    private val eventMarkers = mutableMapOf<Marker, Event>()
    private val eventKeys = mutableMapOf<Event, String>()


    /**
     * Called when the fragment is created.
     * Initializes the location receiver.
     *
     * @param savedInstanceState The saved state of the fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializing location receiver
        locationReceiver = LocationBroadcastReceiver(this)
    }

    /**
     * Called to create the view hierarchy of the fragment.
     *
     * @param inflater The LayoutInflater used to inflate the layout.
     * @param container The parent view that the fragment's UI will be attached to.
     * @param savedInstanceState The saved state of the fragment.
     * @return The root view of the fragment's layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_maps, container, false)
        return rootView
    }

    /**
     * Called after the view hierarchy has been created.
     * Checks for location permissions and initializes the map if permissions are granted.
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState The saved state of the fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Checks for location permission when fragment is visible
        if (!checkPermission()) {
            requestUserPermissions()
        } else {
          // Permission is granted, initialize the map
            initializeMap()
            startLocationService()
        }
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Registers the location broadcast receiver.
     */
    override fun onResume() {
        super.onResume()

        // Registers broadcast receiver when the fragment is visible
        context?.registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_LOCATION_BROADCAST),
            android.content.Context.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Called when the fragment is no longer visible to the user.
     * Unregisters the location broadcast receiver.
     */
    override fun onPause() {
        super.onPause()

        // Unregisters broadcast receiver when the fragment is no longer visible
        try {
            context?.unregisterReceiver(locationReceiver)
        } catch (e: Exception) {
            // receiver not registered
            e.printStackTrace()
        }
    }

    /**
     * Called when the fragment is destroyed.
     * Stops the location service.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()
    }

    /**
     * Handles the result of a permission request.
     *
     * @param requestCode The request code passed in the permission request.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the requested permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                Snackbar.make(rootView, "Location permission granted", Snackbar.LENGTH_SHORT).show()
                initializeMap()
                startLocationService()
            } else {
                // Permission is denied
                Snackbar.make(rootView, "Location permission denied, it is required to show your location on the map", Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Called when a new location is received.
     * Updates the current location and the map.
     *
     * @param location The new location.
     */
    override fun onLocationReceived(location: Location) {
        currentLocation = location
        // Update UI with new location
        Snackbar.make(
            rootView,
            "Location: ${location.latitude}, ${location.longitude}",
            Snackbar.LENGTH_SHORT
        ).show()

        // Update map with new location when implemented
        updateMapWithLocation(location)
    }

    /**
     * Starts the location service to receive location updates.
     */
    private fun startLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().startForegroundService(serviceIntent)
    }

    /**
     * Stops the location service.
     */
    private fun stopLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().stopService(serviceIntent)
    }

    /**
     * Checks if the app has the required location permission.
     *
     * @return True if the permission is granted, false otherwise.
     */
    private fun checkPermission() =
        ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Requests the user to grant location permissions.
     * Displays a rationale if necessary.
     */
    private fun requestUserPermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Ask the user
            Snackbar.make(rootView, "Location permission needed to show location", Snackbar.LENGTH_LONG).setAction("OK") {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }.show()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    /**
     * Initializes the Google Map by setting up the map fragment and requesting the map asynchronously.
     */
    private fun initializeMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    /**
     * Called when the Google Map is ready to be used.
     * Configures the map and loads event markers from Firebase.
     *
     * @param map The Google Map instance.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Set up the map
        with(googleMap!!) {
            setOnMarkerClickListener(this@MapsFragment)
            uiSettings.isZoomControlsEnabled = true

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isMyLocationEnabled = true
                uiSettings.isMyLocationButtonEnabled = true

                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        zoomToLocation(it)
                        doneInitialZoom = true
                    }
                }
            }
        }

        currentLocation?.let {
            if(!doneInitialZoom){
                zoomToLocation(it)
                doneInitialZoom = true
            }
        }

        loadEventsFromFirebase()
    }

    /**
     * Zooms the map to the specified location.
     *
     * @param location The location to zoom to.
     */
    private fun zoomToLocation(location: Location){
        googleMap?.let {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
        }
    }

    /**
     * Updates the map with the specified location.
     * Performs an initial zoom if it hasn't been done yet.
     *
     * @param location The location to update the map with.
     */
    private fun updateMapWithLocation(location: Location) {
        // Update the camera position if initial zoom hasn't happened yet
        if (!doneInitialZoom) {
            zoomToLocation(location)
            doneInitialZoom = true
        }
    }

    /**
     * Loads event markers from Firebase and adds them to the map.
     */
    private fun loadEventsFromFirebase() {

        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }

        // Get database reference
        val database = Firebase.database(dotenv["DATABASE_URL"])
        val eventsRef = database.getReference("Events")

        eventsRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear existing event markers and keys
                eventMarkers.keys.forEach { it.remove() }
                eventMarkers.clear()
                eventKeys.clear()

                // Add new event markers for each event in the database
                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    val key = eventSnapshot.key
                    if (event != null && key != null) {
                        addEventMarker(event)
                        eventKeys[event] = key
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(
                    rootView,
                    "Failed to load events: ${error.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

    }

    /**
     * Adds a marker for the specified event to the map.
     *
     * @param event The event to add a marker for.
     */
    private fun addEventMarker(event: Event) {
        val eventLocation = event.eventLocation
        if (eventLocation.latitude != 0.0 || eventLocation.longitude != 0.0) {
            val position = LatLng(eventLocation.latitude, eventLocation.longitude)
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(event.eventName)
                    .snippet(event.eventType)
            )

            marker?.let {eventMarkers[it] = event}
        }
    }

    /**
     * Called when a marker on the map is clicked.
     * Displays the details of the associated event.
     *
     * @param marker The marker that was clicked.
     * @return True if the click was handled, false otherwise.
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        val event = eventMarkers[marker] ?: return false

        showEventDetails(event)

        return true
    }

    /**
     * Displays the details of the specified event in a dialog.
     *
     * @param event The event to display details for.
     */
    private fun showEventDetails(event: Event) {
        // Checks if the user is logged in or not
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isLoggedIn = currentUser != null && !currentUser.isAnonymous

        // Get the event key from our map
        val eventKey = eventKeys[event] ?: ""

        val fragment = EventDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("event", event)
                putBoolean("isLoggedIn", isLoggedIn)
                putString("eventKey", eventKey) // Add the event key
            }
        }

        fragment.show(parentFragmentManager, "EventDetailsFragment")
    }


    companion object {
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

        /**
         * Factory method to create a new instance of this fragment.
         *
         * @return A new instance of MapsFragment.
         */
        @JvmStatic
        fun newInstance() = MapsFragment()
    }

}