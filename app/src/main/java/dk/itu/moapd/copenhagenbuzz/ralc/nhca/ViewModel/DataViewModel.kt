package dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DataViewModel : ViewModel() {

    /*
    _events is a MutableLiveData object that holds a list of Event objects.
    events is a LiveData object that provides read-only access to the list of events.
    */
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>>
        get() = _events


    init {
        fetchEvents()
    }

    /**
     * Generates and fetches the list of events asynchronously.
     */
    private fun fetchEvents() {
        viewModelScope.launch {
            val eventList = generateEvents()
            _events.value = eventList
        }
    }

    /**
     * Generates a list of events.
     *
     * @return A list of Event objects.
     */
    private suspend fun generateEvents(): List<Event> {
        return withContext(Dispatchers.Default) {
            // Simulate generating a list of events
            List(10) { index ->
                Event(
                    eventName = "Event $index",
                    eventLocation = "Location $index",
                    eventPhotoURL = "http://example.com/photo$index.jpg",
                    eventDate = "2025-12-0${index + 1}",
                    eventType = "Type $index",
                    eventDescription = "Description for event $index"
                )
            }
        }
    }


}