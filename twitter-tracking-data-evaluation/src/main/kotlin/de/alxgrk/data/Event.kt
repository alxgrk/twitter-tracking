package de.alxgrk.data

import com.fasterxml.jackson.annotation.JsonInclude

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
}