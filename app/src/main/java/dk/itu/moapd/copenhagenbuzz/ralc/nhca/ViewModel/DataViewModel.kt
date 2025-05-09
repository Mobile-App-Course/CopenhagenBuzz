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
    private val _events = MutableLiveData<List<Event>>(emptyList())
    val events: LiveData<List<Event>>
        get() = _events

    private val _favoriteEvents = MutableLiveData<List<Event>>(emptyList())
    val favoriteEvents: LiveData<List<Event>>
        get() = _favoriteEvents

    /**
     * Updates the list of favorite events.
     *
     * @param newFavorites The new list of favorite events to store.
     */
    fun updateFavoriteEvents(newFavorites: List<Event>) {
        _favoriteEvents.value = newFavorites
    }
}