package de.alxgrk.twittertracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.volley.toolbox.Volley
import de.alxgrk.twittertracking.TwitterTrackingAccessibilityService.Companion.TAG


object ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (context != null && intent.action == Intent.ACTION_SCREEN_OFF) {
            val sharedPreferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            val sessionStore = SessionStore(sharedPreferences)
            val eventRepository = EventRepository(context.filesDir, Volley.newRequestQueue(context))

            if (sessionStore.twitterWasOpened) {
                end(sessionStore, eventRepository)
            }
        }
    }

    private fun end(sessionStore: SessionStore, eventRepository: EventRepository) {
        eventRepository.publish(Event.SessionEndEvent(userId(sessionStore)))

        sessionStore.twitterWasOpened = false
        sessionStore.sessionState = SessionState.NEW
        sessionStore.lastScrollY = 0
        sessionStore.searchTextEnteredManually = null
        sessionStore.currentActivity = ActivitiesOfInterest.UNKNOWN_ACTIVITY
    }

    private fun userId(sessionStore: SessionStore) = sessionStore.userId ?: "unknown_user"
}

fun TwitterTrackingAccessibilityService.registerScreenOffReceiver() =
    registerReceiver(ScreenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

fun TwitterTrackingAccessibilityService.unregisterScreenOffReceiver() =
    unregisterReceiver(ScreenOffReceiver)