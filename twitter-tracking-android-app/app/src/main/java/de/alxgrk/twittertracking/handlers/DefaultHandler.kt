package de.alxgrk.twittertracking.handlers

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.EventRepository
import de.alxgrk.twittertracking.Logger
import de.alxgrk.twittertracking.SessionStore

object DefaultHandler : EventHandler {
    override fun handle(accessibilityEvent: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        Logger.d("Captured event details: $accessibilityEvent")
    }
}