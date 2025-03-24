package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.squareup.picasso.Picasso
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.FavoriteRowItemBinding
import io.github.cdimascio.dotenv.dotenv

class FavoriteAdapter(
    options: FirebaseRecyclerOptions<Event>,
    private val context: Context,
) : FirebaseRecyclerAdapter<Event, FavoriteAdapter.ViewHolder>(options) {

    inner class ViewHolder(val binding: FavoriteRowItemBinding): RecyclerView.ViewHolder(binding.root)

    companion object{
        fun create(context: Context): FavoriteAdapter {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            // Load environment variables
            val dotenv = dotenv {
                directory = "./assets"
                filename = "env"
            }

            // Create query for this user's favorites
            val databaseRef = Firebase.database(dotenv["DATABASE_URL"])
                .reference.child("favorites").child(userId)

            // Configure FirebaseRecyclerOptions
            val options = FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(databaseRef, Event::class.java)
                .build()

            return FavoriteAdapter(options, context)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FavoriteRowItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Event) {
        with(holder.binding) {
            eventNameTextView.text = model.eventName
            eventTypeTextView.text = model.eventType

            // Picasso used to load image
            Picasso.get()
                .load(model.eventPhotoURL)
                .fit()
                .centerCrop()
                .into(eventPhotoImageView)
        }
    }
}