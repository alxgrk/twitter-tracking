package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.EventRepository
import de.alxgrk.twittertracking.Logger
import de.alxgrk.twittertracking.SessionStore

interface EventHandler {
    fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo)
}

class Handlers(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {

    private val handlers = setOf(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED to create<ActivityChangeHandler>(),
        AccessibilityEvent.TYPE_VIEW_CLICKED to create<ClickHandler>(),
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED to create<PotentialFollowByTweetHandler>(),
        AccessibilityEvent.TYPE_VIEW_SCROLLED to create<ScrollHandler>(),
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED to create<TextEditHandler>()
    )

    private inline fun <reified T : EventHandler> create() =
        T::class.java.getConstructor(EventRepository::class.java, SessionStore::class.java)
            .newInstance(eventRepository, sessionStore)

    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        val handler: EventHandler = handlers
            .firstOrNull { accessibilityEvent.eventType == it.first }?.second
            ?: DefaultHandler

        Logger.d("Handling event with type ${AccessibilityEvent.eventTypeToString(accessibilityEvent.eventType)} with handler ${handler::class.java.simpleName}")

        handler.handle(accessibilityEvent, nodeInfo)
    }


}
