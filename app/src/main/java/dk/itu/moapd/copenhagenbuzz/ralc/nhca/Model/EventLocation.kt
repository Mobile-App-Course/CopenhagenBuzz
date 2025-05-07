package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.os.Parcelable
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import kotlinx.parcelize.Parcelize

/**
 * Represents a location associated with an event.
 *
 * This data class is used to store information about an event's location, including its
 * latitude, longitude, address, and distance from the user's current location. It implements
 * the `Parcelable` interface to allow it to be passed between Android components.
 *
 * @property latitude The latitude of the event location.
 * @property longitude The longitude of the event location.
 * @property address The address of the event location.
 * @property distance The distance in meters from the user's location to the event location. Defaults to 0f.
 */
@Parcelize
data class EventLocation(
    var latitude: Double,
    var longitude: Double,
    var address: String,
    var distance: Float = 0f  // Distance in meters from user's location
) : Parcelable {

    /**
     * Secondary constructor that initializes the properties with default values.
     */
    constructor() : this(0.0, 0.0, "", 0f)

    /**
     * Returns a string representation of the `EventLocation` object.
     *
     * @return A string containing the latitude, longitude, address, and distance of the event location.
     */
    override fun toString(): String {
        return "EventLocation(latitude='$latitude', longitude='$longitude', address='$address', distance='$distance')"
    }
}