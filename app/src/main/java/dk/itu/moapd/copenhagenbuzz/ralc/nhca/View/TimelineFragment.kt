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

/**
 * A simple [Fragment] subclass that displays a timeline of events.
 */
class TimelineFragment : Fragment() {

    private lateinit var eventAdapter: EventAdapter
    private val dataViewModel: DataViewModel by activityViewModels()

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

        val listView: ListView = view.findViewById(R.id.event_list_view)
        val isLoggedIn = requireActivity().intent.getBooleanExtra("isLoggedIn", false)

        // Load environment variables
        val dotenv = dotenv {
            directory = "./assets"  // Change from "/assets" to "./assets"
            filename = "env"
        }


        val databaseRef = Firebase.database(dotenv["DATABASE_URL"]).getReference("Events")
        val query = databaseRef.orderByChild("eventDate")

        // Used to log if there is data retrieval or not
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("TimelineFragment", "Data retrieved: ${snapshot.childrenCount} events")
                    for (eventSnapshot in snapshot.children) {
                        val event = eventSnapshot.getValue(Event::class.java)
                        Log.d("TimelineFragment", "Event: $event")
                    }
                } else {
                    Log.d("TimelineFragment", "No data found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TimelineFragment", "Database error: ${error.message}")
            }
        })

        eventAdapter = EventAdapter.create(query, requireContext(), isLoggedIn)
        listView.adapter = eventAdapter

        dataViewModel.favoriteEvents.observe(viewLifecycleOwner, Observer { favoriteEvents ->
            (listView.adapter as? EventAdapter)?.setFavoriteEvents(favoriteEvents)
        })

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