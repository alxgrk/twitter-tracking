package de.alxgrk.twittertracking

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.android.volley.toolbox.Volley
import de.alxgrk.twittertracking.ActivitiesOfInterest.*
import de.alxgrk.twittertracking.BuildConfig.API_URL
import de.alxgrk.twittertracking.Event.*
import de.alxgrk.twittertracking.Event.ClickEvent.*
import de.alxgrk.twittertracking.SessionState.*
import de.alxgrk.twittertracking.ViewIds.*
import de.alxgrk.twittertracking.ViewIds.Companion.toViewId
import java.security.MessageDigest
import kotlin.math.absoluteValue


class TwitterTrackingAccessibilityService : AccessibilityService() {

    private lateinit var sessionStore: SessionStore
    private lateinit var eventRepository: EventRepository

    override fun onServiceConnected() {
        Log.d(TAG, "Service connected")

        val sharedPreferences = applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        sessionStore = SessionStore(sharedPreferences)
        eventRepository = EventRepository(application.filesDir, Volley.newRequestQueue(this))

        if (sessionStore.userId != null)
            sessionStarts()
    }

    private fun sessionStarts() {
        eventRepository.clear()
        eventRepository.publish(SessionStartEvent(userId()))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "About to unbind.")

        sessionStore.sessionState = NEW
        eventRepository.publish(SessionEndEvent(userId()))

        sessionStore.lastScrollY = 0
        sessionStore.searchTextEnteredManually = null
        sessionStore.currentActivity = UNKNOWN_ACTIVITY
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        Log.e(TAG, "Was interrupted...")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val nodeInfo = event?.source ?: rootInActiveWindow ?: return
        nodeInfo.refresh()

        if (sessionStore.userId == null)
            stepWiseUserNameExtraction(nodeInfo)

        when (event?.eventType) {

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(event)

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleActivityChange(event, nodeInfo)

            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleClick(nodeInfo)

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextEdit(nodeInfo)

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> handlePotentialFollowByTweet(event)

            else -> {
                val eventTypeAsString = event?.let {
                    AccessibilityEvent.eventTypeToString(it.eventType)
                }
                Log.d(TAG, "Captured event with type '$eventTypeAsString': $event")
            }
        }

