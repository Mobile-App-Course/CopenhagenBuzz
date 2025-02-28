package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import com.squareup.picasso.Picasso

class EventAdapter(private val context: Context, private var resource: Int, data: List<Event>) :
    ArrayAdapter<Event>(context, R.layout.event_row_item, data) {

    private class ViewHolder(view: View) {
        val eventName = view.findViewById<TextView>(R.id.text_field_event_name)
        val eventLocation = view.findViewById<TextView>(R.id.edit_text_event_location)
        val eventPhoto = view.findViewById<ImageView>(R.id.text_field_event_photo_url)
        val eventDate = view.findViewById<TextView>(R.id.text_field_event_date)
        val eventType = view.findViewById<TextView>(R.id.text_field_event_type)
        val eventDescription = view.findViewById<TextView>(R.id.text_field_event_description)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val viewHolder = (view.tag as? ViewHolder) ?: ViewHolder(view)

        getItem(position)?.let { event ->
            populateViewHolder(viewHolder, event)
        }

        return view
    }

    private fun populateViewHolder(viewHolder: ViewHolder, event: Event) {
        with(viewHolder) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhoto)
            eventName.text = event.eventName
            eventLocation.text = event.eventLocation
            eventDate.text = event.eventDate
            eventType.text = event.eventType
            eventDescription.text = event.eventDescription

            // set like button listener
            
        }
    }
}