package de.alxgrk.data

data class Session(
    val sessionStartEvent: Event,
    val sessionEndEvent: Event,
    val sessionEventsInChronologicalOrder: List<Event>
)
