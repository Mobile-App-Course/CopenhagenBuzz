package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.graphics.BlendModeColorFilter
import android.view.View
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.EventRowItemBinding
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.NearbyEventRowItemBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying events in a list or grid view.
 *
 * This adapter supports two modes:
 * - Firebase query-based adapter for dynamically loading events from Firebase.
 * - Pre-sorted list-based adapter for displaying a static list of events.
 *
 * @property favoriteEventKeys A set of keys for favorite events to highlight in the UI.
 * @property context The context in which the adapter is used.
 * @property isLoggedIn A flag indicating whether the user is logged in.
 * @property layoutResId The resource ID of the layout used for each item.
 * @property auth The FirebaseAuth instance for user authentication.
 * @property firebaseAdapter The FirebaseListAdapter for query-based event loading.
 * @property sortedEvents A list of pre-sorted events with their keys.
 * @property locationAvailable Indicates if user location is available for distance calculation.
 */
class EventAdapter : BaseAdapter {
    private var favoriteEventKeys: MutableSet<String> = mutableSetOf()
    private var favoriteEvents: List<Event> = emptyList()
    private val context: Context
    private var isLoggedIn: Boolean
    private val layoutResId: Int
    private val auth = FirebaseAuth.getInstance()  // Add FirebaseAuth instance

    // Flag to indicate if user location is available
    private var locationAvailable: Boolean = true

    /**
     * ViewHolder class for holding view bindings.
     *
     * @property binding The view binding for the item layout.
     */
    private class ViewHolder(val binding: ViewBinding)

    // For Firebase query-based adapter
    private var firebaseAdapter: FirebaseListAdapter<Event>? = null

    // For custom sorted list-based adapter
    private var sortedEvents: List<Pair<String, Event>>? = null

    companion object {
        /**
         * Factory method to create the adapter with a Firebase query.
         *
         * @param query The Firebase query for loading events.
         * @param context The context in which the adapter is used.
         * @param isLoggedIn A flag indicating whether the user is logged in.
         * @return An instance of `EventAdapter`.
         */
        fun create(query: Query, context: Context, isLoggedIn: Boolean = false): EventAdapter {
            val options = FirebaseListOptions.Builder<Event>()
                .setQuery(query, Event::class.java)
                .setLayout(R.layout.event_row_item)
                .setLifecycleOwner(context as androidx.lifecycle.LifecycleOwner)
                .build()

            return EventAdapter(options, context, isLoggedIn, R.layout.event_row_item)
        }

        /**
         * Factory method to create the adapter with a pre-sorted list of events.
         *
         * @param sortedEvents A list of event keys and their corresponding events.
         * @param context The context in which the adapter is used.
         * @param isLoggedIn A flag indicating whether the user is logged in.
         * @param layoutResId The resource ID of the layout used for each item.
         * @return An instance of `EventAdapter`.
         */
        fun createWithSortedEvents(
            sortedEvents: List<Pair<String, Event>>,
            context: Context,
            isLoggedIn: Boolean = false,
            layoutResId: Int = R.layout.event_row_item
        ): EventAdapter {
            return EventAdapter(sortedEvents, context, isLoggedIn, layoutResId)
        }
    }

    /**
     * Constructor for Firebase query-based adapter.
     *
     * @param options The FirebaseListOptions for configuring the adapter.
     * @param context The context in which the adapter is used.
     * @param isLoggedIn A flag indicating whether the user is logged in.
     * @param layoutResId The resource ID of the layout used for each item.
     */
    constructor(options: FirebaseListOptions<Event>, context: Context, isLoggedIn: Boolean, layoutResId: Int) {
        this.context = context
        this.isLoggedIn = isLoggedIn
        this.layoutResId = layoutResId
        this.firebaseAdapter = object : FirebaseListAdapter<Event>(options) {
            override fun populateView(view: View, event: Event, position: Int) {
                this@EventAdapter.populateView(view, event, getRef(position).key ?: "")
            }
        }

        // Load favorites immediately if user is logged in
        if (isLoggedIn) {
            loadFavorites()
        }
    }

    /**
     * Constructor for pre-sorted events list.
     *
     * @param sortedEvents A list of event keys and their corresponding events.
     * @param context The context in which the adapter is used.
     * @param isLoggedIn A flag indicating whether the user is logged in.
     * @param layoutResId The resource ID of the layout used for each item.
     */
    constructor(sortedEvents: List<Pair<String, Event>>, context: Context, isLoggedIn: Boolean, layoutResId: Int) {
        this.context = context
        this.isLoggedIn = isLoggedIn
        this.sortedEvents = sortedEvents
        this.layoutResId = layoutResId

        // Load favorites immediately if user is logged in
        if (isLoggedIn) {
            loadFavorites()
        }
    }

