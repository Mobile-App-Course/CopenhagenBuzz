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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
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
 * A simple [Fragment] subclass.
 * Use the [MapsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapsFragment : Fragment(), LocationBroadcastReceiver.LocationListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var rootView: View
    private var locationReceiver: LocationBroadcastReceiver? = null
    private var currentLocation: Location? = null
    private var googleMap: GoogleMap? = null
    private val eventMarkers = mutableMapOf<Marker, Event>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializing location receiver
        locationReceiver = LocationBroadcastReceiver(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_maps, container, false)
        return rootView
    }

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

    override fun onResume() {
        super.onResume()

        // Registers broadcast receiver when the fragment is visible
        context?.registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_LOCATION_BROADCAST),
            android.content.Context.RECEIVER_NOT_EXPORTED
        )
    }

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

    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()
    }

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

    private fun startLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().startForegroundService(serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().stopService(serviceIntent)
    }

    /**
     * This method checks if the user allows the application uses all location-aware resources to
     * monitor the user's location.
     *
     * @return A boolean value with the user permission agreement.
     */
    private fun checkPermission() =
        ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Create a set of dialogs to show to the users and ask them for permissions to get the device's
     * resources.
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

    private fun initializeMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

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
            }
        }

        currentLocation?.let { updateMapWithLocation(it) }

        loadEventsFromFirebase()
    }

    private fun updateMapWithLocation(location: Location) {
        googleMap?.let {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
        }
    }

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

                // Clears existing event markers
                eventMarkers.keys.forEach { it.remove() }
                eventMarkers.clear()

                // Adds new event markers for each event in the database
                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.let {addEventMarker(it)}
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

    override fun onMarkerClick(marker: Marker): Boolean {
        val event = eventMarkers[marker] ?: return false

        showEventDetails(event)

        return true
    }

    private fun showEventDetails(event: Event) {
        val fragment = EventDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("event", event)
            }
        }

        fragment.show(parentFragmentManager, "EventDetailsFragment")
    }

    companion object {
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

        /**
         * Factory method to create a new instance of this fragment.
         */
        @JvmStatic
        fun newInstance() = MapsFragment()
    }

}