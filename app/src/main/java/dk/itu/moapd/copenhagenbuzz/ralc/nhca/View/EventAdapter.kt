package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

 import android.content.Context
 import android.content.Intent
 import android.view.LayoutInflater
 import android.view.View
 import android.view.ViewGroup
 import android.widget.ArrayAdapter
 import com.google.android.material.snackbar.Snackbar
 import com.squareup.picasso.Picasso
 import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
 import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
 import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.EventRowItemBinding
 import android.graphics.Color

class EventAdapter(
    private val context: Context,
    private var resource: Int,
    data: List<Event>,
    private val isLoggedIn : Boolean
) : ArrayAdapter<Event>(context, R.layout.event_row_item, data) {

     private var favoriteEvents: List<Event> = emptyList()
     private class ViewHolder(val binding: EventRowItemBinding)

     override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
         val binding: EventRowItemBinding
         val viewHolder: ViewHolder

         if (convertView == null) {
             binding = EventRowItemBinding.inflate(LayoutInflater.from(context), parent, false)
             viewHolder = ViewHolder(binding)
             binding.root.tag = viewHolder
         } else {
             binding = (convertView.tag as ViewHolder).binding
             viewHolder = convertView.tag as ViewHolder
         }

         getItem(position)?.let { event ->
             populateViewHolder(viewHolder, event)
         }

         return binding.root
     }

     private fun populateViewHolder(viewHolder: ViewHolder, event: Event) {
         with(viewHolder.binding) {
             Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
             eventNameTextView.text = event.eventName
             eventSubtitleTextView.text = context.getString(R.string.event_subtitle, event.eventDate, event.eventLocation, event.eventType)
             eventDescriptionTextView.text = event.eventDescription

             // Conditionally render buttons
             buttonFavorite.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
             buttonShare.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
             editButton.visibility = if (isLoggedIn) View.VISIBLE else View.GONE

             // Change favorite icon color if the event is in the favoriteEvents list
             // CHANGE SO THAT THE EVENTS THAT ARE NOT FAVORITES USE THE FAVORITE ICON THAT IS NOT OUTLINED,
             // AND NOT THE TIMELINE ICON
             if (favoriteEvents.contains(event)) {
                 buttonFavorite.icon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
             } else {
                 buttonFavorite.icon.clearColorFilter()
             }

             buttonFavorite.setOnClickListener { view ->
                 Snackbar.make(view, "Event Favorite!", Snackbar.LENGTH_SHORT).show()
             }

             buttonShare.setOnClickListener { view ->
                 Snackbar.make(view, "Event Shared!", Snackbar.LENGTH_SHORT).show()
             }
         }
     }

    fun setFavoriteEvents(favoriteEvents: List<Event>) {
        this.favoriteEvents = favoriteEvents
        notifyDataSetChanged()
    }
 }