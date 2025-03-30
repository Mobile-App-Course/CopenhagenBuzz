package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.content.BroadcastReceiver
import android.location.Location
import android.content.Context
import android.content.Intent
import android.os.Build

class LocationBroadcastReceiver(private val locationListener: LocationListener) : BroadcastReceiver(){

    interface LocationListener{
        fun onLocationReceived(location: Location)
    }

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
