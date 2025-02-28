package dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.github.javafaker.Faker
import java.text.SimpleDateFormat
import java.util.*

class DataViewModel : ViewModel() {

    /*
    _events is a MutableLiveData object that holds a list of Event objects.
    events is a LiveData object that provides read-only access to the list of events.
    */
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>>
        get() = _events

    private val faker = Faker()
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

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
            val faker = Faker()
            List(10) { _ ->
                Event(
                    eventName = faker.name().toString(),
                    eventLocation = faker.address().cityName(),
                    eventPhotoURL = "https://picsum.photos/300/200?random=${System.currentTimeMillis()}",
                    eventDate = dateFormat.format(faker.date().birthday()),
                    eventType = faker.book().genre(),
                    eventDescription = faker.lorem().sentence(10)
                )
            }
        }
    }


}