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

/**
 * Adapter for managing and displaying a list of favorite events.
 *
 * This class is responsible for fetching favorite events from Firebase, keeping track of
 * the current list of events, and notifying listeners when the data changes. It uses
 * Firebase Realtime Database to retrieve and monitor changes to the user's favorite events.
 *
 * @property context The context in which the adapter is used.
 */
class FavoriteAdapter private constructor(private val context: Context) {

    // List to keep track of current events
    private val events = mutableListOf<Event>()

    // List of data change listeners
    private val dataChangedListeners = mutableListOf<DataChangedListener>()

    /**
     * Interface for data change callbacks.
     *
     * This interface is ued to receive updates when the list of favorite events changes.
     */
    interface DataChangedListener {
        /**
         * Called when the list of favorite events is updated.
         *
         * @param events The updated list of favorite events.
         */
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
        /**
         * Called when the data at the `Favorites` reference changes.
         *
         * This method clears the current list of events and fetches the updated list
         * of favorite events from the `Events` reference. Once all events are loaded,
         * it notifies the registered listeners.
         *
         * @param snapshot The snapshot of the `Favorites` data.
         */
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
                    /**
                     * Called when the data for a specific event is retrieved.
                     *
                     * Adds the event to the list and notifies listeners once all events are loaded.
                     *
                     * @param eventSnapshot The snapshot of the event data.
                     */
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

                    /**
                     * Called when the event data retrieval is canceled.
                     *
                     * Handles errors and ensures listeners are notified even if some events fail to load.
                     *
                     * @param error The error that occurred.
                     */
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

        /**
         * Called when the `Favorites` data retrieval is canceled.
         *
         * Handles errors and notifies listeners with the current list of events.
         *
         * @param error The error that occurred.
         */
        override fun onCancelled(error: DatabaseError) {
            // Handle error
            notifyDataChanged()
        }
    }

    /**
     * Starts listening for changes to the user's favorite events.
     *
     * Adds a `ValueEventListener` to the `Favorites` reference to monitor changes in real-time.
     */
    fun startListening() {
        favoritesRef.addValueEventListener(favoritesListener)
    }

    /**
     * Stops listening for changes to the user's favorite events.
     *
     * Removes the `ValueEventListener` from the `Favorites` reference.
     */
    fun stopListening() {
        favoritesRef.removeEventListener(favoritesListener)
    }

    /**
     * Adds a data change listener to receive updates when the list of favorite events changes.
     *
     * @param listener The listener to add.
     */
    fun addDataChangedListener(listener: DataChangedListener) {
        dataChangedListeners.add(listener)
        // Notify new listener of current data immediately
        listener.onDataChanged(events.toList())
    }

    /**
     * Removes a data change listener.
     *
     * @param listener The listener to remove.
     */
    fun removeDataChangedListener(listener: DataChangedListener) {
        dataChangedListeners.remove(listener)
    }

    /**
     * Notifies all registered listeners of data changes.
     *
     * Creates a copy of the current list of events to avoid concurrent modification issues.
     */
    private fun notifyDataChanged() {
        val eventsCopy = events.toList() // Create a copy to avoid concurrent modification
        dataChangedListeners.forEach { it.onDataChanged(eventsCopy) }
    }

    companion object {
        /**
         * Factory method to create an instance of `FavoriteAdapter`.
         *
         * @param context The context in which the adapter is used.
         * @return A new instance of `FavoriteAdapter`.
         */
        @JvmStatic
        fun create(context: Context): FavoriteAdapter {
            return FavoriteAdapter(context)
        }
    }
}