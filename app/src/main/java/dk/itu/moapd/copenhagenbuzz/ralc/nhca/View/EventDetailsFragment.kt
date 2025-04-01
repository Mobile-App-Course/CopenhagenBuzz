package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FragmentEventDetailsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventDetailsFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEventDetailsBinding
    private lateinit var event: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            event = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("event", Event::class.java) ?: Event()
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("event") ?: Event()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Formats date from a Unix Timestamp
        val date = Date(event.eventDate)
        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)

        // Populate UI with event data
        with(binding) {
            eventName.text = event.eventName
            eventDate.text = formattedDate
            eventLocation.text = event.eventLocation.address
            eventType.text = event.eventType
            eventDescription.text = event.eventDescription

            // Load image with Picasso
            if (event.eventPhotoURL.isNotEmpty()) {
                Picasso.get()
                    .load(event.eventPhotoURL)
                    .placeholder(R.drawable.baseline_firebase)
                    .into(eventPhoto)
            }

            // Add favorite button functionality
            favoriteButton.setOnClickListener {
                toggleFavorite()
            }

            // Add share button functionality
            shareButton.setOnClickListener {
                shareEvent()
            }
        }
    }

    private fun toggleFavorite() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            showSnackbar("Please log in to add favorites")
            return
        }

        val userId = currentUser.uid
        val database = FirebaseDatabase.getInstance()
        val favoritesRef = database.getReference("Favorites").child(userId)

        // Check if this event is already a favorite
        favoritesRef.child(event.eventName).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // Event is already favorited, so remove it
                removeFromFavorites(userId, favoritesRef)
            } else {
                // Event is not favorited, so add it
                addToFavorites(userId, favoritesRef)
            }
        }.addOnFailureListener { e ->
            showSnackbar("Error accessing favorites: ${e.message}")
        }
    }

    private fun addToFavorites(userId: String, favoritesRef: DatabaseReference) {
        favoritesRef.child(event.eventName).setValue(event)
            .addOnSuccessListener {
                showSnackbar("Added to favorites")
            }
            .addOnFailureListener { e ->
                showSnackbar("Failed to add to favorites: ${e.message}")
            }
    }

    private fun removeFromFavorites(userId: String, favoritesRef: DatabaseReference) {
        favoritesRef.child(event.eventName).removeValue()
            .addOnSuccessListener {
                showSnackbar("Removed from favorites")
            }
            .addOnFailureListener { e ->
                showSnackbar("Failed to remove event from favorites: ${e.message}")
            }
    }

    private fun shareEvent() {
        // Format date for sharing
        val date = Date(event.eventDate)
        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)

        // Create share text
        val shareText = """
            Check out this event in Copenhagen!
            
            ${event.eventName}
            Date: $formattedDate
            Location: ${event.eventLocation.address}
            Type: ${event.eventType}
            
            ${event.eventDescription}
        """.trimIndent()

        // Create share intent
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
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
}