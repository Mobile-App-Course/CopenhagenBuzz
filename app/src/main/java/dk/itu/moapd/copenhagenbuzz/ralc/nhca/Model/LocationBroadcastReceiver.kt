package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.content.BroadcastReceiver
import android.location.Location
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * A `BroadcastReceiver` that listens for location updates broadcasted by the `LocationService`.
 *
 * This class receives location updates and passes them to a `LocationListener` for further processing.
 *
 * @property locationListener The listener that handles received location updates.
 */
class LocationBroadcastReceiver(private val locationListener: LocationListener) : BroadcastReceiver(){

    /**
     * Interface for handling received location updates.
     */
    interface LocationListener{
        /**
         * Called when a location is received.
         *
         * @param location The received `Location` object.
         */
        fun onLocationReceived(location: Location)
    }

    /**
     * Called when the `BroadcastReceiver` receives an intent.
     *
     * This method checks if the received intent contains a location update from the `LocationService`.
     * If a location is found, it is passed to the `LocationListener`.
     *
     * @param context The `Context` in which the receiver is running.
     * @param intent The `Intent` received by the `BroadcastReceiver`.
     */
    override fun onReceive(context: Context, intent: Intent){
        if (intent.action == LocationService.ACTION_LOCATION_BROADCAST) {
            val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(LocationService.EXTRA_LOCATION, Location::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(LocationService.EXTRA_LOCATION)

            }

            location?.let {
                locationListener.onLocationReceived(it)
            }
        }
    }
}
