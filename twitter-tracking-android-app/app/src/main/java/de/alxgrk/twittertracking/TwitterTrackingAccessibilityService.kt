package de.alxgrk.twittertracking

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import java.security.MessageDigest


class TwitterTrackingAccessibilityService : AccessibilityService() {

    lateinit var trackingLifecycleManager: TrackingLifecycleManager

    override fun onServiceConnected() {
        Logger.d("Service connected")

        trackingLifecycleManager = TrackingLifecycleManager(this)

        trackingLifecycleManager.start()
        registerScreenOffReceiver()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.d("About to unbind.")

        unregisterScreenOffReceiver()
        trackingLifecycleManager.end()

        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        Logger.e("Was interrupted...")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val nodeInfo = (event ?: return).source ?: rootInActiveWindow ?: return
        nodeInfo.refresh()

        if (trackingLifecycleManager.sessionStore.userId == null) {
            val newState = trackingLifecycleManager.usernameExtractor.extractStepwise(nodeInfo)
            if (newState == SessionState.USERNAME_EXTRACTED) {
                trackingLifecycleManager.sessionStarts()
                performLeftSwipe()
            }
        }

        if (!trackingLifecycleManager.sessionStore.twitterWasOpened)
            trackingLifecycleManager.start()

        trackingLifecycleManager.handlers.handle(event, nodeInfo)

        nodeInfo.recycle()
    }

    private fun performLeftSwipe() {
        val displayMetrics = resources.displayMetrics

        val middleXValue = displayMetrics.widthPixels / 2
        val middleYValue = displayMetrics.heightPixels / 2

        GestureDescription.Builder().apply {
            val path = Path().apply {
                moveTo(middleXValue.toFloat(), middleYValue.toFloat())
                lineTo(0f, middleYValue.toFloat())
            }

            addStroke(StrokeDescription(path, 100, 50))
            dispatchGesture(build(), null, null)
        }
    }

}

fun calculateUserIdHash(userName: String) = MessageDigest
    .getInstance("SHA-256")
    .digest(userName.toByteArray())
    .joinToString("") { "%02x".format(it) }