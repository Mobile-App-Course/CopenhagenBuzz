package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.app.*
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.View.MainActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority


class LocationService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val NOTIFICATION_CHANNEL_ID = "location_service"
        const val ACTION_LOCATION_BROADCAST = "dk.itu.moapd.copenhagenbuzz.ralc.nhca.LOCATION_BROADCAST"
        const val EXTRA_LOCATION = "extra_location"

        // Request parameters
        private const val UPDATE_INTERVAL = 10000L // 10 seconds
        private const val FASTEST_INTERVAL = 5000L // 5 seconds

    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    broadcastLocation(location)
                }
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            startForeground(NOTIFICATION_ID, createNotification())
            startLocationUpdates()
            isServiceRunning = true
        }

        // If service is killed by the system, it will be restarted
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        // New way to create a LocationRequest using the Builder
        val locationRequest = LocationRequest.Builder(UPDATE_INTERVAL)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .build()

        // Check permissions to see if it is allowed to access location
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        {
            // Permission not granted, stop the service
            stopSelf()
            return
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isServiceRunning = false
    }

    private fun broadcastLocation(location: Location) {
        val intent = Intent(ACTION_LOCATION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CopenhagenBuzz Location Service")
            .setContentText("Tracking your location")
            .setSmallIcon(R.drawable.baseline_firebase)
            .setContentIntent(pendingIntent)
            .build()
    }
}