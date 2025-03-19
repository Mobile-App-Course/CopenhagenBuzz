/*
    MIT License

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
     */

package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Intent
import androidx.navigation.fragment.NavHostFragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ContentMainBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.github.cdimascio.dotenv.dotenv

/**
 * The MainActivity class represents the main screen of the application, and also allows the user to input event details and add them to the event list.
 */
class MainActivity : AppCompatActivity() {

    // Binding objects for the activity and content layouts
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var contentMainBinding: ContentMainBinding

    private lateinit var menuProfile: MenuItem
    private lateinit var menuLogout: MenuItem

    private lateinit var database: DatabaseReference

    /**
     * Called when the activity is first created. This inflates the different bindings with the necessary variables.
     * It also sets up the listeners for the different UI components.
     * @param savedInstanceState Saves the latest state of the activity, if it has been shut down.
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Load environment variables
        val dotenv = dotenv()

        // Apply dynamic colors if running on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        // Enable offline persistence for Firebase
        Firebase.database(dotenv["DATABASE_URL"]).setPersistenceEnabled(true)

        database = Firebase.database(dotenv["DATABASE_URL"]).reference
        database.keepSynced(true)

        // Check if the user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // If not logged in, start LoginActivity for anonymous login
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("isAnonymousLogin", true)
            startActivity(intent)
            finish()
            return
        } else {
            // Show Snackbar with authentication type
            showAuthTypeSnackbar(currentUser)
        }

        // Inflate the layout for this activity
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        // Set the content view of the activity
        setContentView(activityMainBinding.root)

        // Bind the content layout
        contentMainBinding = ContentMainBinding.bind(activityMainBinding.root.findViewById(R.id.content_main))

        // Set the toolbar as the action bar
        setSupportActionBar(contentMainBinding.toolbar)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController

        activityMainBinding.bottomNavigation.setupWithNavController(navController)

        // Conditionally show the add_event_fragment item
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
        val bottomNavigationMenu = activityMainBinding.bottomNavigation.menu
        val addEventMenuItem = bottomNavigationMenu.findItem(R.id.add_event_fragment)
        addEventMenuItem.isVisible = isLoggedIn

        // Conditionally show the favorites_fragment item
        val favoritesMenuItem = bottomNavigationMenu.findItem(R.id.favorites_fragment)
        favoritesMenuItem.isVisible = isLoggedIn

        // Conditionally show event row item buttons if logged in
        // Get this element @+id/edit_button
    }

    /**
     * Inflate the menu items for use in the action bar.
     * @param menu The options menu in which you place your items.
     * @return Boolean Return true for the menu to be displayed; if false, it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        if (menu != null) {
            menuProfile = menu.findItem(R.id.menu_profile)
            menuLogout = menu.findItem(R.id.menu_logout)
            //addEventButton = menu.findItem(R.id.add_event_fragment)
        }

        setMenuListeners()
        return true
    }

    /**
     * Prepare the options menu before it is displayed.
     * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
     * @return Boolean Return true for the menu to be displayed; if false, it will not be shown.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
        menuProfile.isVisible = isLoggedIn
        menuLogout.isVisible = !isLoggedIn
        //addEventButton.isVisible = isLoggedIn
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Set listeners to display the correct menu items (icons) based on the user's login status.
     */
    private fun setMenuListeners() {
        menuProfile.setOnMenuItemClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("isLoggedIn", true)
            startActivity(intent)
            finish()
            true
        }

        menuLogout.setOnMenuItemClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("isLoggedIn", false)
            startActivity(intent)
            finish()
            true
        }
    }

    private fun showAuthTypeSnackbar(user: FirebaseUser) {
        val authType = when {
            user.isAnonymous -> "Anonymous"
            user.providerData.any { it.providerId == "google.com" } -> "Google"
            user.providerData.any { it.providerId == "password" } -> "Email"
            else -> "Unknown"
        }
        Snackbar.make(findViewById(android.R.id.content), "Logged in as $authType", Snackbar.LENGTH_LONG).show()
    }
}