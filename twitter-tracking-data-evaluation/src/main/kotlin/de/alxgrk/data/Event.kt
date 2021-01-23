package de.alxgrk.data

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Event(
    val eventType: EventType,
    val userId: String,
    val action: String,
    val timestamp: String,
    val target: String? = null,
    val selector: String? = null,
    val scrollPosition: Int? = null,
    val estimatedTweetsScrolled: Int? = null
) {
    enum class EventType {
        BROWSER,
        ANDROID
    }

    fun zonedTimestamp() =
        if (timestamp.endsWith("Z", ignoreCase = false))
            ZonedDateTime.parse(timestamp)
        else
            ZonedDateTime.parse(timestamp + "Z")
}
