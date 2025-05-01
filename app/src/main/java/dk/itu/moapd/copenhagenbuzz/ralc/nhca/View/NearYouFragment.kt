package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel
import io.github.cdimascio.dotenv.dotenv
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NearYouFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NearYouFragment : Fragment() {
    private lateinit var eventAdapter: EventAdapter
    private val dataViewModel: DataViewModel by activityViewModels()

    // Reference coordinates (Copenhagen center as an example)
    private val referenceLatitude = 48.664070
    private val referenceLongitude = 7.666496

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
        return inflater.inflate(R.layout.fragment_nearyou, container, false)
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView: ListView = view.findViewById(R.id.event_list_view)
        val isLoggedIn = requireActivity().intent.getBooleanExtra("isLoggedIn", false)

        // Load environment variables
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }

        val databaseRef = Firebase.database(dotenv["DATABASE_URL"]).getReference("Events")

        // Retrieve all events and sort them by distance
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

                    // Sort events by distance to reference point
                    eventsList.sortBy { (_, event) ->
                        calculateDistance(
                            referenceLatitude, referenceLongitude,
                            event.eventLocation.latitude, event.eventLocation.longitude
                        )
                    }

                    // Create and set adapter with sorted events
                    eventAdapter = EventAdapter.createWithSortedEvents(
                        eventsList,
                        requireContext(),
                        isLoggedIn
                    )
                    listView.adapter = eventAdapter

                    // Observe favorite events
                    dataViewModel.favoriteEvents.observe(viewLifecycleOwner, Observer { favoriteEvents ->
                        (listView.adapter as? EventAdapter)?.setFavoriteEvents(favoriteEvents)
                    })
                } else {
                    Log.d("TimelineFragment", "No data found")
                    // Create empty adapter
                    eventAdapter = EventAdapter.create(databaseRef, requireContext(), isLoggedIn)
                    listView.adapter = eventAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TimelineFragment", "Database error: ${error.message}")
            }
        })
    }

    /**
     * Calculate distance between two geographic coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    companion object {
        /**
         * Factory method to create a new instance of this fragment.
         * @return A new instance of fragment CalendarFragment.
         */
        @JvmStatic
        fun newInstance() = NearYouFragment()
    }
}