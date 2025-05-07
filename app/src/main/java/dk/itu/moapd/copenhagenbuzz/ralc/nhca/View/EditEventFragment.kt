package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.DATABASE_URL
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.EventLocation
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentEditEventBinding
import io.github.cdimascio.dotenv.dotenv
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/**
 * Creates a notification for the foreground service.
 *
 * This method builds a notification with a title, text, and a small icon. It also
 * includes a pending intent that opens the `MainActivity` when the notification is clicked.
 *
 * @return The created `Notification` object.
 */
class EditEventFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditEventBinding
    private lateinit var database: DatabaseReference
    private lateinit var event: Event
    private lateinit var eventKey: String
    private lateinit var geocoder: Geocoder
    private var imageUri: Uri? = null

    // Add these fields to the EditEventFragment class
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var tempImageUri: Uri? = null

    // Location coordinates
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var hasValidCoordinates: Boolean = false

    /**
     * Activity result launcher for selecting an image from the gallery.
     * Updates the `imageUri` and displays the selected image.
     */
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                imageUri = uri
                displaySelectedImage(uri)
                // Store the URI string to the hidden field
                binding.editTextEventPhotoUrl.setText(uri.toString())
            }
        }
    }

    /**
     * Called when the fragment is created.
     * Initializes event data, Firebase database reference, geocoder, and permission launchers.
     *
     * @param savedInstanceState The saved state of the fragment.
     */
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

        // Initialize Geocoder
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // Initialize location data from event
        latitude = event.eventLocation.latitude
        longitude = event.eventLocation.longitude
        hasValidCoordinates = (latitude != 0.0 || longitude != 0.0)

        // Initialize permission launchers
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
                showSnackbar("Storage permission is needed to select photos")
            }
        }

        // Initialize activity result launchers for camera and gallery
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                tempImageUri?.let {
                    imageUri = it
                    displaySelectedImage(it)
                }
            }
        }

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                imageUri = it
                displaySelectedImage(it)
            }
        }
    }

    /**
     * Called to create the view hierarchy of the fragment.
     *
     * @param inflater The `LayoutInflater` used to inflate the layout.
     * @param container The parent view that the fragment's UI will be attached to.
     * @param savedInstanceState The saved state of the fragment.
     * @return The root view of the fragment's layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called after the view hierarchy has been created.
     * Sets up the UI components and listeners.
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState The saved state of the fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate form with existing event data
        populateFormWithEventData()

        // Setup dropdown for event types
        setupEventTypeDropdown()

        // Setup listeners for buttons and date picker
        setupListeners()

        // Add text change listener for location field
        setupLocationListener()

        // Setup image click listener
        setupImageClickListener()

        // Load existing image if available
        loadExistingImageIfAvailable()
    }

    /**
     * Populates the form fields with the existing event data.
     */
    private fun populateFormWithEventData() {
        binding.editTextEventName.setText(event.eventName)
        binding.editTextEventLocation.setText(event.eventLocation.address)
        binding.editTextEventPhotoUrl.setText(event.eventPhotoURL)

        // Format date for display
        val date = java.util.Date(event.eventDate)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        binding.editTextEventDate.setText(formattedDate)

        binding.autoCompleteTextViewEventType.setText(event.eventType)
        binding.editTextEventDescription.setText(event.eventDescription)
    }

    /**
     * Sets up the dropdown menu for selecting event types.
     */
    private fun setupEventTypeDropdown() {
        val eventTypes = arrayOf("Festival", "Meetup", "Workshop", "Seminar", "Conference", "Lan party")
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_dropdown_item, eventTypes)
        binding.autoCompleteTextViewEventType.setAdapter(adapter)
    }

    /**
     * Sets up listeners for various UI components, such as buttons and the date picker.
     */
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

    /**
     * Sets up a click listener for the image card to show image picker options.
     */
    private fun setupImageClickListener() {
        binding.eventImageCard.setOnClickListener {
            showImagePickerOptions()
        }
    }

    /**
     * Loads the existing event image into the image view, if available.
     */
    private fun loadExistingImageIfAvailable() {
        val existingImageUrl = binding.editTextEventPhotoUrl.text.toString()
        if (existingImageUrl.isNotEmpty()) {
            try {
                // Check if it's a Uri or a URL
                if (existingImageUrl.startsWith("http")) {
                    // It's a web URL
                    Glide.with(this)
                        .load(existingImageUrl)
                        .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
                        .error(R.drawable.ic_image_error) // Add an error drawable
                        .into(binding.eventImageView)
                } else {
                    // It's likely a local URI
                    val uri = Uri.parse(existingImageUrl)
                    Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
                        .error(R.drawable.ic_image_error) // Add an error drawable
                        .into(binding.eventImageView)
                }
            } catch (e: Exception) {
                // Handle error loading image
                binding.eventImageView.setImageResource(R.drawable.ic_image_placeholder) // Use your placeholder drawable
            }
        } else {
            // No image, set a placeholder
            binding.eventImageView.setImageResource(R.drawable.ic_image_placeholder) // Use your placeholder drawable
        }
    }

    /**
     * Displays a dialog with options to take a photo or choose one from the gallery.
     */
    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Option")
            .setItems(options) { dialog, index ->
                when (index) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> checkGalleryPermissionAndOpenGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * Displays the selected image in the image view.
     *
     * @param uri The URI of the selected image.
     */
    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
            .error(R.drawable.ic_image_error) // Add an error drawable
            .into(binding.eventImageView)
    }

    /**
     * Sets up a text change listener for the location field to perform geocoding.
     */
    private fun setupLocationListener() {
        // Add a text change listener to the location field for geocoding
        binding.editTextEventLocation.addTextChangedListener(object : TextWatcher {
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
    }

    /**
     * Geocodes a location string to get latitude and longitude.
     *
     * This method uses the `Geocoder` to convert a location name into geographic coordinates.
     * It runs the geocoding process in a background thread to avoid blocking the UI thread.
     * If the geocoding is successful, the latitude and longitude are updated, and a success
     * message is displayed. Otherwise, an error message is shown.
     *
     * @param locationText The location text to geocode.
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
                        if (!binding.editTextEventLocation.isFocused) {
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
     * Validates the input fields for the event form.
     *
     * This method checks if the event name, location, and date fields are filled in correctly.
     * It also validates the date format and ensures that the location coordinates are valid.
     * If any validation fails, an appropriate error message is displayed.
     *
     * @return `true` if all inputs are valid, `false` otherwise.
     */
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

        // Check if we have valid coordinates
        if (!hasValidCoordinates) {
            showSnackbar("Location coordinates couldn't be determined. Please check the address.")
            return false
        }

        return true
    }

    /**
     * Saves the changes made to the event in Firebase.
     *
     * This method disables the update button during the upload process. If a new image is selected,
     * it uploads the image to Firebase Storage before saving the event. Otherwise, it directly
     * updates the event with the existing image URL.
     */
    private fun saveChangesToFirebase() {
        binding.updateEventButton.isEnabled = false // Disable button during upload

        // If there's a new image to upload
        if (imageUri != null && !imageUri.toString().startsWith("http")) {
            uploadPhotoAndSaveEvent()
        } else {
            // No new image, just save the existing URL
            saveEventWithPhotoUrl(event.eventPhotoURL)
        }
    }

    /**
     * Uploads a new photo to Firebase Storage and saves the event.
     *
     * This method deletes the old photo from Firebase Storage (if it exists) and uploads the
     * new photo. Once the upload is successful, it retrieves the download URL and saves the
     * event with the new photo URL.
     */
    private fun uploadPhotoAndSaveEvent() {
        val storageRef = FirebaseStorage.getInstance().reference

        // Delete the old photo if it exists
        val oldPhotoUrl = event.eventPhotoURL
        if (oldPhotoUrl.isNotEmpty() && oldPhotoUrl.startsWith("https://")) {
            val oldPhotoRef = storageRef.storage.getReferenceFromUrl(oldPhotoUrl)
            oldPhotoRef.delete()
                .addOnSuccessListener {
                    showSnackbar("Old photo deleted successfully")
                }
                .addOnFailureListener { e ->
                    showSnackbar("Failed to delete old photo: ${e.message}")
                }
        }

        // Upload the new photo
        val photoRef = storageRef.child("event_photos/${UUID.randomUUID()}")
        photoRef.putFile(imageUri!!)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    saveEventWithPhotoUrl(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                binding.updateEventButton.isEnabled = true
                showSnackbar("Failed to upload image: ${e.message}")
            }
    }

    /**
     * Saves the event with the provided photo URL to Firebase.
     *
     * This method updates the event object with the new values from the form fields and saves
     * it to the Firebase Realtime Database.
     *
     * @param photoUrl The URL of the photo to associate with the event.
     */
    private fun saveEventWithPhotoUrl(photoUrl: String) {
        // Create an updated EventLocation object
        val locationText = binding.editTextEventLocation.text.toString().trim()
        val updatedEventLocation = EventLocation(latitude, longitude, locationText)

        // Update event object with new values
        event.eventName = binding.editTextEventName.text.toString().trim()
        event.eventLocation = updatedEventLocation
        event.eventPhotoURL = photoUrl
        event.eventDate = convertDateToTimestamp(binding.editTextEventDate.text.toString().trim())
        event.eventType = binding.autoCompleteTextViewEventType.text.toString().trim()
        event.eventDescription = binding.editTextEventDescription.text.toString().trim()

        // Save to Firebase
        database.child("Events").child(eventKey).setValue(event)
            .addOnSuccessListener {
                binding.updateEventButton.isEnabled = true
                showSnackbar("Event updated successfully!")
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.updateEventButton.isEnabled = true
                showSnackbar("Failed to update event: ${e.message}")
            }
    }

    /**
     * Displays a confirmation dialog for deleting the event.
     *
     * This method shows a dialog with options to confirm or cancel the deletion of the event.
     * If confirmed, the event is deleted from Firebase.
     */
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ -> deleteEvent() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Deletes the event from Firebase.
     *
     * This method verifies that the current user is the creator of the event before deleting it.
     * It removes the event from all users' favorites and then deletes the event itself from
     * the Firebase Realtime Database.
     */
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

    /**
     * Displays a calendar dialog for selecting a date.
     *
     * This method initializes a `DatePickerDialog` with the current date or a previously set date
     * (if available in the provided `editText`). The selected date is formatted as `DD/MM/YYYY`
     * and set in the `editText`.
     *
     * @param editText The `TextInputEditText` where the selected date will be displayed.
     */
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

    /**
     * Converts a date string in `DD/MM/YYYY` format to a timestamp.
     *
     * This method parses the provided date string and converts it into a timestamp in milliseconds.
     * If the date string is invalid, it returns `-1L`.
     *
     * @param dateString The date string to convert.
     * @return The timestamp in milliseconds, or `-1L` if the conversion fails.
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
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Displays a snackbar with the provided message.
     *
     * This method shows a `Snackbar` at the bottom of the screen with the given message.
     *
     * @param message The message to display in the snackbar.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Checks for camera permission and opens the camera if granted.
     *
     * This method checks if the camera permission is granted. If not, it either shows a rationale
     * dialog or directly requests the permission. If the permission is granted, the camera is opened.
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

    /**
     * Checks for gallery permission and opens the gallery if granted.
     *
     * This method checks if the gallery permission is granted. If not, it either shows a rationale
     * dialog or directly requests the permission. If the permission is granted, the gallery is opened.
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
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog(
                    "Gallery Permission",
                    "Storage permission is needed to select images for your events.",
                    permission,
                    galleryPermissionLauncher
                )
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Displays a permission rationale dialog.
     *
     * This method shows a dialog explaining why a specific permission is needed. The user can
     * either grant the permission or cancel the request.
     *
     * @param title The title of the dialog.
     * @param message The message explaining why the permission is needed.
     * @param permission The permission being requested.
     * @param permissionLauncher The launcher to request the permission.
     */
    private fun showPermissionRationaleDialog(
        title: String,
        message: String,
        permission: String,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ ->
                permissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Opens the camera for taking a photo.
     *
     * This method creates a new image file in the media store and launches the camera app
     * to capture a photo. If the file creation fails, a snackbar is displayed.
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
     * Opens the gallery for selecting an image.
     *
     * This method launches the gallery app to allow the user to select an image.
     */
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
}