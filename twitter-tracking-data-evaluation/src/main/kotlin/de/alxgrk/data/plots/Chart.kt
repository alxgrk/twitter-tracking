@file:Suppress("EXPERIMENTAL_API_USAGE")

package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Event
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.makeFile
import org.reflections.Reflections
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.full.createInstance

interface Chart {

    fun Analyse.create(sessionsPerUserId: Map<UserId, List<Session>>) {
        val chartFile = File("./${this@Chart::class.simpleName!!}.${if (export) "svg" else "html"}")
        val plot = createPlot(sessionsPerUserId)

        store(plot, chartFile)
    }

    fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot

    fun Event.durationInMilliseconds(other: Event): Long = ChronoUnit.MILLIS.between(
        this.zonedTimestamp(),
        other.zonedTimestamp()
    ).absoluteValue

    fun Int.toRandomColor(): String {
        val color =
            listOf(1.1, 1.2, 1.4)
                .map { minOf((Random.nextInt(60, 120) * it * (this * 0.3)).toInt(), 210) }
                .joinToString(",")
        return "rgb($color)"
    }
}

fun Analyse.store(plot: Plot, chartFile: File) {
    if (export) {
        OrcaContainer(DockerImageName.parse("quay.io/plotly/orca"))
            .withExposedPorts(9091)
            .use { container ->
                container.start()
                val json = plot.toJson().toString()

                container.copyFileToContainer(Transferable.of(json.toByteArray()), "/root/input.json")
                val result = container.execInContainer(
                    "orca", "graph", "/root/input.json",
                    "-f", "svg",
                    "-d", "/root/",
                    "-o", "plot.svg"
                )
                container.copyFileFromContainer("/root/plot.svg", chartFile.absolutePath)
                debug("Orca finished with code: ${result.exitCode}")
                info("Stored chart at ${chartFile.absolutePath}")
            }
    } else {
        plot.makeFile(path = chartFile.toPath(), show = false)
        info("Stored chart at ${chartFile.absolutePath}")
    }
}

val chartSubClasses by lazy {
    Reflections("de.alxgrk.data.plots").getSubTypesOf(Chart::class.java)
}

fun findChartNames() = chartSubClasses.map { it.simpleName }
fun findCharts() = chartSubClasses.map { it.simpleName to it.kotlin.createInstance() }.toMap()

data class Interactions(
    val total: Int,
    var mediaClicks: Int = 0,
    var detailViews: Int = 0,
    var hashtagClicks: Int = 0,
    var authorProfileClicks: Int = 0,
    var likes: Int = 0,
    var retweets: Int = 0,
    var postings: Int = 0,
    var follows: Int = 0,
    var followsByTweet: Int = 0
) {
    fun increaseCountFor(target: String?) {
        when (target) {
            "like" -> likes++
            "retweet" -> retweets++
            "posting" -> postings++
            "followByTweet" -> followsByTweet++
            "follow" -> follows++
            "clickOnMedia" -> mediaClicks++
            "clickOnHashtag" -> hashtagClicks++
            "openDetailsView" -> detailViews++
            "visitAuthorsProfile" -> authorProfileClicks++
        }
    }

    val likesPerSession by lazy { likes.toDouble() / total }
    val retweetsPerSession by lazy { retweets.toDouble() / total }
    val postingsPerSession by lazy { postings.toDouble() / total }
    val followsByTweetPerSession by lazy { followsByTweet.toDouble() / total }
    val followsPerSession by lazy { follows.toDouble() / total }
    val mediaClicksPerSession by lazy { mediaClicks.toDouble() / total }
    val hashtagClicksPerSession by lazy { hashtagClicks.toDouble() / total }
    val detailViewsPerSession by lazy { detailViews.toDouble() / total }
    val authorProfileClicksPerSession by lazy { authorProfileClicks.toDouble() / total }
}

class OrcaContainer(dockerImageName: DockerImageName) : GenericContainer<OrcaContainer>(dockerImageName)
