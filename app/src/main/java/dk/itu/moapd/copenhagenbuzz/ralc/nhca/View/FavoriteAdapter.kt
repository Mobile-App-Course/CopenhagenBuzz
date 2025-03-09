package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FavoriteRowItemBinding

class FavoriteAdapter(
    private val context: Context,
    private val favoriteEvents: List<Event>
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: FavoriteRowItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FavoriteRowItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = favoriteEvents[position]
        with(holder.binding) {
            Picasso.get().load(event.eventPhotoURL).into(eventPhotoImageView)
            eventNameTextView.text = event.eventName
            eventTypeTextView.text = event.eventType


        }
    }

    override fun getItemCount(): Int {
        return favoriteEvents.size
    }
}