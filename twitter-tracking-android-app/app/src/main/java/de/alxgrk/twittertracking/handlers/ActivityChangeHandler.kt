package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.*

class ActivityChangeHandler(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {

    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        Logger.d("Changed activity to ${accessibilityEvent.className}")
        sessionStore.currentActivity =
            ActivitiesOfInterest.parse(accessibilityEvent.className?.toString() ?: return)

        when (sessionStore.currentActivity) {
            ActivitiesOfInterest.TWEET_DETAIL -> eventRepository.publish(
                Event.ClickEvent.OpenDetailsViewEvent(
                    sessionStore.userIdOrDefault, sessionStore.currentActivity
                )
            )
            ActivitiesOfInterest.GALLERY -> eventRepository.publish(
                Event.ClickEvent.ClickOnMediaEvent(
                    sessionStore.userIdOrDefault, sessionStore.currentActivity.toString()
                )
            )
            else -> { /* do nothing */
            }
        }

        handlePotentialHashtagClick(nodeInfo)
    }

    private fun handlePotentialHashtagClick(nodeInfo: AccessibilityNodeInfo) {
        if (sessionStore.currentActivity == ActivitiesOfInterest.SEARCH) {
            if (sessionStore.searchTextEnteredManually == null) {
                nodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.SEARCH_BOX.withPrefix())
                    .firstOrNull()
                    ?.also {
                        Logger.d("Triggered search activity with no prior typing into query view - hence, hashtag click for ${it.text}")
                        eventRepository.publish(
                            Event.ClickEvent.ClickOnHashtagEvent(
                                sessionStore.userIdOrDefault, it.text?.toString() ?: ""
                            )
                        )
                    }
            }
            // clear in any case
            sessionStore.searchTextEnteredManually = null
        }
    }

}