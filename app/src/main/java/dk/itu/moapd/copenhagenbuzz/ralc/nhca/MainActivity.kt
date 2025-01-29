package dk.itu.moapd.copenhagenbuzz.ralc.nhca

import android.app.DatePickerDialog
import android.os.Bundle
import android .util.Log
import android.view.Window
import android .widget . EditText
import androidx.appcompat.app.AppCompatActivity
import androidx .core.view. WindowCompat
import com.google.android.material.textfield.TextInputEditText
import com.google . android . material . floatingactionbutton . FloatingActionButton
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }

    // GUI variables
    private lateinit var eventName: EditText
    private lateinit var eventLocation: EditText
    private lateinit var eventDate: EditText
    private lateinit var eventType: EditText
    private lateinit var eventDescription: EditText
    private lateinit var addEventButton: FloatingActionButton

    // Creates an instance of the Event class
    private val event: Event = Event("", "", "", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Linking of UI components
        eventName = findViewById(R.id.edit_text_event_name)
        eventLocation = findViewById(R.id.edit_text_event_location)
        eventDate = findViewById(R.id.edit_text_event_date)
        eventType = findViewById(R.id.edit_text_event_type)
        eventDescription = findViewById(R.id.edit_text_event_description)
        addEventButton = findViewById(R.id.floating_button_event_add)

        // Listener for user interaction in the `Add Event` button.
        addEventButton.setOnClickListener {
            // Only execute the following code when the user fills all `EditText`.
            if (eventName.text.toString().isNotEmpty() &&
                eventLocation.text.toString().isNotEmpty()) {

                // Update the object attributes.
                event.setEventName(eventName.text.toString().trim())
                event.setEventLocation(eventLocation.text.toString().trim())
                event.setEventDate(eventDate.text.toString().trim())
                event.setEventType(eventType.text.toString().trim())
                event.setEventDescription(eventDescription.text.toString().trim())
                // TODO: Implement the missing code here.

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