package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.`MaterialButton$InspectionCompanion`
import com.google.android.material.snackbar.Snackbar
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import com.squareup.picasso.Picasso

/**
 * An adapter that provides views for displaying Event data in a ListView.
 *
 * @param context The context in which the adapter is running.
 * @param resource The resource ID for a layout file containing a layout to use when instantiating views.
 * @param data The list of Event objects to represent in the ListView.
 */
class EventAdapter(private val context: Context, private var resource: Int, data: List<Event>) :
    ArrayAdapter<Event>(context, R.layout.event_row_item, data) {

    /**
     * A ViewHolder class that holds references to the views for each data item.
     *
     * @constructor Creates a ViewHolder and initializes the views.
     * @param view The view containing the views to be initialized.
     */
    private class ViewHolder(view: View) {
        val eventName: TextView = view.findViewById(R.id.event_name_text_view)
        val eventPhoto: ImageView = view.findViewById(R.id.event_photo_image_view)
        val eventSubtitle: TextView = view.findViewById(R.id.event_subtitle_text_view)
        val eventDescription: TextView = view.findViewById(R.id.event_description_text_view)
        val likeButton: MaterialButton = view.findViewById(R.id.button_thumb_up)
        val favoriteButton: MaterialButton = view.findViewById(R.id.button_favorite)
        val shareButton: MaterialButton = view.findViewById(R.id.button_share)
    }

    /**
     * Returns a view for the specified position in the data set.
     *
     * @param position The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A view corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val viewHolder = (view.tag as? ViewHolder) ?: ViewHolder(view)

        getItem(position)?.let { event ->
            populateViewHolder(viewHolder, event)
        }

        return view
    }

    /**
     * Populates the ViewHolder with data from the given Event object.
     *
     * @param viewHolder The ViewHolder containing the views to be populated.
     * @param event The Event object containing the data to populate the views.
     */
    private fun populateViewHolder(viewHolder: ViewHolder, event: Event) {
        with(viewHolder) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhoto)
            eventName.text = event.eventName
            eventSubtitle.text = "Date: ${event.eventDate}\nLocation: ${event.eventLocation}\nType: ${event.eventType}"
            eventDescription.text = event.eventDescription

            // Like button listener
            likeButton.setOnClickListener { view ->
                Snackbar.make(view, "Event Liked!", Snackbar.LENGTH_SHORT).show()
            }

            // Favorite button listener
            favoriteButton.setOnClickListener { view ->
                Snackbar.make(view, "Event Favorite!", Snackbar.LENGTH_SHORT).show()
            }

            // Share button listener
            shareButton.setOnClickListener { view ->
                Snackbar.make(view, "Event Shared!", Snackbar.LENGTH_SHORT).show()
            }

        }
    }
}