package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.ActivitiesOfInterest
import de.alxgrk.twittertracking.Event
import de.alxgrk.twittertracking.EventRepository
import de.alxgrk.twittertracking.SessionStore

class PotentialFollowByTweetHandler(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {
    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        if (sessionStore.currentActivity == ActivitiesOfInterest.BOTTOM_SHEET_FOLLOW
            && (accessibilityEvent.text?.any { it.contains("folg") } == true
                    || accessibilityEvent.text?.any { it.contains("follow") } == true)
        )
            eventRepository.publish(
                Event.ClickEvent.FollowByTweetEvent(
                    sessionStore.userIdOrDefault,
                    accessibilityEvent.text?.toString() ?: ""
                )
            )
    }
}