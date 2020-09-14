package de.alxgrk.twittertracking.handlers

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import de.alxgrk.twittertracking.ActivitiesOfInterest
import de.alxgrk.twittertracking.Event
import de.alxgrk.twittertracking.EventRepository
import de.alxgrk.twittertracking.SessionStore
import kotlin.math.absoluteValue

class ScrollHandler(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {

    private var scrollEventCount = 0

    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        val scrollPosition = accessibilityEvent.getAlreadyScrolled()
        if (sessionStore.currentActivity == ActivitiesOfInterest.MAIN && scrollEventCount++ % 5 == 0) {
            if (scrollPosition == null && scrollEventCount % 10 == 0) {
                // roughly, every 10th scroll is a tweet
                eventRepository.publish(
                    Event.ScrollEvent(
                        sessionStore.userIdOrDefault,
                        -1,
                        scrollEventCount % 10
                    )
                )
            }
            if (scrollPosition != null && scrollPosition > 2) {
                eventRepository.publish(
                    Event.ScrollEvent(
                        sessionStore.userIdOrDefault,
                        scrollPosition,
                        Math.floorDiv(scrollPosition, 1000)
                    )
                )
            }
        }
    }

    private fun AccessibilityEvent?.getAlreadyScrolled(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getAlreadyScrolledPAndHigher()
        } else {
            getAlreadyScrolledDeltaOAndLower()
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun AccessibilityEvent?.getAlreadyScrolledPAndHigher(): Int? =
        this?.scrollDeltaY?.absoluteValue?.plus(sessionStore.lastScrollY)
            ?.also { sessionStore.lastScrollY = it }

    // for some reason, scrollY is always -1 on Oreo
    private fun AccessibilityEvent?.getAlreadyScrolledDeltaOAndLower(): Int? = null
    // this?.scrollY?.absoluteValue?.also { sessionStore.lastScrollY = it }

}