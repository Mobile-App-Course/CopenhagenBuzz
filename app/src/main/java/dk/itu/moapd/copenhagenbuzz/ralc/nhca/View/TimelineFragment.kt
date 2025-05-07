package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.activityViewModels
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel
import androidx.lifecycle.Observer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.cdimascio.dotenv.dotenv
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import com.google.firebase.auth.FirebaseAuth

/**
 * A simple [Fragment] subclass that displays a timeline of events.
 */
class TimelineFragment : Fragment() {

    private lateinit var eventAdapter: EventAdapter
    private val dataViewModel: DataViewModel by activityViewModels()
    private lateinit var listView: ListView
    private var isLoggedIn: Boolean = false

    /**
     * Called to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.event_list_view)
        val currentUser = FirebaseAuth.getInstance().currentUser
        isLoggedIn = currentUser != null && !currentUser.isAnonymous

        // Load data on first creation
        loadEventsData()

        // Observe favorite events
        dataViewModel.favoriteEvents.observe(viewLifecycleOwner, Observer { favoriteEvents ->
            (listView.adapter as? EventAdapter)?.setFavoriteEvents(favoriteEvents)
        })
    }

    /**
     * Load events from Firebase and setup the adapter
     */
    private fun loadEventsData() {
        // Load environment variables
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }

        val databaseRef = Firebase.database(dotenv["DATABASE_URL"]).getReference("Events")

        // Retrieve all events and sort them by date
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("TimelineFragment", "Data retrieved: ${snapshot.childrenCount} events")

                    // Create a list to hold events with their keys
                    val eventsList = mutableListOf<Pair<String, Event>>()

                    // Populate the list
                    for (eventSnapshot in snapshot.children) {
                        val event = eventSnapshot.getValue(Event::class.java)
                        val key = eventSnapshot.key
                        if (event != null && key != null) {
                            eventsList.add(Pair(key, event))
                            Log.d("TimelineFragment", "Event: $event")
                        }
                    }

                    // Sort events by date
                    eventsList.sortBy { (_, event) -> event.eventDate }

                    // Create and set adapter with sorted events, using the standard event_row_item layout
                    eventAdapter = EventAdapter.createWithSortedEvents(
                        eventsList,
                        requireContext(),
                        isLoggedIn,
                        R.layout.event_row_item  // Explicitly use the standard layout
                    )
                    listView.adapter = eventAdapter

                    // Update favorites if we have them
                    dataViewModel.favoriteEvents.value?.let { favorites ->
                        eventAdapter.setFavoriteEvents(favorites)
                    }
                } else {
                    Log.d("TimelineFragment", "No data found")
                    // Create empty adapter
                    eventAdapter = EventAdapter.createWithSortedEvents(
                        emptyList(),
                        requireContext(),
                        isLoggedIn,
                        R.layout.event_row_item  // Explicitly use the standard layout
                    )
                    listView.adapter = eventAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TimelineFragment", "Database error: ${error.message}")
            }
        })
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this fragment
        if (::listView.isInitialized) {
            loadEventsData()
        }
    }

    companion object {
        /**
         * Factory method to create a new instance of this fragment.
         * @return A new instance of fragment TimelineFragment.
         */
        @JvmStatic
        fun newInstance() = TimelineFragment()
    }
}