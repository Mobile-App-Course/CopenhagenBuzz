package dk.itu.moapd.copenhagenbuzz.ralc.nhca.ViewModel

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.EventLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.net.Uri
import java.util.*
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.location.Geocoder
import io.github.cdimascio.dotenv.dotenv

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

    // Add event status
    private val _addEventStatus = MutableLiveData<Resource<Boolean>>()
    val addEventStatus: LiveData<Resource<Boolean>> = _addEventStatus

    // Upload status
    private val _uploadStatus = MutableLiveData<Resource<String>>()
    val uploadStatus: LiveData<Resource<String>> = _uploadStatus

    // Geocode status
    private val _geocodeStatus = MutableLiveData<Resource<EventLocation>>()
    val geocodeStatus: LiveData<Resource<EventLocation>> = _geocodeStatus



    /**
     * Updates the list of favorite events.
     *
     * @param newFavorites The new list of favorite events to store.
     */
    fun updateFavoriteEvents(newFavorites: List<Event>) {
        _favoriteEvents.value = newFavorites
    }

    /**
     * Uploads an image to Firebase Storage
     */
    fun uploadEventImage(imageUri: Uri) {
        _uploadStatus.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val photoRef = storageRef.child("event_photos/${UUID.randomUUID()}")

                // Use withContext to perform IO operations
                withContext(Dispatchers.IO) {
                    photoRef.putFile(imageUri).await()
                    val downloadUrl = photoRef.downloadUrl.await().toString()
                    _uploadStatus.postValue(Resource.Success(downloadUrl))
                }
            } catch (e: Exception) {
                _uploadStatus.postValue(Resource.Error("Upload failed: ${e.message}"))
            }
        }
    }

    /**
     * Geocodes a location address
     */
    fun geocodeLocation(locationText: String, geocoder: Geocoder) {
        _geocodeStatus.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val addressList = geocoder.getFromLocationName(locationText, 1)

                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    val formattedAddress = address.getAddressLine(0) ?: locationText
                    val eventLocation = EventLocation(
                        address.latitude,
                        address.longitude,
                        formattedAddress
                    )
                    _geocodeStatus.postValue(Resource.Success(eventLocation))
                } else {
                    _geocodeStatus.postValue(Resource.Error("Location not found"))
                }
            } catch (e: Exception) {
                _geocodeStatus.postValue(Resource.Error("Geocoding error: ${e.message}"))
            }
        }
    }

    /**
     * Saves event to Firebase
     */
    fun saveEventToFirebase(event: Event) {
        _addEventStatus.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val dotenv = dotenv {
                    directory = "./assets"
                    filename = "env"
                }

                val database = Firebase.database(dotenv["DATABASE_URL"])
                val eventsRef = database.getReference("Events")

                withContext(Dispatchers.IO) {
                    eventsRef.push().setValue(event).await()
                    _addEventStatus.postValue(Resource.Success(true))
                }
            } catch (e: Exception) {
                _addEventStatus.postValue(Resource.Error("Failed to save event: ${e.message}"))
            }
        }
    }
}

/**
 * Resource class to handle data states
 */
sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}