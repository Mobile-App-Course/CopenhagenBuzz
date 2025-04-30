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

    // Activity result launcher for image selection
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

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            showSnackbar("Permission denied")
        }
    }

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

        // Add text change listener for location field
        setupLocationListener()

        // Setup image click listener
        setupImageClickListener()

        // Load existing image if available
        loadExistingImageIfAvailable()
    }

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

    private fun setupImageClickListener() {
        binding.eventImageCard.setOnClickListener {
            showImagePickerOptions()
        }
    }

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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
            .error(R.drawable.ic_image_error) // Add an error drawable
            .into(binding.eventImageView)
    }

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

    private fun uploadPhotoAndSaveEvent() {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef = storageRef.child("event_photos/${UUID.randomUUID()}")

        photoRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    saveEventWithPhotoUrl(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                binding.updateEventButton.isEnabled = true
                showSnackbar("Failed to upload image: ${e.message}")
            }
    }

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

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
}