package de.alxgrk.twittertracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (context != null && intent.action == Intent.ACTION_SCREEN_OFF) {
            val trackingLifecycleManager = TrackingLifecycleManager(context)

            if (trackingLifecycleManager.sessionStore.twitterWasOpened) {
                trackingLifecycleManager.end()
            }
        }
    }

}

fun TwitterTrackingAccessibilityService.registerScreenOffReceiver() =
    registerReceiver(ScreenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

fun TwitterTrackingAccessibilityService.unregisterScreenOffReceiver() =
    unregisterReceiver(ScreenOffReceiver)