package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel


/**
 * A simple [Fragment] subclass to display favorite events in a RecyclerView.
 * Use the [FavoritesFragment.newInstance] factory method to create an instance of this fragment.
 */
class FavoritesFragment : Fragment() {

    private lateinit var adapter: FavoriteAdapter

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
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored in to the view.
     * @param view The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.favorites_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Creates and set the Firebase adapter
        adapter = FavoriteAdapter.create(requireContext())
        recyclerView.adapter = adapter
    }

    companion object {
        /**
         * Factory method to create a new instance of this fragment.
         * @return A new instance of fragment FavoritesFragment.
         */
        @JvmStatic
        fun newInstance() = FavoritesFragment()
    }

    /**
     * Called when the Fragment is visible to the user and actively running.
     * Starts listening for data changes in the Firebase adapter.
     */
    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    /**
     * Called when the Fragment is no longer started.
     * Stops listening for data changes in the Firebase adapter.
     */
    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}