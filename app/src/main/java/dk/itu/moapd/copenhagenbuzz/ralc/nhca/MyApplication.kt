package dk.itu.moapd.copenhagenbuzz.ralc.nhca

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.github.cdimascio.dotenv.dotenv

/**
 * Firebase Realtime Database URL.
 *
 * IMPORTANT: This is a sensitive information and should not be hardcoded in the source code. You
 * must create a `env` file in the `/app/src/main/assets` directory and add the following line:
 * DATABASE_URL=https://xxxxxxxxxx-default-rtdb.europe-west1.firebasedatabase.app
 */
val DATABASE_URL: String = dotenv {
    directory = "./assets"
    filename = "env"
}["DATABASE_URL"]

class MyApplication: Application() {



    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colors to activities if available.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Enable disk persistence for the Firebase Realtime Database and keep it synchronized.
        Firebase.database(DATABASE_URL).setPersistenceEnabled(true)
        Firebase.database(DATABASE_URL).reference.keepSynced(true)
    }
}