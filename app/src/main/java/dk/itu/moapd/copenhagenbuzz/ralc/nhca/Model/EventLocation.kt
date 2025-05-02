package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.os.Parcelable
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import kotlinx.parcelize.Parcelize

@Parcelize
data class EventLocation(
    var latitude: Double,
    var longitude: Double,
    var address: String,
    var distance: Float = 0f  // Distance in meters from user's location
) : Parcelable {
    constructor() : this(0.0, 0.0, "", 0f)

    override fun toString(): String {
        return "EventLocation(latitude='$latitude', longitude='$longitude', address='$address', distance='$distance')"
    }
}