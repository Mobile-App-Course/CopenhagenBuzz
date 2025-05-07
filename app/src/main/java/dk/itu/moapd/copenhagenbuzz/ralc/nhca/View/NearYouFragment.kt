package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.EventLocation
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel
import io.github.cdimascio.dotenv.dotenv

/**
 * A fragment that displays events sorted by distance from the user's location.
 * Use the [NearYouFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NearYouFragment : Fragment() {
    private lateinit var eventAdapter: EventAdapter
    private val dataViewModel: DataViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseRef: DatabaseReference

    // Save reference to the ValueEventListener to remove it later
    private var valueEventListener: ValueEventListener? = null

    // User's current location coordinates (null by default)
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var locationPermissionGranted = false

    // Store events with calculated distances
    private val eventsWithDistance = mutableListOf<Pair<String, Event>>()

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        locationPermissionGranted = allGranted

        if (allGranted) {
            // Permissions granted, get location
            getLastLocation()
        } else {
            // Permissions denied, load events without location
            Toast.makeText(
                requireContext(),
                "Location permission denied. Distances will be shown as N/A.",
                Toast.LENGTH_LONG
            ).show()
            // Load events without user location
            loadEvents()
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_nearyou, container, false)
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Load environment variables
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }

        // Initialize Firebase reference once
        databaseRef = Firebase.database(dotenv["DATABASE_URL"]).getReference("Events")

        // Request location permissions
        requestLocationPermission()
    }

    /**
     * Called when the fragment is no longer in use. This is called
     * after onStop() and before onDetach().
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the Firebase event listener when the fragment view is destroyed
        removeEventListener()
    }

    /**
     * Requests location permissions from the user.
     * If permissions are already granted, retrieves the user's last known location.
     */
    private fun requestLocationPermission() {
        when {
            // Check if permissions are already granted
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                // Permissions already granted
                locationPermissionGranted = true
                getLastLocation()
            }
            // Should show rationale
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    requireContext(),
                    "Location permission is needed to calculate distance to events",
                    Toast.LENGTH_LONG
                ).show()
                // Request permissions
                requestPermissions()
            }
            else -> {
                // Request permissions directly
                requestPermissions()
            }
        }
    }

    /**
     * Launch permission request
     */
    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Retrieves the user's last known location.
     * If location permissions are granted, updates the user's location and loads events.
     * If permissions are not granted or an error occurs, loads events without location.
     */
    private fun getLastLocation() {
        try {
            if (locationPermissionGranted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        // Update user location
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        Log.d("NearYouFragment", "User location: $userLatitude, $userLongitude")
                    } else {
                        Log.d("NearYouFragment", "Location is null, will show N/A for distances")
                        // Keep userLatitude and userLongitude as null
                    }
                    // Load events with user location (or null)
                    loadEvents()
                }.addOnFailureListener { e ->
                    Log.e("NearYouFragment", "Error getting location: ${e.message}")
                    // Load events without location
                    loadEvents()
                }
            } else {
                // Permissions not granted, load events without location
                loadEvents()
            }
        } catch (e: SecurityException) {
            Log.e("NearYouFragment", "Security exception: ${e.message}")
            // Load events without location
            loadEvents()
        }
    }

    /**
     * Remove event listener from Firebase
     */
    private fun removeEventListener() {
        valueEventListener?.let {
            databaseRef.removeEventListener(it)
            valueEventListener = null
            Log.d("NearYouFragment", "ValueEventListener removed")
        }
    }

    /**
     * Loads events from Firebase, calculates their distances from the user's location if available,
     * otherwise marks distances as unavailable.
     */
    private fun loadEvents() {
        // First, check if fragment is still attached to avoid crashes
        if (!isAdded) {
            Log.d("NearYouFragment", "Fragment not attached, skipping loadEvents")
            return
        }

        val listView: ListView = view?.findViewById(R.id.event_list_view) ?: run {
            Log.e("NearYouFragment", "ListView not found")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val isLoggedIn = currentUser != null && !currentUser.isAnonymous

        // Remove any existing listener before adding a new one
        removeEventListener()

        // Create a new listener and store the reference
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if fragment is still attached
                if (!isAdded) {
                    Log.d("NearYouFragment", "Fragment not attached during callback, ignoring data")
                    return
                }

                if (snapshot.exists()) {
                    Log.d("NearYouFragment", "Data retrieved: ${snapshot.childrenCount} events")

                    // Clear previous list
                    eventsWithDistance.clear()

                    // Populate the list and calculate distances if location is available
                    for (eventSnapshot in snapshot.children) {
                        val event = eventSnapshot.getValue(Event::class.java)
                        val key = eventSnapshot.key
                        if (event != null && key != null) {
                            // Only calculate distance if user location is available
                            if (userLatitude != null && userLongitude != null) {
                                // Calculate distance from user to event
                                val distance = calculateDistance(
                                    userLatitude!!, userLongitude!!,
                                    event.eventLocation.latitude, event.eventLocation.longitude
                                )
                                // Store distance in the event object
                                event.eventLocation.distance = distance
                                Log.d("NearYouFragment", "Event: ${event.eventName}, Distance: ${distance/1000}km")
                            } else {
                                // Set distance to -1 to indicate "N/A"
                                event.eventLocation.distance = -1f
                                Log.d("NearYouFragment", "Event: ${event.eventName}, Distance: N/A")
                            }

                            eventsWithDistance.add(Pair(key, event))
                        }
                    }

                    // Sort events by distance if location is available, otherwise by name
                    if (userLatitude != null && userLongitude != null) {
                        eventsWithDistance.sortBy { (_, event) -> event.eventLocation.distance }
                    } else {
                        // Sort by event name if location not available
                        eventsWithDistance.sortBy { (_, event) -> event.eventName }
                    }

                    // Create and set adapter with sorted events, using the nearby_event_row_item layout
                    eventAdapter = EventAdapter.createWithSortedEvents(
                        eventsWithDistance,
                        requireContext(),
                        isLoggedIn,
                        R.layout.nearby_event_row_item
                    )

                    // Set a flag to indicate whether location is available
                    eventAdapter.setLocationAvailable(userLatitude != null && userLongitude != null)

                    listView.adapter = eventAdapter

                    // Observe favorite events
                    dataViewModel.favoriteEvents.observe(viewLifecycleOwner, Observer { favoriteEvents ->
                        if (isAdded) {
                            (listView.adapter as? EventAdapter)?.setFavoriteEvents(favoriteEvents)
                        }
                    })
                } else {
                    Log.d("NearYouFragment", "No data found")
                    // Create empty adapter
                    eventAdapter = EventAdapter.create(databaseRef, requireContext(), isLoggedIn)
                    eventAdapter.setLocationAvailable(userLatitude != null && userLongitude != null)
                    listView.adapter = eventAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NearYouFragment", "Database error: ${error.message}")
            }
        }

        databaseRef.addValueEventListener(valueEventListener!!)
    }

    /**
     * Calculates the distance between two geographic coordinates using Android's Location API.
     *
     * @param lat1 Latitude of the first location.
     * @param lon1 Longitude of the first location.
     * @param lat2 Latitude of the second location.
     * @param lon2 Longitude of the second location.
     * @return The distance in meters between the two locations.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    companion object {
        /**
         * Factory method to create a new instance of this fragment.
         * @return A new instance of fragment NearYouFragment.
         */
        @JvmStatic
        fun newInstance() = NearYouFragment()
    }
}