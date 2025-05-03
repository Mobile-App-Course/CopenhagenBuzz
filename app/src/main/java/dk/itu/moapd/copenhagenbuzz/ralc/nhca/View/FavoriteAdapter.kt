package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.content.Context
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import io.github.cdimascio.dotenv.dotenv

class FavoriteAdapter private constructor(private val context: Context) {

    // List to keep track of current events
    private val events = mutableListOf<Event>()

    // List of data change listeners
    private val dataChangedListeners = mutableListOf<DataChangedListener>()

    // Interface for data change callbacks
    interface DataChangedListener {
        fun onDataChanged(events: List<Event>)
    }

    // Firebase references
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val dotenv = dotenv {
        directory = "./assets"
        filename = "env"
    }
    private val database = Firebase.database(dotenv["DATABASE_URL"])
    private val favoritesRef = database.reference.child("Favorites").child(userId)
    private val eventsRef = database.reference.child("Events")

    // ValueEventListener for favorites
    private val favoritesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            events.clear()

            // For each favorite, fetch the actual event data
            val totalFavorites = snapshot.childrenCount
            if (totalFavorites == 0L) {
                notifyDataChanged() // No favorites, notify with empty list
                return
            }

            var loadedCount = 0
            for (favoriteSnapshot in snapshot.children) {
                val eventId = favoriteSnapshot.key ?: continue

                eventsRef.child(eventId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(eventSnapshot: DataSnapshot) {
                        val event = eventSnapshot.getValue(Event::class.java)
                        if (event != null) {
                            events.add(event)
                        }

                        loadedCount++
                        if (loadedCount == totalFavorites.toInt()) {
                            // All events are loaded, notify listeners
                            notifyDataChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                        loadedCount++
                        if (loadedCount == totalFavorites.toInt()) {
                            notifyDataChanged()
                        }
                    }
                })
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle error
            notifyDataChanged()
        }
    }

    // Start listening for database changes
    fun startListening() {
        favoritesRef.addValueEventListener(favoritesListener)
    }

    // Stop listening for database changes
    fun stopListening() {
        favoritesRef.removeEventListener(favoritesListener)
    }

    // Add a data change listener
    fun addDataChangedListener(listener: DataChangedListener) {
        dataChangedListeners.add(listener)
        // Notify new listener of current data immediately
        listener.onDataChanged(events.toList())
    }

    // Remove a data change listener
    fun removeDataChangedListener(listener: DataChangedListener) {
        dataChangedListeners.remove(listener)
    }

    // Notify all listeners of data changes
    private fun notifyDataChanged() {
        val eventsCopy = events.toList() // Create a copy to avoid concurrent modification
        dataChangedListeners.forEach { it.onDataChanged(eventsCopy) }
    }

    companion object {
        @JvmStatic
        fun create(context: Context): FavoriteAdapter {
            return FavoriteAdapter(context)
        }
    }
}