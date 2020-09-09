package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.*
import de.alxgrk.twittertracking.ViewIds.Companion.stripPrefix

class ClickHandler(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {
    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        Logger.logTree(nodeInfo)

        when (val viewId = nodeInfo.viewIdResourceName?.stripPrefix()) {
            ViewIds.LIKE -> eventRepository.publish(Event.ClickEvent.LikeEvent(sessionStore.userIdOrDefault))
            ViewIds.RETWEET -> eventRepository.publish(Event.ClickEvent.RetweetEvent(sessionStore.userIdOrDefault))
            ViewIds.POSTING -> eventRepository.publish(Event.ClickEvent.PostingEvent(sessionStore.userIdOrDefault))
            // 'followByTweet' covered in NOTIFICATION_STATE_CHANGED
            ViewIds.FOLLOW -> eventRepository.publish(Event.ClickEvent.FollowEvent(sessionStore.userIdOrDefault))
            ViewIds.VIDEO_PLAYER -> eventRepository.publish(
                Event.ClickEvent.ClickOnMediaEvent(
                    sessionStore.userIdOrDefault,
                    viewId.toString()
                )
            )
            // 'clickOnHashtag' covered in WINDOWS_CHANGED
            // 'openDetailsView' covered in WINDOW_STATE_CHANGED
            ViewIds.AUTHORS_PROFILE -> eventRepository.publish(
                Event.ClickEvent.VisitAuthorsProfileEvent(
                    sessionStore.userIdOrDefault
                )
            )
            else -> when { // android.widget.FrameLayout
                nodeInfo.treeContains(ViewIds.LINK) -> eventRepository.publish(
                    Event.ClickEvent.ClickOnMediaEvent(
                        sessionStore.userIdOrDefault,
                        ViewIds.LINK.toString()
                    )
                )
                nodeInfo.treeContains(ViewIds.VIDEO_WEB) || nodeInfo.treeContains(ViewIds.VIDEO_RETWEETED) -> eventRepository.publish(
                    Event.ClickEvent.ClickOnMediaEvent(
                        sessionStore.userIdOrDefault,
                        ViewIds.VIDEO_WEB.toString()
                    )
                )
                else -> eventRepository.publish(
                    Event.ClickEvent.UnknownClickEvent(
                        sessionStore.userIdOrDefault,
                        nodeInfo.viewIdResourceName ?: return
                    )
                )
            }
        }
    }

    private fun AccessibilityNodeInfo.treeContains(prefixStrippedViewId: ViewIds): Boolean {
        if (viewIdResourceName.stripPrefix() == prefixStrippedViewId)
            return true

        var oneMatched = false
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            oneMatched = child.treeContains(prefixStrippedViewId)
        }
        return oneMatched
    }

}