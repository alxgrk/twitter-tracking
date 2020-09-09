package de.alxgrk.twittertracking

import android.view.accessibility.AccessibilityNodeInfo
import de.alxgrk.twittertracking.SessionState.*
import de.alxgrk.twittertracking.ViewIds.Companion.VIEW_ID_PREFIX

class UsernameExtractor(val sessionStore: SessionStore) {

    fun extractStepwise(nodeInfo: AccessibilityNodeInfo): SessionState =

        when (sessionStore.sessionState) {
            NEW -> {
                val userNameContainer =
                    nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}name_container")
                if (userNameContainer.isEmpty())
                    nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}toolbar")
                        .firstOrNull()
                        ?.let { drawerNode ->
                            val openDrawer =
                                if (drawerNode.childCount > 0) drawerNode.getChild(0) else return@let null

                            if (openDrawer.isClickable) {
                                openDrawer.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                DRAWER_OPENED.also { sessionStore.sessionState = it }
                            }
                            null
                        }
                        ?: NEW
                else
                    NEW
            }
            DRAWER_OPENED -> {
                nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}name_container")
                    .firstOrNull()
                    ?.let { containerNode ->
                        val userName =
                            if (containerNode.childCount > 0) containerNode.getChild(0) else return@let DRAWER_OPENED
                        Logger.i("Logged in user is ${userName.text}")

                        sessionStore.userId =
                            calculateUserIdHash(
                                userName.text?.toString() ?: return@let DRAWER_OPENED
                            )

                        USERNAME_EXTRACTED.also { sessionStore.sessionState = it }
                    }
                    ?: DRAWER_OPENED
            }
            USERNAME_EXTRACTED -> USERNAME_EXTRACTED
        }

}