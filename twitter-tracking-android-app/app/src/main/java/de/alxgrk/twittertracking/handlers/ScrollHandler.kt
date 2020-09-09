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
        if (scrollPosition != null
            && sessionStore.currentActivity == ActivitiesOfInterest.MAIN
            && scrollEventCount++ % 5 == 0
            && scrollPosition > 2
        )
            eventRepository.publish(
                Event.ScrollEvent(
                    sessionStore.userIdOrDefault,
                    scrollPosition,
                    Math.floorDiv(scrollPosition, 1000)
                )
            )
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

    private fun AccessibilityEvent?.getAlreadyScrolledDeltaOAndLower(): Int? =
        this?.scrollY?.absoluteValue?.also { sessionStore.lastScrollY = it }

}