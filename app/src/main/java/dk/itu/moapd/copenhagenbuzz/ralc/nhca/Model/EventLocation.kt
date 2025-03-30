package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.os.Parcelable
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model.Event
import kotlinx.parcelize.Parcelize

@Parcelize
data class EventLocation(
    var latitude: Double,
    var longitude: Double,
    var address: String
) : Parcelable {
    constructor() : this(0.0, 0.0, "")

    override fun toString(): String {
        return "EventLocation(latitude='$latitude', longitude='$longitude', address='$address')"
    }
}