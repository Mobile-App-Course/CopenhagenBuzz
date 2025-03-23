package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.View
import android.graphics.Color
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.Query
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.EventRowItemBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    options: FirebaseListOptions<Event>,
    private val context: Context,
    private val isLoggedIn: Boolean
) : FirebaseListAdapter<Event>(options) {

    private var favoriteEvents: List<Event> = emptyList()
    private class ViewHolder(val binding: EventRowItemBinding)

    companion object {
        // Factory method to create the adapter with required FirebaseListOptions
        fun create(query: Query, context: Context, isLoggedIn: Boolean): EventAdapter {
            val options = FirebaseListOptions.Builder<Event>()
                .setQuery(query, Event::class.java)
                .setLayout(R.layout.event_row_item)
                .setLifecycleOwner(context as androidx.lifecycle.LifecycleOwner)
                .build()

            return EventAdapter(options, context, isLoggedIn)
        }
    }

    override fun populateView(view: View, event: Event, position: Int) {
        val binding: EventRowItemBinding

        if (view.tag == null) {
            binding = EventRowItemBinding.bind(view)
            view.tag = ViewHolder(binding)
        } else {
            binding = (view.tag as ViewHolder).binding
        }

        // Directly populate the view here instead of calling another method
        with(binding) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
            eventNameTextView.text = event.eventName
            // Format the timestamp into a readable date string
            val date = Date(event.eventDate)
            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            eventSubtitleTextView.text = context.getString(R.string.event_subtitle, formattedDate, event.eventLocation, event.eventType)


            eventDescriptionTextView.text = event.eventDescription

            // Conditionally render buttons
            buttonFavorite.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            buttonShare.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            editButton.visibility = if (isLoggedIn) View.VISIBLE else View.GONE

            // Change favorite icon color if the event is in the favoriteEvents list
            if (favoriteEvents.contains(event)) {
                buttonFavorite.icon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                buttonFavorite.icon.clearColorFilter()
            }

            buttonFavorite.setOnClickListener { v ->
                Snackbar.make(v, "Event Favorite!", Snackbar.LENGTH_SHORT).show()
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
}