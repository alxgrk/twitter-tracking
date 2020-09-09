package de.alxgrk.twittertracking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.android.volley.toolbox.Volley
import de.alxgrk.twittertracking.handlers.Handlers

class TrackingLifecycleManager(context: Context) {

    private val eventRepository =
        EventRepository(context.filesDir, Volley.newRequestQueue(context))
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        SessionStore.SHARED_PREF_TAG,
        AccessibilityService.MODE_PRIVATE
    )
    val sessionStore = SessionStore(sharedPreferences)
    val handlers = Handlers(eventRepository, sessionStore)
    val usernameExtractor = UsernameExtractor(sessionStore)

    fun start() {
        if (sessionStore.userId != null)
            sessionStarts()

        sessionStore.twitterWasOpened = true
    }

    fun sessionStarts() {
        eventRepository.clear()
        eventRepository.publish(Event.SessionStartEvent(sessionStore.userIdOrDefault))
    }

    fun end() {
        eventRepository.publish(Event.SessionEndEvent(sessionStore.userIdOrDefault))

        sessionStore.twitterWasOpened = false
        sessionStore.sessionState = SessionState.NEW
        sessionStore.lastScrollY = 0
        sessionStore.searchTextEnteredManually = null
        sessionStore.currentActivity = ActivitiesOfInterest.UNKNOWN_ACTIVITY
    }
}