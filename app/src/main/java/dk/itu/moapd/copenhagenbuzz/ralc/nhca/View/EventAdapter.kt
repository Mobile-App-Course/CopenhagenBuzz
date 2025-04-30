package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.View
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.FragmentActivity
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.EventRowItemBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter : BaseAdapter {
    private var favoriteEvents: List<Event> = emptyList()
    private val context: Context
    private val isLoggedIn: Boolean
    private class ViewHolder(val binding: EventRowItemBinding)

    // For Firebase query-based adapter
    private var firebaseAdapter: FirebaseListAdapter<Event>? = null

    // For custom sorted list-based adapter
    private var sortedEvents: List<Pair<String, Event>>? = null

    companion object {
        // Factory method to create the adapter with Firebase query
        fun create(query: Query, context: Context, isLoggedIn: Boolean): EventAdapter {
            val options = FirebaseListOptions.Builder<Event>()
                .setQuery(query, Event::class.java)
                .setLayout(R.layout.event_row_item)
                .setLifecycleOwner(context as androidx.lifecycle.LifecycleOwner)
                .build()

            return EventAdapter(options, context, isLoggedIn)
        }

        // Factory method to create the adapter with pre-sorted events
        fun createWithSortedEvents(
            sortedEvents: List<Pair<String, Event>>,
            context: Context,
            isLoggedIn: Boolean
        ): EventAdapter {
            return EventAdapter(sortedEvents, context, isLoggedIn)
        }
    }

    // Constructor for Firebase query-based adapter
    constructor(options: FirebaseListOptions<Event>, context: Context, isLoggedIn: Boolean) {
        this.context = context
        this.isLoggedIn = isLoggedIn
        this.firebaseAdapter = object : FirebaseListAdapter<Event>(options) {
            override fun populateView(view: View, event: Event, position: Int) {
                this@EventAdapter.populateView(view, event, getRef(position).key ?: "")
            }
        }
    }

    // Constructor for pre-sorted events list
    constructor(sortedEvents: List<Pair<String, Event>>, context: Context, isLoggedIn: Boolean) {
        this.context = context
        this.isLoggedIn = isLoggedIn
        this.sortedEvents = sortedEvents
    }

    override fun getCount(): Int {
        return sortedEvents?.size ?: firebaseAdapter?.count ?: 0
    }

    override fun getItem(position: Int): Event {
        return sortedEvents?.get(position)?.second ?: firebaseAdapter?.getItem(position) ?: Event()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: EventRowItemBinding
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.event_row_item, parent, false)

        if (view.tag == null) {
            binding = EventRowItemBinding.bind(view)
            view.tag = ViewHolder(binding)
        } else {
            binding = (view.tag as ViewHolder).binding
        }

        val event = getItem(position)
        val eventKey = sortedEvents?.get(position)?.first ?: ""

        populateView(view, event, eventKey)

        return view
    }

    private fun populateView(view: View, event: Event, eventKey: String) {
        val binding = if (view.tag == null) {
            val newBinding = EventRowItemBinding.bind(view)
            view.tag = ViewHolder(newBinding)
            newBinding
        } else {
            (view.tag as ViewHolder).binding
        }

        with(binding) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
            eventNameTextView.text = event.eventName
            // Format the timestamp into a readable date string
            val date = Date(event.eventDate)
            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            eventSubtitleTextView.text = context.getString(R.string.event_subtitle, formattedDate, event.eventLocation.address, event.eventType)

            eventDescriptionTextView.text = event.eventDescription

            // Get current user ID
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid

            // Get boolean to check if the current user is the creator of the event
            val isCreator = userId != null && userId == event.creatorUserId

            // Add click listener for the edit button
            editButton.setOnClickListener { v ->
                showEditDialog(event, eventKey)
            }

            // Conditionally render buttons
            buttonFavorite.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            buttonShare.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            // Show edit button only if the current user is the creator of the event
            editButton.visibility = if (isCreator) View.VISIBLE else View.GONE

            // Change favorite icon color if the event is in the favoriteEvents list
            if (favoriteEvents.contains(event)) {
                buttonFavorite.icon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                buttonFavorite.icon.clearColorFilter()
            }

            buttonFavorite.setOnClickListener { v ->
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid

                if (userId == null) {
                    Snackbar.make(v, "You need to be logged in to favorite events", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
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
                                buttonFavorite.icon.clearColorFilter()
                                Snackbar.make(v, "Removed from favorites", Snackbar.LENGTH_SHORT).show()
                            }
                    } else {
                        // Not favorited, so add (store true as the value under the eventKey)
                        favoritesRef.child(eventKey).setValue(true)
                            .addOnSuccessListener {
                                buttonFavorite.icon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
                                Snackbar.make(v, "Added to favorites", Snackbar.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(v, "Failed to add to favorites: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            }
                    }
                }
            }

            buttonShare.setOnClickListener { v ->
                Snackbar.make(v, "Event Shared!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    fun setFavoriteEvents(favoriteEvents: List<Event>) {
        this.favoriteEvents = favoriteEvents
        notifyDataSetChanged()
    }

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

    fun startListening() {
        firebaseAdapter?.startListening()
    }

    fun stopListening() {
        firebaseAdapter?.stopListening()
    }
}