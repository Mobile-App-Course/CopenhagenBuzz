/*
MIT License

Copyright (c) [2025] [Rasmus Alexander Christiansen, Nikolaj Heuer Løjmand Carlson]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Data class representing an event.
 *
 * @property eventName The name of the event.
 * @property eventLocation The location where the event is held.
 * @property eventPhotoURL The URL of the photo of the event.
 * @property eventDate The date when the event takes place.
 * @property eventType The category of the event.
 * @property eventDescription A brief description of the event.
 */
@Parcelize
data class Event(
    var creatorUserId: String,
    var eventName: String,
    var eventLocation: EventLocation,
    var eventPhotoURL: String,
    var eventDate: Long,
    var eventType: String,
    var eventDescription: String
) : Parcelable {
    constructor() : this("", "", EventLocation(), "", 0, "", "")

    override fun toString(): String {
        return "Event(eventName='$eventName', eventLocation='$eventLocation', eventPhotoURL='$eventPhotoURL', eventDate='$eventDate', eventType='$eventType', eventDescription='$eventDescription')"
    }
}