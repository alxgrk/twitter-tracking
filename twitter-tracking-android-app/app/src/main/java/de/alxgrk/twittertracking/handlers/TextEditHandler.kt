package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.EventRepository
import de.alxgrk.twittertracking.Logger
import de.alxgrk.twittertracking.SessionStore
import de.alxgrk.twittertracking.ViewIds
import de.alxgrk.twittertracking.ViewIds.Companion.stripPrefix

class TextEditHandler(
    private val eventRepository: EventRepository,
    private val sessionStore: SessionStore
) : EventHandler {
    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        val editText = nodeInfo.viewIdResourceName.stripPrefix()
        if (editText == ViewIds.SEARCH_BOX || editText == ViewIds.SEARCH_EDIT_TEXT) {
            sessionStore.searchTextEnteredManually = nodeInfo.text?.toString()?.also {
                Logger.d("Search text entered into query view: $it")
            }
        }
    }
}