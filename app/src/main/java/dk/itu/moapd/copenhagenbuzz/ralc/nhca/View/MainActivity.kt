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

    import android.app.DatePickerDialog
    import android.content.Intent
    import androidx.navigation.fragment.NavHostFragment
    import android.media.Image
    import android.os.Bundle
    import android.util.Log
    import android.view.Menu
    import android.view.MenuItem
    import android.widget.ArrayAdapter
    import android.widget.AutoCompleteTextView
    import android.widget.EditText
    import android.widget.ImageView
    import androidx.appcompat.app.AppCompatActivity
    import androidx.navigation.ui.setupWithNavController
    import com.google.android.material.textfield.TextInputEditText
    import com.google.android.material.floatingactionbutton.FloatingActionButton
    import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
    import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
    import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding
    import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ContentMainBinding
    import java.util.Calendar
    import com.google.android.material.snackbar.Snackbar

    /**
     * The MainActivity class represents the main screen of the application, and also allows the user to input event details and add them to the event list.
     */
    class MainActivity : AppCompatActivity() {

        // Binding objects for the activity and content layouts
        private lateinit var activityMainBinding: ActivityMainBinding
        private lateinit var contentMainBinding: ContentMainBinding

        companion object {
            private lateinit var menuProfile: MenuItem
            private lateinit var menuLogout: MenuItem
        }

        /**
         * Called when the activity is first created. This inflates the different bindings with the necessary variables.
         * It also sets up the listeners for the different UI components.
         * @param savedInstanceState Saves the latest state of the activity, if it has been shut down.
         *
         */
        override fun onCreate(savedInstanceState: Bundle?) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)

            // Inflate the layout for this activity
            activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
            // Set the content view of the activity
            setContentView(activityMainBinding.root)

            // Bind the content layout
            contentMainBinding = ContentMainBinding.bind(activityMainBinding.root.findViewById(R.id.content_main))

            // Set the toolbar as the action bar
            setSupportActionBar(contentMainBinding.toolbar)


            val navHostFragment = supportFragmentManager
                .findFragmentById(
                    R.id.fragment_container_view
                ) as NavHostFragment
            val navController = navHostFragment.navController

            activityMainBinding.bottomNavigation.setupWithNavController(navController)

        }

        override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_toolbar, menu)
            if (menu != null) {
                menuProfile = menu.findItem(R.id.menu_profile)
                menuLogout = menu.findItem(R.id.menu_logout)
            }
            setMenuListeners()

            return true
        }

        override fun onPrepareOptionsMenu(menu: Menu): Boolean {
            val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
            menuProfile.isVisible = isLoggedIn
            menuLogout.isVisible = !isLoggedIn
            return super.onPrepareOptionsMenu(menu)
        }


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
    }