package de.alxgrk.data

import java.time.temporal.ChronoUnit

data class Session(
    val sessionStartEvent: Event,
    val sessionEndEvent: Event,
    val sessionEventsInChronologicalOrder: List<Event>
) {
    enum class SessionType {
        SCROLLING,
        POSTING,
        FOLLOW
    }

    fun sessionTypes(): List<SessionType> {
        val sessionTypes = mutableListOf<SessionType>()
        if (sessionEventsInChronologicalOrder.any { it.target == "posting" })
            sessionTypes.add(SessionType.POSTING)
        if (sessionEventsInChronologicalOrder.any { it.target == "followByTweet" || it.target == "follow" })
            sessionTypes.add(SessionType.FOLLOW)
        if (sessionEventsInChronologicalOrder.none { it.target == "posting" || it.target == "followByTweet" || it.target == "follow" })
            sessionTypes.add(SessionType.SCROLLING)
        return sessionTypes
    }
}

fun Session.durationInSeconds(): Long = ChronoUnit.SECONDS.between(
    sessionStartEvent.zonedTimestamp(),
    sessionEndEvent.zonedTimestamp()
)

fun List<Session>.sessionTypes() = this
    .flatMap { session -> session.sessionTypes() }
    .groupingBy { it }
    .eachCount()

fun List<Session>.fractionOfSessionTypes() = sessionTypes()
    .mapValues { it.value / this.size.toDouble() }
