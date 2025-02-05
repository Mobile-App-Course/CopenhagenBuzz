package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.app.DatePickerDialog
import android.os.Bundle
import android .util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android .widget . EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google . android . material . floatingactionbutton . FloatingActionButton
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ContentMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var contentMainBinding: ContentMainBinding

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }

    // GUI variables
    private lateinit var eventName: EditText
    private lateinit var eventLocation: EditText
    private lateinit var eventDate: EditText
    private lateinit var eventType: AutoCompleteTextView
    private lateinit var eventDescription: EditText
    private lateinit var addEventButton: FloatingActionButton


    // Creates an instance of the Event class
    private val event: Event = Event("", "", "", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

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
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, eventTypes)

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
    }

    // Function to show the calendar
    private fun showCalendar(editText: TextInputEditText) {
        // Values used to get the current date
        val calendar = Calendar.getInstance()
        val calYear = calendar.get(Calendar.YEAR)
        val calMonth = calendar.get(Calendar.MONTH)
        val calDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Used to create and show the calendar
        val calendarPicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formats the selected date to be displayed in the EditText
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(selectedDate)
            },
            calYear, calMonth, calDay
        )
        calendarPicker.show()
    }

    private fun showMessage() {
        Log.d(TAG, event.toString())
    }
}