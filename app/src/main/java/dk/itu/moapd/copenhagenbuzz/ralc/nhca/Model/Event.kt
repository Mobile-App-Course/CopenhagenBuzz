package dk.itu.moapd.copenhagenbuzz.ralc.nhca.Model

data class Event (var eventName: String,
                   var eventLocation: String,
                   var eventDate: String,
                   var eventType: String,
                   var eventDescription: String) {

    fun getEventName(): String {
        return eventName
    }

    fun setEventName(eventName: String) {
        this.eventName = eventName
    }

    fun getEventLocation(): String {
        return eventLocation
    }

    fun setEventLocation(eventLocation: String) {
        this.eventLocation = eventLocation
    }

    fun getEventDate(): String {
        return eventDate
    }

    fun setEventDate(eventDate: String) {
        this.eventDate = eventDate
    }

    fun getEventType(): String {
        return eventType
    }

    fun setEventType(eventType: String) {
        this.eventType = eventType
    }

    fun getEventDescription(): String {
        return eventDescription
    }

    fun setEventDescription(eventDescription: String) {
        this.eventDescription = eventDescription
    }

// Implement the missing accessors and mutators methods.

    override fun toString(): String {
        return "Event(eventName='$eventName', eventLocation='$eventLocation', eventDate='$eventDate', eventType='$eventType', eventDescription='$eventDescription')"
    }

}