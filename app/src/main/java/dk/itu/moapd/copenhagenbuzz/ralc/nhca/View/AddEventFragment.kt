package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.app.DatePickerDialog
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.EventLocation
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentAddEventBinding
import io.github.cdimascio.dotenv.dotenv
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.toString

class AddEventFragment : Fragment() {

    private lateinit var binding: FragmentAddEventBinding
    private lateinit var database: DatabaseReference
    private lateinit var geocoder: Geocoder

    // UI Elements
    private lateinit var eventName: EditText
    private lateinit var eventLocation: EditText
    private lateinit var eventPhotoURL: EditText
    private lateinit var eventDate: TextInputEditText
    private lateinit var eventType: AutoCompleteTextView
    private lateinit var eventDescription: EditText
    private lateinit var addEventButton: Button

    // Location coordinates
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var hasValidCoordinates: Boolean = false

    // Event model
    private val event: Event = Event("", "", EventLocation(0.0,0.0,""), "", 0L, "", "")

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

        // Initialize Geocoder
        geocoder = Geocoder(requireContext(), Locale.getDefault())

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

        // Add a text change listener to the location field for geocoding
        eventLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val locationText = s.toString().trim()
                if (locationText.length > 3) { // Only geocode if there's enough text
                    geocodeLocation(locationText)
                } else {
                    hasValidCoordinates = false
                }
            }
        })

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
            latitude = it.getDouble(EVENT_LATITUDE, 0.0)
            longitude = it.getDouble(EVENT_LONGITUDE, 0.0)
            hasValidCoordinates = it.getBoolean(HAS_VALID_COORDINATES, false)
        }
    }

    /**
     * Geocodes a location string to get latitude and longitude
     * @param locationText The location text to geocode
     */
    private fun geocodeLocation(locationText: String) {
        // Run geocoding in a background thread to avoid blocking UI
        thread {
            try {
                val addressList = geocoder.getFromLocationName(locationText, 1)

                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    latitude = address.latitude
                    longitude = address.longitude
                    hasValidCoordinates = true

                    // Show success message on UI thread
                    activity?.runOnUiThread {
                        showSnackbar("Location found: ${address.getAddressLine(0)}")
                    }
                } else {
                    hasValidCoordinates = false
                    activity?.runOnUiThread {
                        // Only show error if user has finished typing
                        if (!eventLocation.isFocused) {
                            showSnackbar("Couldn't find location. Please check the address.")
                        }
                    }
                }
            } catch (e: IOException) {
                hasValidCoordinates = false
                activity?.runOnUiThread {
                    showSnackbar("Geocoding error: ${e.message}")
                }
            }
        }
    }

    /**
     * Reverse geocodes coordinates to get address details
     * @param lat Latitude
     * @param lng Longitude
     * @return Address string or null if not found
     */
    private fun reverseGeocode(lat: Double, lng: Double): String? {
        try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressLines = mutableListOf<String>()

                // Get address components
                val streetNumber = address.subThoroughfare ?: ""
                val street = address.thoroughfare ?: ""
                val city = address.locality ?: ""
                val postalCode = address.postalCode ?: ""

                // Build formatted address
                if (street.isNotEmpty()) {
                    addressLines.add("$street $streetNumber")
                }
                if (city.isNotEmpty()) {
                    addressLines.add(city)
                }
                if (postalCode.isNotEmpty()) {
                    addressLines.add(postalCode)
                }

                return addressLines.joinToString(", ")
            }
        } catch (e: IOException) {
            // Handle exception
        }
        return null
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

        // Check if we have valid coordinates
        if (!hasValidCoordinates) {
            showSnackbar("Location coordinates couldn't be determined. Please check the address.")
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

        val locationText = eventLocation.text.toString().trim()
        val eventLocation = EventLocation(latitude, longitude, locationText)

        // Create the event object
        val event = Event(
            creatorUserId = userId,
            eventName = eventName.text.toString().trim(),
            eventLocation = eventLocation,
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
        latitude = 0.0
        longitude = 0.0
        hasValidCoordinates = false
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
        outState.putDouble(EVENT_LATITUDE, latitude)
        outState.putDouble(EVENT_LONGITUDE, longitude)
        outState.putBoolean(HAS_VALID_COORDINATES, hasValidCoordinates)
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
        private const val EVENT_LATITUDE = "EVENT_LATITUDE"
        private const val EVENT_LONGITUDE = "EVENT_LONGITUDE"
        private const val HAS_VALID_COORDINATES = "HAS_VALID_COORDINATES"
    }
}