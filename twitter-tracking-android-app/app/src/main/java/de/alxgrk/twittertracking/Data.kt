package de.alxgrk.twittertracking

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.alxgrk.twittertracking.ViewIds.*
import java.time.LocalDateTime

val OBJECT_MAPPER = ObjectMapper().apply {
    disable(SerializationFeature.INDENT_OUTPUT)
}

@JsonInclude(Include.NON_NULL)
sealed class Event(
    val action: String,
    val userId: String,
    val eventType: String = "ANDROID",
    val timestamp: String = LocalDateTime.now().toString()
) {
    override fun toString(): String = OBJECT_MAPPER.writeValueAsString(this)

    class SessionStartEvent(userId: String) : Event("session_start", userId)

    class SessionEndEvent(userId: String) : Event("session_end", userId)

    class ScrollEvent(
        userId: String,
        val scrollPosition: Int,
        val estimatedTweetsScrolled: Int
    ) : Event("scroll", userId)

    sealed class ClickEvent(
        userId: String,
        val target: String?,
        val selector: String
    ) : Event("click", userId) {

        class LikeEvent(userId: String) : ClickEvent(userId, "like", LIKE.toString())
        class RetweetEvent(userId: String) : ClickEvent(userId, "retweet", RETWEET.toString())
        class PostingEvent(userId: String) : ClickEvent(userId, "posting", POSTING.toString())
        class FollowEvent(userId: String) : ClickEvent(userId, "follow", FOLLOW.toString())
        class VisitAuthorsProfileEvent(userId: String) :
            ClickEvent(userId, "visitAuthorsProfile", AUTHORS_PROFILE.toString())

        class ClickOnMediaEvent(userId: String, selector: String) :
            ClickEvent(userId, "clickOnMedia", selector)

        class ClickOnHashtagEvent(userId: String, selector: String) :
            ClickEvent(userId, "clickOnHashtag", selector)

        class FollowByTweetEvent(userId: String, selector: String) :
            ClickEvent(userId, "followByTweet", selector)

        class OpenDetailsViewEvent(userId: String, activity: ActivitiesOfInterest) :
            ClickEvent(userId, "openDetailsView", activity.toString())

        class UnknownClickEvent(userId: String, selector: String) :
            ClickEvent(userId, null, selector)
    }

}

enum class ActivitiesOfInterest(private val originalString: String?) {
    GALLERY("com.twitter.app.gallery.GalleryActivity"),
    MAIN("com.twitter.app.main.MainActivity"),
    PROFILE("com.twitter.app.profiles.ProfileActivity"),
    TWEET_DETAIL("com.twitter.android.TweetDetailActivity"),
    SEARCH("com.twitter.android.search.SearchActivity"),
    COMPOSER("com.twitter.composer.ComposerActivity"),
    BOTTOM_SHEET_FOLLOW("com.google.android.material.bottomsheet.a"),
    UNKNOWN_ACTIVITY(null);

    override fun toString() = originalString ?: "UNKNOWN"

    companion object {
        fun parse(activityName: String) =
            values().firstOrNull { it.originalString == activityName } ?: UNKNOWN_ACTIVITY
    }
}

enum class ViewIds(private val prefixStrippedViewId: String?) {
    LIKE("inline_like"),
    RETWEET("inline_retweet"),
    POSTING("button_tweet"),
    FOLLOW("follow_button"),
    VIDEO_PLAYER("player"),
    AUTHORS_PROFILE("tweet_profile_image"),

    LINK("card_container"),
    VIDEO_WEB("video_chrome"),
    VIDEO_RETWEETED("video_player_view"),
    SEARCH_BOX("query_view"),
    SEARCH_EDIT_TEXT("query"),
    UNKNOWN_ID(null);

    override fun toString() = prefixStrippedViewId ?: "UNKNOWN"

    fun withPrefix() = "${TwitterTrackingAccessibilityService.VIEW_ID_PREFIX}${toString()}"

    companion object {
        fun String.toViewId() =
            values().firstOrNull { it.prefixStrippedViewId == this } ?: UNKNOWN_ID
    }
}

enum class SessionState {
    NEW,
    DRAWER_OPENED,
    USERNAME_EXTRACTED;
}