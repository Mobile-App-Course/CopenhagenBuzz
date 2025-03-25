package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentAddEventBinding
import io.github.cdimascio.dotenv.dotenv
import java.util.Calendar

class AddEventFragment : Fragment() {

    private lateinit var binding: FragmentAddEventBinding
    private lateinit var database: DatabaseReference
    
    // UI Elements
    private lateinit var eventName: EditText
    private lateinit var eventLocation: EditText
    private lateinit var eventPhotoURL: EditText
    private lateinit var eventDate: TextInputEditText
    private lateinit var eventType: AutoCompleteTextView
    private lateinit var eventDescription: EditText
    private lateinit var addEventButton: Button

    // Event model
    private val event: Event = Event("", "", "", "", 0L, "", "")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEventBinding.inflate(inflater, container, false)

        // Initialize Firebase Database reference
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }
        database = Firebase.database(dotenv["DATABASE_URL"]).reference
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI Elements
        eventName = binding.editTextEventName
        eventLocation = binding.editTextEventLocation
        eventPhotoURL = binding.editTextEventPhotoUrl
        eventDate = binding.editTextEventDate
        eventType = binding.autoCompleteTextViewEventType
        eventDescription = binding.editTextEventDescription
        addEventButton = binding.addEventButton

        // Setup AutoCompleteTextView
        val eventTypes = arrayOf("Festival", "Meetup", "Workshop", "Seminar", "Conference", "Lan party")
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_dropdown_item, eventTypes)

        val eventTypeDropdown = binding.autoCompleteTextViewEventType
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

        // Button click listener
        addEventButton.setOnClickListener {
            if (validateInputs()) {
                saveEventToFirebase()
            }
        }

        // Date Picker
        eventDate.setOnClickListener { showCalendar(eventDate) }

        // Restore saved state
        savedInstanceState?.let {
            eventName.setText(it.getString(EVENT_NAME, ""))
            eventLocation.setText(it.getString(EVENT_LOCATION, ""))
            eventPhotoURL.setText(it.getString(EVENT_PHOTO_URL, ""))
            eventDate.setText(it.getString(EVENT_DATE, ""))
            eventType.setText(it.getString(EVENT_TYPE, ""))
            eventDescription.setText(it.getString(EVENT_DESCRIPTION, ""))
        }
    }

    /**
     * Validates user inputs before saving to Firebase
     * @return Boolean indicating if all inputs are valid
     */
    private fun validateInputs(): Boolean {
        // Check if required fields are not empty
        if (eventName.text.toString().trim().isEmpty()) {
            showSnackbar("Event name cannot be empty")
            return false
        }

        if (eventLocation.text.toString().trim().isEmpty()) {
            showSnackbar("Event location cannot be empty")
            return false
        }

        // Validate date format
        val dateString = eventDate.text.toString().trim()
        if (dateString.isEmpty()) {
            showSnackbar("Please select a date")
            return false
        }

        val timestamp = convertDateToTimestamp(dateString)
        if (timestamp == -1L) {
            showSnackbar("Invalid date format. Please use DD/MM/YYYY format")
            return false
        }

        // Validate photo URL format - optional basic check
        val photoUrl = eventPhotoURL.text.toString().trim()
        if (photoUrl.isNotEmpty() && !photoUrl.startsWith("http")) {
            showSnackbar("Photo URL should start with http:// or https://")
            return false
        }

        return true
    }



    /**
     * Saves the event data to Firebase Realtime Database
     */
    private fun saveEventToFirebase() {
        // Get the current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "anonymous"

        // Create the event object
        val event = Event(
            creatorUserId = userId,
            eventName = eventName.text.toString().trim(),
            eventLocation = eventLocation.text.toString().trim(),
            eventPhotoURL = eventPhotoURL.text.toString().trim(),
            eventDate = convertDateToTimestamp(eventDate.text.toString().trim()),
            eventType = eventType.text.toString().trim(),
            eventDescription = eventDescription.text.toString().trim()
        )

        // Saves to Firebase using push() that generates a unique key
        val eventsRef = database.child("Events").push()

        eventsRef.setValue(event)
            .addOnSuccessListener {
                showSnackbar("Event added successfully!")
                clearForm()
            }
            .addOnFailureListener { e ->
                showSnackbar("Failed to add event: ${e.message}")
            }
    }

    /**
     * Clears the form after successful submission
     */
    private fun clearForm() {
        eventName.text?.clear()
        eventLocation.text?.clear()
        eventPhotoURL.text?.clear()
        eventDate.text?.clear()
        eventType.text?.clear()
        eventDescription.text?.clear()
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
        super.onSaveInstanceState(outState)
        outState.putString(EVENT_NAME, eventName.text.toString())
        outState.putString(EVENT_LOCATION, eventLocation.text.toString())
        outState.putString(EVENT_PHOTO_URL, eventPhotoURL.text.toString())
        outState.putString(EVENT_DATE, eventDate.text.toString())
        outState.putString(EVENT_TYPE, eventType.text.toString())
        outState.putString(EVENT_DESCRIPTION, eventDescription.text.toString())
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
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(selectedDate)
            },
            calYear,
            calMonth,
            calDay
        )
        calendarPicker.show()
    }

    /**
     * Function to log the event details.
     */
    private fun showMessage() {
        val message = "Event Added: " + event.toString()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(addEventButton)
            .show()
    }

    /**
     * Function to display a snackbar message
     * @param message The message to display
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(addEventButton)
            .show()
    }
    
    /**
     * Converts a date string in format "DD/MM/YYYY" to a timestamp in milliseconds
     * Returns a distinctive error value (-1L) if parsing fails
     */
    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val parts = dateString.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1 // Calendar months are 0-based
                val year = parts[2].toInt()

                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            } else {
                -1L // Return an error value instead, which can then be checked on
            }
        } catch (e: Exception) {
            -1L // Error value
        }
    }

    companion object {
        private const val EVENT_NAME = "EVENT_NAME"
        private const val EVENT_LOCATION = "EVENT_LOCATION"
        private const val EVENT_PHOTO_URL = "EVENT_PHOTO_URL"
        private const val EVENT_DATE = "EVENT_DATE"
        private const val EVENT_TYPE = "EVENT_TYPE"
        private const val EVENT_DESCRIPTION = "EVENT_DESCRIPTION"
    }
}
