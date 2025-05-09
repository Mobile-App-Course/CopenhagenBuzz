package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import coil.compose.rememberAsyncImagePainter
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel.DataViewModel


/**
 * A simple [Fragment] subclass to display favorite events in a RecyclerView.
 * This fragment uses Jetpack Compose to render the UI and integrates with a Firebase-based adapter
 * to fetch and display the user's favorite events.
 * Use the [FavoritesFragment.newInstance] factory method to create an instance of this fragment.
 */
class FavoritesFragment : Fragment() {

    private lateinit var adapter: FavoriteAdapter
    private lateinit var viewModel: DataViewModel

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
        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(DataViewModel::class.java)

        // Creates and sets the adapter
        adapter = FavoriteAdapter.create(requireContext())

        // Setup the adapter as a data source for the ViewModel
        setupAdapterWithViewModel()

        // Return a ComposeView (which replaced the inflation of the XML layout we did before)
        return ComposeView(requireContext()).apply {
            // When fragment is stopped, the composition will still be active
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        FavoritesScreen(viewModel)
                    }
                }
            }
        }
    }

    /**
     * Sets up the adapter to update the ViewModel when data changes
     */
    private fun setupAdapterWithViewModel() {
        adapter.addDataChangedListener(object : FavoriteAdapter.DataChangedListener {
            override fun onDataChanged(events: List<Event>) {
                viewModel.updateFavoriteEvents(events)
            }
        })
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

/**
 * Composable function to display the list of favorite events.
 *
 * @param viewModel The ViewModel used to fetch and manage the list of favorite events.
 */
@Composable
fun FavoritesScreen(viewModel: DataViewModel) {
    // Observe favorite events from the ViewModel
    val favoriteEvents = viewModel.favoriteEvents.observeAsState(listOf())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(favoriteEvents.value) { event ->
            FavoriteEventCard(event)
        }
    }
}

/**
 * Composable function to display a card for a single favorite event.
 *
 * @param event The event to display in the card.
 */
@Composable
fun FavoriteEventCard(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(8.dp),
        elevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.eventType,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Image(
                painter = rememberAsyncImagePainter(model = event.eventPhotoURL),
                contentDescription = "Event Photo",
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
    }
}