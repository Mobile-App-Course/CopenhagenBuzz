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
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.LocationBroadcastReceiver
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.LocationService

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapsFragment : Fragment(), LocationBroadcastReceiver.LocationListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var rootView: View
    private var locationReceiver: LocationBroadcastReceiver? = null
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

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
        // Code will be implemented later here, to actually show the map
    }

    private fun updateMapWithLocation(location: Location) {
        // Code will be implemented later to update the map with the current location
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