    /**
     * Loads favorite events from Firebase for the current user.
     */
    private fun loadFavorites() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("Favorites")
            .child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    favoriteEventKeys.clear()

                    for (child in snapshot.children) {
                        val eventKey = child.key
                        if (eventKey != null) {
                            favoriteEventKeys.add(eventKey)
                        }
                    }

                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return The number of items.
     */
    override fun getCount(): Int {
        return sortedEvents?.size ?: firebaseAdapter?.count ?: 0
    }

    /**
     * Returns the event at the specified position.
     *
     * @param position The position of the item.
     * @return The event at the specified position.
     */
    override fun getItem(position: Int): Event {
        return sortedEvents?.get(position)?.second ?: firebaseAdapter?.getItem(position) ?: Event()
    }

    /**
     * Returns the ID of the item at the specified position.
     *
     * @param position The position of the item.
     * @return The ID of the item.
     */
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * Returns the view for the item at the specified position.
     *
     * @param position The position of the item.
     * @param convertView The recycled view, if available.
     * @param parent The parent view group.
     * @return The view for the item.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)
        val binding: ViewBinding

        if (view.tag == null) {
            binding = when (layoutResId) {
                R.layout.nearby_event_row_item -> NearbyEventRowItemBinding.bind(view)
                else -> EventRowItemBinding.bind(view)
            }
            view.tag = ViewHolder(binding)
        } else {
            binding = (view.tag as ViewHolder).binding
        }

        val event = getItem(position)
        val eventKey = getEventKeyAt(position)

        populateView(view, event, eventKey)

        return view
    }

    /**
     * Gets the event key at the specified position.
     *
     * @param position The position of the item.
     * @return The event key.
     */
    private fun getEventKeyAt(position: Int): String {
        return sortedEvents?.get(position)?.first ?:
        (firebaseAdapter?.getRef(position)?.key ?: "")
    }

    /**
     * Populates the view for an event.
     *
     * @param view The view to populate.
     * @param event The event to display.
     * @param eventKey The key of the event.
     */
    private fun populateView(view: View, event: Event, eventKey: String) {
        val binding = (view.tag as ViewHolder).binding

        when (binding) {
            is NearbyEventRowItemBinding -> {
                populateNearbyEventView(binding, event, eventKey)
            }
            is EventRowItemBinding -> {
                populateStandardEventView(binding, event, eventKey)
            }
        }
    }

    /**
     * Populates the standard event view.
     *
     * @param binding The binding for the standard event layout.
     * @param event The event to display.
     * @param eventKey The key of the event.
     */
    private fun populateStandardEventView(binding: EventRowItemBinding, event: Event, eventKey: String) {
        with(binding) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
            eventNameTextView.text = event.eventName

            // Format the timestamp into a readable date string
            val date = Date(event.eventDate)
            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            eventSubtitleTextView.text = context.getString(R.string.event_subtitle, formattedDate, event.eventLocation.address, event.eventType)

            eventDescriptionTextView.text = event.eventDescription

            // Get current user ID
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            // Get boolean to check if the current user is the creator of the event
            val isCreator = userId != null && userId == event.creatorUserId

            // Add click listener for the edit button
            editButton.setOnClickListener { v ->
                showEditDialog(event, eventKey)
            }

            // Conditionally render buttons
            buttonFavorite.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            // Show edit button only if the current user is the creator of the event
            editButton.visibility = if (isCreator) View.VISIBLE else View.GONE

            // Check if this event is favorited
            val isFavorite = favoriteEventKeys.contains(eventKey) || favoriteEvents.contains(event)

            // Update favorite icon color based on favorite status
            if (isFavorite) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    buttonFavorite.icon.colorFilter = BlendModeColorFilter(Color.RED, android.graphics.BlendMode.SRC_IN)
                }
            } else {
                buttonFavorite.icon.clearColorFilter()
            }

            buttonFavorite.setOnClickListener { v ->
                toggleFavorite(v, eventKey)
            }

            buttonShare.setOnClickListener { v ->
                Snackbar.make(v, "Event Shared!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Populates the nearby event view.
     *
     * @param binding The binding for the nearby event layout.
     * @param event The event to display.
     * @param eventKey The key of the event.
     */
    private fun populateNearbyEventView(binding: NearbyEventRowItemBinding, event: Event, eventKey: String) {
        with(binding) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
            eventNameTextView.text = event.eventName

            // Format the timestamp into a readable date string
            val date = Date(event.eventDate)
            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            eventSubtitleTextView.text = context.getString(R.string.event_subtitle, formattedDate, event.eventLocation.address, event.eventType)

            // Display distance based on availability and validity
            if (locationAvailable && event.eventLocation.distance >= 0) {
                // Format distance - convert meters to kilometers if distance is over 1000m
                val distance = event.eventLocation.distance
                val formattedDistance = when {
                    distance < 1000 -> String.format("%.0f m", distance)
                    else -> String.format("%.1f km", distance / 1000)
                }
                eventDistanceTextView.text = context.getString(R.string.event_distance, formattedDistance)
            } else {
                // Display N/A if location is not available or distance is invalid
                eventDistanceTextView.text = context.getString(R.string.event_distance, "N/A")
            }

            eventDescriptionTextView.text = event.eventDescription

            // Get current user ID
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            // Get boolean to check if the current user is the creator of the event
            val isCreator = userId != null && userId == event.creatorUserId

            // Add click listener for the edit button
            editButton.setOnClickListener { v ->
                showEditDialog(event, eventKey)
            }

            // Conditionally render buttons
            buttonFavorite.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            // Show edit button only if the current user is the creator of the event
            editButton.visibility = if (isCreator) View.VISIBLE else View.GONE

            // Check if this event is favorited
            val isFavorite = favoriteEventKeys.contains(eventKey) || favoriteEvents.contains(event)

            // Update favorite icon color based on favorite status
            if (isFavorite) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    buttonFavorite.icon.colorFilter = BlendModeColorFilter(Color.RED, android.graphics.BlendMode.SRC_IN)
                }
            } else {
                buttonFavorite.icon.clearColorFilter()
            }

            buttonFavorite.setOnClickListener { v ->
                toggleFavorite(v, eventKey)
            }

            buttonShare.setOnClickListener { v ->
                Snackbar.make(v, "Event Shared!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets whether location is available for distance calculation.
     * When set to false, distances will be displayed as "N/A".
     *
     * @param available True if location is available, false otherwise.
     */
    fun setLocationAvailable(available: Boolean) {
        this.locationAvailable = available
        notifyDataSetChanged()
    }

    /**
     * Toggles the favorite status of an event.
     *
     * @param v The view that triggered the action.
     * @param eventKey The key of the event.
     */
    private fun toggleFavorite(v: View, eventKey: String) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId == null) {
            Snackbar.make(v, "You need to be logged in to favorite events", Snackbar.LENGTH_SHORT).show()
            return
        }

        val favoritesRef = FirebaseDatabase.getInstance().reference
            .child("Favorites")
            .child(userId)

        // Toggle favorite status
        favoritesRef.child(eventKey).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Already favorited, so remove
                favoritesRef.child(eventKey).removeValue()
                    .addOnSuccessListener {
                        Snackbar.make(v, "Removed from favorites", Snackbar.LENGTH_SHORT).show()
                        // We don't need to call notifyDataSetChanged() because the ValueEventListener will handle it
                    }
            } else {
                // Not favorited, so add (store true as the value under the eventKey)
                favoritesRef.child(eventKey).setValue(true)
                    .addOnSuccessListener {
                        Snackbar.make(v, "Added to favorites", Snackbar.LENGTH_SHORT).show()
                        // We don't need to call notifyDataSetChanged() because the ValueEventListener will handle it
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(v, "Failed to add to favorites: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Sets the list of favorite events.
     * This method is kept for backward compatibility.
     *
     * @param favoriteEvents The list of favorite events.
     */
    fun setFavoriteEvents(favoriteEvents: List<Event>) {
        this.favoriteEvents = favoriteEvents
        notifyDataSetChanged()
    }

    /**
     * Displays the edit dialog for an event.
     *
     * @param event The event to edit.
     * @param eventKey The key of the event.
     */
    private fun showEditDialog(event: Event, eventKey: String) {
        val bundle = Bundle().apply {
            putParcelable("event", event)
            putString("eventKey", eventKey)
        }

        val editEventFragment = EditEventFragment().apply {
            arguments = bundle
        }

        editEventFragment.show((context as FragmentActivity).supportFragmentManager, "editEventFragment")
    }

    /**
     * Starts listening for Firebase data changes.
     */
    fun startListening() {
        firebaseAdapter?.startListening()
    }

    /**
     * Stops listening for Firebase data changes.
     */
    fun stopListening() {
        firebaseAdapter?.stopListening()
    }
}