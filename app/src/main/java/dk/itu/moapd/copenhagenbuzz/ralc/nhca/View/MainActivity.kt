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
            // Tag for logging
            private val TAG = MainActivity::class.qualifiedName
            // Keys for saving the state of the activity
            private const val EVENT_NAME = "EVENT_NAME"
            private const val EVENT_LOCATION = "EVENT_LOCATION"
            private const val EVENT_DATE = "EVENT_DATE"
            private const val EVENT_TYPE = "EVENT_TYPE"
            private const val EVENT_DESCRIPTION = "EVENT_DESCRIPTION"
        }

        // GUI variables
        private lateinit var eventName: EditText
        private lateinit var eventLocation: EditText
        private lateinit var eventDate: EditText
        private lateinit var eventType: AutoCompleteTextView
        private lateinit var eventDescription: EditText
        private lateinit var addEventButton: FloatingActionButton
        private lateinit var menuProfile: MenuItem
        private lateinit var menuLogout: MenuItem

        // Creates an instance of the Event class
        private val event: Event = Event("", "", "", "", "")

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
            setContentView(activityMainBinding.root)
            setSupportActionBar(findViewById(R.id.toolbar))

            // Bind the content layout
            contentMainBinding = ContentMainBinding.bind(activityMainBinding.root.findViewById(R.id.content_main))

            // Linking of UI components
            eventName = contentMainBinding.editTextEventName
            eventLocation = contentMainBinding.editTextEventLocation
            eventDate = contentMainBinding.editTextEventDate
            eventType = contentMainBinding.autoCompleteTextViewEventType
            eventDescription = contentMainBinding.editTextEventDescription
            addEventButton = contentMainBinding.floatingButtonEventAdd


            // AutoCompleteTextView (list of event types) configuration
            val eventTypes = arrayOf("Festival", "Meetup", "Workshop", "Seminar", "Conference")
            val adapter = ArrayAdapter(this, R.layout.custom_dropdown_item, eventTypes)

            val eventTypeDropdown = findViewById<AutoCompleteTextView>(R.id.auto_complete_text_view_event_type)
            eventTypeDropdown.setAdapter(adapter)

            // Listener for if the user clicks on the Event Type once
            eventTypeDropdown.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    eventTypeDropdown.showDropDown()
                }
            }

            // Listener for if the user clicks away from the Event Type (but doesn't click on any other boxes), and then clicks back unto the list
            eventTypeDropdown.setOnClickListener {
                eventTypeDropdown.requestFocus() // Ensure it gets focus
                eventTypeDropdown.showDropDown() // Show dropdown immediately
            }

            // Listener for user interaction in the `Add Event` button.
            addEventButton.setOnClickListener {
                // Only execute the following code when the user fills all `EditText`.
                if (eventName.text.toString().isNotEmpty() &&
                    eventLocation.text.toString().isNotEmpty()) {

                    // Update the object attributes.
                    event.eventName = eventName.text.toString().trim()
                    event.eventLocation = eventLocation.text.toString().trim()
                    event.eventDate = eventDate.text.toString().trim()
                    event.eventType = eventType.text.toString().trim()
                    event.eventDescription = eventDescription.text.toString().trim()

                    // Write in the `Logcat` system.
                    showMessage()
                }
            }

            // Set a OnClickListener to show the calendar when clicked
            eventDate.setOnClickListener {
                showCalendar(eventDate as TextInputEditText)
            }
            // Checks if there is a saved instance state and sets the text fields to the saved values, otherwise empty
            savedInstanceState?.let {
                eventName.setText(it.getString(EVENT_NAME, ""))
                eventLocation.setText(it.getString(EVENT_LOCATION, ""))
                eventDate.setText(it.getString(EVENT_DATE, ""))
                eventType.setText(it.getString(EVENT_TYPE, ""))
                eventDescription.setText(it.getString(EVENT_DESCRIPTION, ""))
            }
        }

        override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_toolbar, menu)
            if (menu != null) {
                menuProfile = menu.findItem(R.id.menu_profile)
                menuLogout = menu.findItem(R.id.menu_logout)
            }
            return true
        }
        
        override fun onPrepareOptionsMenu(menu: Menu): Boolean {
            val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
            menuProfile.isVisible = isLoggedIn
            menuLogout.isVisible = !isLoggedIn
            return super.onPrepareOptionsMenu(menu)
        }


        /**
         * The method saves the current state of the activity.
         *
         * It is called before the activity may be destroyed so that the state can be saved.
         * The state is then saved in the provided Bundle object.
         *
         * @param outState The Bundle in which to place the saved state.
         */
        override fun onSaveInstanceState(outState: Bundle) {
            outState.putString(EVENT_NAME, eventName.text.toString())
            outState.putString(EVENT_LOCATION, eventLocation.text.toString())
            outState.putString(EVENT_DATE, eventDate.text.toString())
            outState.putString(EVENT_TYPE, eventType.text.toString())
            outState.putString(EVENT_DESCRIPTION, eventDescription.text.toString())
            super.onSaveInstanceState(outState)
        }

        /**
         * Function to show the calendar and allow the user to pick a date.
         * @param editText The TextInputEditText where the selected date will be displayed.
         */
        private fun showCalendar(editText: TextInputEditText) {
            // Values used to get the current date
            val calendar = Calendar.getInstance()
            val calYear = calendar.get(Calendar.YEAR)
            val calMonth = calendar.get(Calendar.MONTH)
            val calDay = calendar.get(Calendar.DAY_OF_MONTH)

            // Used to create and show the calendar
            val calendarPicker = DatePickerDialog(
                this,
                R.style.CustomDatePickerDialog,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formats the selected date to be displayed in the EditText
                    val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    editText.setText(selectedDate)
                },
                calYear, calMonth, calDay
            )
            calendarPicker.show()
        }

        /**
         * Function to log the event details.
         */
        private fun showMessage() {
            val message = "Event Added: " + event.toString()
            Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }