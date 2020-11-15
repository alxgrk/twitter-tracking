package de.alxgrk.data

import io.inbot.eskotlinwrapper.AsyncIndexRepository
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toCollection
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.builder.SearchSourceBuilder.searchSource
import org.elasticsearch.search.sort.SortBuilders.fieldSort

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
                        lastEndEvent = event
                        null
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
                            sessionEvents.add(event)
                            null
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
            if (lastEndEvent == null) {
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
