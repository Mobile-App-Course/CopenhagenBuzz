package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.EventLocation
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.Resource
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentAddEventBinding
import io.github.cdimascio.dotenv.dotenv
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.jvm.java

/**
 * Fragment for adding a new event.
 * Handles user input, geocoding, image selection, and saving event data to Firebase.
 */
class AddEventFragment : Fragment() {

    private lateinit var binding: FragmentAddEventBinding
    private lateinit var database: DatabaseReference
    private lateinit var viewModel: DataViewModel
    private lateinit var geocoder: Geocoder

    // UI Elements
    private lateinit var eventName: EditText
    private lateinit var eventLocation: EditText
    private lateinit var geocodeButton: Button
    private lateinit var eventPhotoURL: EditText
    private lateinit var eventDate: TextInputEditText
    private lateinit var eventType: AutoCompleteTextView
    private lateinit var eventDescription: EditText
    private lateinit var addEventButton: Button

    // Location coordinates
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var hasValidCoordinates: Boolean = false

    // Image URL
    private var imageUri: Uri? = null
    private var tempImageUri: Uri? = null
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var geocodedLocation: EventLocation? = null
    private var selectedDate: Long? = null

    // Event model
    private val event: Event = Event("", "", EventLocation(0.0,0.0,""), "", 0L, "", "")

    /**
     * Called when the fragment is created.
     * Initializes permission launchers and activity result launchers for camera and gallery.
     */
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[DataViewModel::class.java]

