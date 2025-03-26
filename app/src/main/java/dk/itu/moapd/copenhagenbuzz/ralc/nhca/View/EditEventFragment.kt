package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.DATABASE_URL
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentEditEventBinding
import io.github.cdimascio.dotenv.dotenv
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditEventFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditEventBinding
    private lateinit var database: DatabaseReference
    private lateinit var event: Event
    private lateinit var eventKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get event and event key from arguments
        arguments?.let {
            event = it.getParcelable("event") ?: Event()
            eventKey = it.getString("eventKey", "")
        }

        // Initialize Firebase database reference
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }
        database = Firebase.database(DATABASE_URL).reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate form with existing event data
        populateFormWithEventData()

        // Setup dropdown for event types
        setupEventTypeDropdown()

        // Setup listeners for buttons and date picker
        setupListeners()
    }

    private fun populateFormWithEventData() {
        binding.editTextEventName.setText(event.eventName)
        binding.editTextEventLocation.setText(event.eventLocation)
        binding.editTextEventPhotoUrl.setText(event.eventPhotoURL)

        // Format date for display
        val date = java.util.Date(event.eventDate)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        binding.editTextEventDate.setText(formattedDate)

        binding.autoCompleteTextViewEventType.setText(event.eventType)
        binding.editTextEventDescription.setText(event.eventDescription)
    }

    private fun setupEventTypeDropdown() {
        val eventTypes = arrayOf("Festival", "Meetup", "Workshop", "Seminar", "Conference", "Lan party")
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_dropdown_item, eventTypes)
        binding.autoCompleteTextViewEventType.setAdapter(adapter)
    }

    private fun setupListeners() {
        // Date picker
        binding.editTextEventDate.setOnClickListener { showCalendar(binding.editTextEventDate) }

        // Save button
        binding.updateEventButton.setOnClickListener {
            if (validateInputs()) {
                saveChangesToFirebase()
            }
        }

        // Delete button
        binding.deleteEventButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun validateInputs(): Boolean {
        // Add validation similar to AddEventFragment
        if (binding.editTextEventName.text.toString().trim().isEmpty()) {
            showSnackbar("Event name cannot be empty")
            return false
        }

        if (binding.editTextEventLocation.text.toString().trim().isEmpty()) {
            showSnackbar("Event location cannot be empty")
            return false
        }

        val dateString = binding.editTextEventDate.text.toString().trim()
        if (dateString.isEmpty()) {
            showSnackbar("Please select a date")
            return false
        }

        val timestamp = convertDateToTimestamp(dateString)
        if (timestamp == -1L) {
            showSnackbar("Invalid date format. Please use DD/MM/YYYY format")
            return false
        }

        return true
    }

    private fun saveChangesToFirebase() {
        // Update event object with new values
        event.eventName = binding.editTextEventName.text.toString().trim()
        event.eventLocation = binding.editTextEventLocation.text.toString().trim()
        event.eventPhotoURL = binding.editTextEventPhotoUrl.text.toString().trim()
        event.eventDate = convertDateToTimestamp(binding.editTextEventDate.text.toString().trim())
        event.eventType = binding.autoCompleteTextViewEventType.text.toString().trim()
        event.eventDescription = binding.editTextEventDescription.text.toString().trim()

        // Save to Firebase
        database.child("Events").child(eventKey).setValue(event)
            .addOnSuccessListener {
                showSnackbar("Event updated successfully!")
                dismiss()
            }
            .addOnFailureListener { e ->
                showSnackbar("Failed to update event: ${e.message}")
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ -> deleteEvent() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent() {
        // Verify the current user is the event creator
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        // This should never happen, but is there just in case
        if (userId != event.creatorUserId) {
            showSnackbar("You do not have permission to delete this event")
            return
        }

        // Reference to the favorites node
        val favoritesRef = database.child("Favorites")

        // First, remove the event from all users' favorites
        favoritesRef.get().addOnSuccessListener { favoritesSnapshot ->
            val deletionTasks = mutableListOf<Task<Void>>()

            // Iterate through all users' favorites
            favoritesSnapshot.children.forEach { userFavorites ->
                val userFavoritesRef = favoritesRef.child(userFavorites.key ?: "")

                // Remove the specific event from this user's favorites
                val deletionTask = userFavoritesRef.child(eventKey).removeValue()
                deletionTasks.add(deletionTask)
            }

            // After removing from all favorites, delete the event itself
            Tasks.whenAll(deletionTasks)
                .addOnSuccessListener {
                    // Now delete the event from the Events node
                    database.child("Events").child(eventKey).removeValue()
                        .addOnSuccessListener {
                            showSnackbar("Event deleted successfully")
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            showSnackbar("Failed to delete event: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    showSnackbar("Failed to remove event from favorites: ${e.message}")
                }
        }.addOnFailureListener { e ->
            showSnackbar("Error accessing favorites: ${e.message}")
        }
    }

    private fun showCalendar(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()

        // If there's a date already set, use it as the default
        val dateString = editText.text.toString()
        if (dateString.isNotEmpty()) {
            try {
                val parts = dateString.split("/")
                if (parts.size == 3) {
                    calendar.set(
                        parts[2].toInt(), // year
                        parts[1].toInt() - 1, // month (0-based)
                        parts[0].toInt() // day
                    )
                }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }

        DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, day ->
                val selectedDate = "$day/${month + 1}/$year"
                editText.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

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
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}