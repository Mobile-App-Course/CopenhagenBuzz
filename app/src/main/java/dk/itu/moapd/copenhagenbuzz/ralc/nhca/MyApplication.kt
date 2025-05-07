package dk.itu.moapd.copenhagenbuzz.ralc.nhca

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.github.cdimascio.dotenv.dotenv

/**
 * Firebase Realtime Database URL.
 *
 * This class is used to set up application-wide configurations such as enabling Firebase Realtime
 * Database disk persistence and applying dynamic colors to activities.
 */
val DATABASE_URL: String = dotenv {
    directory = "./assets"
    filename = "env"
}["DATABASE_URL"]

class MyApplication: Application() {


    /**
     * Called when the application is starting, before any other application objects have been created.
     * This is where you should initialize global configurations.
     */
    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colors to activities if available.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Enable disk persistence for the Firebase Realtime Database and keep it synchronized.
        Firebase.database(DATABASE_URL).setPersistenceEnabled(true)
        Firebase.database(DATABASE_URL).reference.keepSynced(true)
    }
}