        nodeInfo.recycle()
    }

    private fun stepWiseUserNameExtraction(nodeInfo: AccessibilityNodeInfo) {
        when (sessionStore.sessionState) {
            NEW -> {
                val userNameContainer =
                    nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}name_container")
                if (userNameContainer.isEmpty())
                    nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}toolbar")
                        .firstOrNull()
                        ?.let {
                            val openDrawer = if (it.childCount > 0) it.getChild(0) else return@let

                            if (openDrawer.isClickable) {
                                openDrawer.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                sessionStore.sessionState = DRAWER_OPENED
                            }
                        }
            }
            DRAWER_OPENED -> {
                nodeInfo.findAccessibilityNodeInfosByViewId("${VIEW_ID_PREFIX}name_container")
                    .firstOrNull()
                    ?.let { containerNode ->
                        val userName =
                            if (containerNode.childCount > 0) containerNode.getChild(0) else return@let
                        Log.i(TAG, "Logged in user is ${userName.text}")

                        sessionStore.userId =
                            calculateUserIdHash(userName.text?.toString() ?: return@let)

                        sessionStarts()
                        sessionStore.sessionState = USERNAME_EXTRACTED

                        performLeftSwipe()
                    }
            }
            USERNAME_EXTRACTED -> { /* do nothing */
            }
        }
    }

    var scrollEventCount = 0
    private fun handleScroll(event: AccessibilityEvent?) {
        val scrollPosition = event.getAlreadyScrolled()
        if (scrollPosition != null
            && sessionStore.currentActivity == MAIN
            && scrollEventCount++ % 5 == 0
            && scrollPosition > 2
        )
            eventRepository.publish(
                ScrollEvent(
                    userId(),
                    scrollPosition,
                    Math.floorDiv(scrollPosition, 1000)
                )
            )
    }

    private fun handleActivityChange(event: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo) {
        Log.d(TAG, "Changed activity to ${event.className}")
        sessionStore.currentActivity =
            ActivitiesOfInterest.parse(event.className?.toString() ?: return)

        when (sessionStore.currentActivity) {
            TWEET_DETAIL -> eventRepository.publish(
                OpenDetailsViewEvent(
                    userId(), sessionStore.currentActivity
                )
            )
            GALLERY -> eventRepository.publish(
                ClickOnMediaEvent(
                    userId(), sessionStore.currentActivity.toString()
                )
            )
            else -> { /* do nothing */
            }
        }

        handlePotentialHashtagClick(nodeInfo)
    }

    private fun handlePotentialHashtagClick(nodeInfo: AccessibilityNodeInfo) {
        if (sessionStore.currentActivity == SEARCH) {
            if (sessionStore.searchTextEnteredManually == null) {
                nodeInfo.findAccessibilityNodeInfosByViewId(SEARCH_BOX.withPrefix())
                    .firstOrNull()
                    ?.also {
                        Log.d(
                            TAG,
                            "Triggered search activity with no prior typing into query view - hence, hashtag click for ${it.text}"
                        )
                        eventRepository.publish(
                            ClickOnHashtagEvent(
                                userId(), it.text?.toString() ?: ""
                            )
                        )
                    }
            }
            // clear in any case
            sessionStore.searchTextEnteredManually = null
        }
    }

    private fun handleClick(nodeInfo: AccessibilityNodeInfo) {
        logTree(nodeInfo)

        when (val viewId = nodeInfo.viewIdResourceName?.stripPrefix()) {
            LIKE -> eventRepository.publish(LikeEvent(userId()))
            RETWEET -> eventRepository.publish(RetweetEvent(userId()))
            POSTING -> eventRepository.publish(PostingEvent(userId()))
            // 'followByTweet' covered in NOTIFICATION_STATE_CHANGED
            FOLLOW -> eventRepository.publish(FollowEvent(userId()))
            VIDEO_PLAYER -> eventRepository.publish(ClickOnMediaEvent(userId(), viewId.toString()))
            // 'clickOnHashtag' covered in WINDOWS_CHANGED
            // 'openDetailsView' covered in WINDOW_STATE_CHANGED
            AUTHORS_PROFILE -> eventRepository.publish(
                VisitAuthorsProfileEvent(
                    userId()
                )
            )
            else -> when { // android.widget.FrameLayout
                nodeInfo.treeContains(LINK) -> eventRepository.publish(
                    ClickOnMediaEvent(
                        userId(),
                        LINK.toString()
                    )
                )
                nodeInfo.treeContains(VIDEO_WEB) || nodeInfo.treeContains(VIDEO_RETWEETED) -> eventRepository.publish(
                    ClickOnMediaEvent(userId(), VIDEO_WEB.toString())
                )
                else -> eventRepository.publish(
                    UnknownClickEvent(
                        userId(),
                        nodeInfo.viewIdResourceName ?: return
                    )
                )
            }
        }
    }

    private fun handleTextEdit(nodeInfo: AccessibilityNodeInfo) {
        val editText = nodeInfo.viewIdResourceName.stripPrefix()
        if (editText == SEARCH_BOX || editText == SEARCH_EDIT_TEXT) {
            sessionStore.searchTextEnteredManually = nodeInfo.text?.toString()?.also {
                Log.d(TAG, "Search text entered into query view: $it")
            }
        }
    }

    private fun handlePotentialFollowByTweet(event: AccessibilityEvent) {
        if (sessionStore.currentActivity == BOTTOM_SHEET_FOLLOW
            && (event.text?.any { it.contains("folg") } == true
                    || event.text?.any { it.contains("follow") } == true)
        )
            eventRepository.publish(
                FollowByTweetEvent(
                    userId(),
                    event.text?.toString() ?: ""
                )
            )
    }

    private fun userId() = sessionStore.userId ?: "unknown_user"

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

    private fun logTree(nodeInfo: AccessibilityNodeInfo, stage: Int = 0) {
        if (API_URL.contains("10.0.2.2"))
            return

        if (stage == 0)
            Log.d(TAG, "- ${nodeInfo.viewIdResourceName}(${nodeInfo.className})")

        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            Log.d(
                TAG,
                "${(0..stage).fold("") { acc, _ -> "$acc\t" }} - ${child.viewIdResourceName}(${child.className}): \"${child.text}\" (${child.contentDescription})"
            )
            logTree(child, stage + 1)
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

    private fun AccessibilityEvent?.getAlreadyScrolledDeltaOAndLower(): Int? =
        this?.scrollY?.absoluteValue?.also { sessionStore.lastScrollY = it }

    companion object {

        private val TAG = TwitterTrackingAccessibilityService::class.java.simpleName
        internal const val VIEW_ID_PREFIX = "com.twitter.android:id/"
        internal fun CharSequence?.stripPrefix(): ViewIds? =
            this?.split(VIEW_ID_PREFIX)?.last()?.toViewId()

    }
}

fun calculateUserIdHash(userName: String) = MessageDigest
    .getInstance("SHA-256")
    .digest(userName.toByteArray())
    .joinToString("") { "%02x".format(it) }