        // Permissions launcher setup
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                showSnackbar("Camera permission denied, it is needed to take photos")
            }
        }
        galleryPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                showSnackbar("Storage permission is needed to be able to select photos")
            }
        }

        // Activity result launchers setup for camera and gallery
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                tempImageUri?.let {
                    imageUri = it
                    updateImagePreview()
                }
            }

        }

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                imageUri = it
                updateImagePreview()
            }
        }
    }

    /**
     * Called to create the view hierarchy for the fragment.
     * Initializes Firebase database reference and geocoder.
     */
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

    /**
     * Called after the view hierarchy has been created.
     * Binds UI elements, sets up event listeners, and restores saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI Elements
        eventName = binding.editTextEventName
        eventLocation = binding.editTextEventLocation
        geocodeButton = binding.geocodeButton
        eventPhotoURL = binding.editTextEventPhotoUrl
        eventDate = binding.editTextEventDate
        eventType = binding.autoCompleteTextViewEventType
        eventDescription = binding.editTextEventDescription
        addEventButton = binding.addEventButton

        observeViewModel()

        geocodeButton.setOnClickListener{
            val locationText = eventLocation.text.toString().trim()
            if (locationText.isNotEmpty()) {
                viewModel.geocodeLocation(locationText, geocoder)
            } else {
                showSnackbar("Please enter a location to geocode")
            }
        }

        // Photo buttons setup
        binding.buttonCapturePhoto.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }

        binding.buttonSelectPhoto.setOnClickListener {
            checkGalleryPermissionAndOpenGallery()
        }

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
                binding.progressBar.visibility = View.VISIBLE

                if (imageUri != null) {
                    // Upload image first, then save event
                    viewModel.uploadEventImage(imageUri!!)
                } else {
                    // Save event with URL from EditText
                    val photoUrl = eventPhotoURL.text.toString().trim()
                    saveEvent(photoUrl)
                }
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
            imageUri = it.getParcelable(IMAGE_URI)
            updateImagePreview()
        }
    }

    /**
     * Checks for camera permission and opens the camera if granted.
     * Shows a rationale dialog if permission is denied.
     */
    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog(
                    "Camera Permission",
                    "Camera permission is needed to take photos for events",
                    Manifest.permission.CAMERA,
                    cameraPermissionLauncher
                )
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun observeViewModel() {
        // Observe geocoding results
        viewModel.geocodeStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    geocodedLocation = resource.data
                    showSnackbar("Location found: ${resource.data.address}")
                }
                is Resource.Error -> {
                    showSnackbar(resource.message)
                    geocodedLocation = null
                }
                is Resource.Loading -> {
                    // Show loading state if needed
                }
            }
        }

        // Observe image upload status
        viewModel.uploadStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    saveEvent(resource.data)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showSnackbar(resource.message)
                }
                is Resource.Loading -> {
                    // Progress already shown
                }
            }
        }

        // Observe event save status
        viewModel.addEventStatus.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.visibility = View.GONE
            when (resource) {
                is Resource.Success -> {
                    showSnackbar("Event added successfully!")
                    clearForm()
                }
                is Resource.Error -> {
                    showSnackbar(resource.message)
                }
                is Resource.Loading -> {
                    // Progress already shown
                }
            }
        }
    }

    /**
     * Checks for gallery permission and opens the gallery if granted.
     * Shows a rationale dialog if permission is denied.
     */
    private fun checkGalleryPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, open gallery
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Show dialog explaining why permission is needed
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Gallery Permission Required")
                    .setMessage("Storage permission is needed to select images for your events.")
                    .setPositiveButton("OK") { _, _ -> galleryPermissionLauncher.launch(permission) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                // Request permission directly
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Shows a dialog explaining why a permission is needed.
     * @param title The title of the dialog.
     * @param message The message explaining the permission.
     * @param permission The permission being requested.
     * @param permissionLauncher The launcher to request the permission.
     */
    private fun showPermissionRationaleDialog(
        title: String,
        message: String,
        permission: String,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") {_,_ ->
                permissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Opens the camera to capture a photo.
     * Creates a temporary URI for the photo.
     */
    private fun openCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Event Photo")
            put(MediaStore.Images.Media.DESCRIPTION, "From Copenhagen Buzz App")
        }

        tempImageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )

        tempImageUri?.let {
            cameraLauncher.launch(it)
        } ?: showSnackbar("Failed to create image file")
    }

    /**
     * Opens the gallery to select an image.
     */
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    /**
     * Updates the image preview with the selected or captured image.
     * Hides the photo URL input if an image is selected.
     */
    private fun updateImagePreview() {
        if (imageUri != null) {
            // Show the image preview and hide the URL input
            binding.imagePreview.visibility = View.VISIBLE
            binding.textInputLayoutEventPhotoUrl.visibility = View.GONE

            // Uses Picasso to load image
            Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.baseline_firebase)
                .into(binding.imagePreview)

            // Removes previous entered url
            eventPhotoURL.setText("")
        } else {
            // Show URL input and hide the image preview
            binding.imagePreview.visibility = View.GONE
            binding.textInputLayoutEventPhotoUrl.visibility = View.VISIBLE
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

        // Check if we have valid coordinates
        if (validateCoordinates()) {
            showSnackbar("Location coordinates couldn't be determined. Please check the address.")
            return false
        }

        return true
    }

    private fun validateCoordinates(): Boolean {
        var locationText = binding.editTextEventLocation.text.toString()

        val addressList = geocoder.getFromLocationName(locationText, 1)

        return addressList.isNullOrEmpty()
    }

    private fun saveEvent(photoUrl: String) {
        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        // Get values from form
        val name = binding.editTextEventName.text.toString()
        val locationText = binding.editTextEventLocation.text.toString()

        // Use either geocoded location or default
        val eventLocation = geocodedLocation ?: EventLocation(55.671, 12.5683, locationText)

        val timestamp = selectedDate ?: System.currentTimeMillis()
        val type = binding.autoCompleteTextViewEventType.text.toString()
        val description = binding.editTextEventDescription.text.toString()

        // Create event object
        val event = Event(userId, name, eventLocation, photoUrl, timestamp, type, description)

        // Save using ViewModel
        viewModel.saveEventToFirebase(event)
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
        geocodedLocation = null
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
     * Displays a calendar dialog to allow the user to pick a date.
     *
     * The selected date is formatted as "DD/MM/YYYY" and displayed in the provided `TextInputEditText`.
     *
     * @param editText The `TextInputEditText` where the selected date will be displayed.
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
     * Displays a `Snackbar` message.
     *
     * This method is used to show a brief message to the user at the bottom of the screen.
     *
     * @param message The message to display in the `Snackbar`.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(addEventButton)
            .show()
    }

    /**
     * Converts a date string in the format "DD/MM/YYYY" to a timestamp in milliseconds.
     *
     * This method parses the provided date string and converts it into a timestamp. If the
     * parsing fails, it returns a distinctive error value (-1L).
     *
     * @param dateString The date string to convert, in the format "DD/MM/YYYY".
     * @return The timestamp in milliseconds, or -1L if parsing fails.
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

    /**
     * Companion object containing constants used in the fragment.
     */
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

        private const val IMAGE_URI = "image_uri"
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
    }
}