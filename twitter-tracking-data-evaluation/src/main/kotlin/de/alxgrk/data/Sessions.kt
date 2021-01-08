package de.alxgrk.data

import io.inbot.eskotlinwrapper.AsyncIndexRepository
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toCollection
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.builder.SearchSourceBuilder.searchSource
import org.elasticsearch.search.sort.SortBuilders.fieldSort
import java.time.temporal.ChronoUnit

class Sessions(private val repo: AsyncIndexRepository<Event>) {

    suspend fun Analyse.extract(userId: UserId): Pair<List<Session>, List<Event>> {
        val result = repo.search(scrolling = true) {
            source(
                searchSource()
                    .size(10_000)
                    .sort(
                        fieldSort("timestamp")
                    )
                    .query(
                        termQuery("user_id", userId.id)
                    )
            )
        }

        val ignoredEvents: MutableList<Event> = mutableListOf()

        var lastStartEvent: Event? = null
        var lastEndEvent: Event? = null
        var sessionEvents: MutableList<Event> = mutableListOf()
        val sessions = result.mappedHits
            .mapNotNull { event ->
                when (event.action) {
                    "session_start" -> {

                        tryToCreateSession(lastStartEvent, lastEndEvent, sessionEvents, ignoredEvents).also {
                            lastStartEvent = event
                            lastEndEvent = null
                            sessionEvents = mutableListOf()
                        }
                    }
                    "session_end" -> {
                        val latest = sessionEvents.lastOrNull() ?: lastStartEvent ?: return@mapNotNull null

                        // only consider event as part of session, if there was no break longer than 1 hour
                        if (ChronoUnit.HOURS.between(
                                latest.zonedTimestamp(),
                                event.zonedTimestamp()
                            ) < 1
                        ) {
                            lastEndEvent = event
                            null
                        } else {
                            debug("WARN: creating new session with event of action '${event.action}' because timeframe between latest and current event is too large")

                            tryToCreateSession(
                                lastStartEvent,
                                latest,
                                sessionEvents,
                                ignoredEvents
                            ).also {
                                lastStartEvent = event
                                lastEndEvent = event
                                sessionEvents = mutableListOf()
                            }
                        }
                    }
                    else -> {
                        if (lastStartEvent != null && lastEndEvent != null) {
                            debug("WARN: creating new session with event of action '${event.action}' because there is was no session_start event")

                            tryToCreateSession(
                                lastStartEvent,
                                lastEndEvent,
                                sessionEvents,
                                ignoredEvents
                            ).also {
                                lastStartEvent = event
                                lastEndEvent = null
                                sessionEvents = mutableListOf()
                                sessionEvents.add(event)
                            }
                        } else {
                            val latest = sessionEvents.lastOrNull() ?: lastStartEvent ?: return@mapNotNull null

                            // only consider event as part of session, if there was no break longer than 1 hour
                            if (ChronoUnit.HOURS.between(
                                    latest.zonedTimestamp(),
                                    event.zonedTimestamp()
                                ) < 1
                            ) {
                                sessionEvents.add(event)
                                null
                            } else {
                                debug("WARN: creating new session with event of action '${event.action}' because timeframe between latest and current event is too large")

                                tryToCreateSession(
                                    lastStartEvent,
                                    latest,
                                    sessionEvents,
                                    ignoredEvents
                                ).also {
                                    lastStartEvent = event
                                    lastEndEvent = null
                                    sessionEvents = mutableListOf()
                                    sessionEvents.add(event)
                                }
                            }
                        }
                    }
                }
            }

        val asList = sessions.toCollection(mutableListOf()).also {
            // if the last session is incomplete, try to store it nevertheless
            val start = lastStartEvent ?: sessionEvents.firstOrNull()
            val lastSession = tryToCreateSession(start, lastEndEvent, sessionEvents, ignoredEvents)
            if (lastSession != null)
                it.add(lastSession)
        }

        return asList.toList() to ignoredEvents
    }

    private fun Analyse.tryToCreateSession(
        lastStartEvent: Event?,
        lastEndEvent: Event?,
        sessionEvents: List<Event>,
        ignoredEvents: MutableList<Event>
    ): Session? {
        if (lastStartEvent != null) {
            if (lastEndEvent == null || lastStartEvent == lastEndEvent) {
                debug("WARN: received another session_start event without having an session_end event - starting new session nonetheless")
                sessionEvents.lastOrNull()
                    ?.also {
                        return Session(lastStartEvent, it, sessionEvents)
                    }
                    ?: debug("WARN: dropping session that only consists of session_start event").also {
                        ignoredEvents.add(lastStartEvent)
                    }
            } else {
                return Session(lastStartEvent, lastEndEvent, sessionEvents)
            }
        } else {
            if (lastEndEvent != null) {
                debug("WARN: formerly incomplete session (no start, only end event) is being dropped due to new session_start event")
                ignoredEvents.addAll(sessionEvents)
                ignoredEvents.add(lastEndEvent)
            }
        }
        return null
    }
}
