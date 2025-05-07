package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.app.*
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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

/**
 * A foreground service that provides location updates.
 *
 * This service uses the `FusedLocationProviderClient` to request location updates and broadcasts
 * the location data to other components. It runs as a foreground service to ensure it continues
 * running even when the app is in the background.
 */
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

    /**
     * Called when the service is created.
     *
     * Initializes the `FusedLocationProviderClient` and sets up the location callback.
     * Also creates the notification channel for the foreground service.
     */
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

    /**
     * Called when the service is started.
     *
     * Starts the service in the foreground and begins requesting location updates.
     *
     * @param intent The intent that started the service.
     * @param flags Additional data about the start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The start mode for the service, indicating it should be restarted if killed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            startLocationUpdates()
            isServiceRunning = true
        }



        // If service is killed by the system, it will be restarted
        return START_STICKY
    }

    /**
     * Called when a client binds to the service.
     *
     * This service does not support binding, so it always returns `null`.
     *
     * @param intent The intent that was used to bind to the service.
     * @return Always returns `null` as binding is not supported.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Called when the service is destroyed.
     *
     * Stops location updates and performs cleanup.
     */
    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    /**
     * Starts requesting location updates.
     *
     * Configures the location request parameters and checks for the necessary permissions.
     * If permissions are not granted, the service stops itself.
     */
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

    /**
     * Stops location updates.
     *
     * This method removes the location updates from the `FusedLocationProviderClient`
     * and sets the `isServiceRunning` flag to `false`.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isServiceRunning = false
    }

    /**
     * Broadcasts the provided location.
     *
     * This method creates an intent with the action `ACTION_LOCATION_BROADCAST` and
     * includes the location as an extra. The intent is then broadcasted to other components.
     *
     * @param location The `Location` object to broadcast.
     */
    private fun broadcastLocation(location: Location) {
        val intent = Intent(ACTION_LOCATION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        sendBroadcast(intent)
    }

    /**
     * Creates a notification channel for the service.
     *
     * This method is required for Android O and above. It creates a notification channel
     * with the ID `NOTIFICATION_CHANNEL_ID` and registers it with the system.
     */
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

    /**
     * Creates a notification for the foreground service.
     *
     * This method builds a notification with a title, text, and a small icon. It also
     * includes a pending intent that opens the `MainActivity` when the notification is clicked.
     *
     * @return The created `Notification` object.
     */
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