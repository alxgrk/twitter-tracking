package de.alxgrk.twittertracking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object Logger {

    private const val TAG = "TwitterTracking"

    fun d(msg: String) = Log.d(TAG, msg)
    fun e(msg: String) = Log.e(TAG, msg)
    fun w(msg: String) = Log.w(TAG, msg)
    fun i(msg: String) = Log.i(TAG, msg)

    fun logTree(nodeInfo: AccessibilityNodeInfo, stage: Int = 0) {
        if (!BuildConfig.DEBUG)
            return

        if (stage == 0)
            d("- ${nodeInfo.viewIdResourceName}(${nodeInfo.className})")

        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            d("${
                (0..stage)
                    .fold("") { acc, _ -> "$acc\t" }
            } - ${child.viewIdResourceName}(${child.className}): \"${child.text}\" (${child.contentDescription})")
            logTree(child, stage + 1)
        }
    }

}