package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Bar
import kscience.plotly.models.BarMode
import kscience.plotly.models.TextPosition

class InteractionDistributionPerSessionPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val interactionsPerUser = sessionsPerUserId
            .map { (userId, sessions) ->
                userId to sessions.fold(Interactions(sessions.size)) { i, session ->
                    session.sessionEventsInChronologicalOrder.forEach {
                        i.increaseCountFor(it.target)
                    }
                    i
                }
            }
            .toMap()

        val bars = interactionsPerUser.entries.mapIndexed { i, (userId, interactions) ->
            Bar {
                x.set(
                    listOf(
                        "likes / session",
                        "retweets / session",
                        "postings / session",
                        "followByTweets / session",
                        "follows / session",
                        "clicksOnMedia / session",
                        "clicksOnHashtag / session",
                        "openDetailsViews / session",
                        "visitAuthorsProfiles / session"
                    )
                )
                val freqs = listOf(
                    interactions.likesPerSession,
                    interactions.retweetsPerSession,
                    interactions.postingsPerSession,
                    interactions.followsByTweetPerSession,
                    interactions.followsPerSession,
                    interactions.mediaClicksPerSession,
                    interactions.hashtagClicksPerSession,
                    interactions.detailViewsPerSession,
                    interactions.authorProfileClicksPerSession,
                )
                y.set(freqs)
                name = userId.id.substring(0, 8)
                // textsList = freqs.map { "%.3f".format(it) }
                // textposition = TextPosition.outside
                marker {
                    color(i.toRandomColor())
                }
            }
        }

        fun averageOf(prop: (Interactions) -> Double) =
            interactionsPerUser.values.map { prop(it) }.filter { it > 0 }.average()

        val averageBar = Bar {
            x.set(
                listOf(
                    "likes / session",
                    "retweets / session",
                    "postings / session",
                    "followByTweets / session",
                    "follows / session",
                    "clicksOnMedia / session",
                    "clicksOnHashtag / session",
                    "openDetailsViews / session",
                    "visitAuthorsProfiles / session"
                )
            )
            val freqs = listOf(
                averageOf { it.likesPerSession },
                averageOf { it.retweetsPerSession },
                averageOf { it.postingsPerSession },
                averageOf { it.followsByTweetPerSession },
                averageOf { it.followsPerSession },
                averageOf { it.mediaClicksPerSession },
                averageOf { it.hashtagClicksPerSession },
                averageOf { it.detailViewsPerSession },
                averageOf { it.authorProfileClicksPerSession }
            )
            y.set(
                freqs
            )
            name = "Average over all Users"
            // textsList = freqs.map { "%.3f".format(it) }
            // textposition = TextPosition.outside
            marker {
                color("rgb(129, 24, 75)")
            }
        }

        return Plotly.plot {
            traces(averageBar, *bars.toTypedArray())

            layout {
                title = "Interactions per Session by Interaction Type"
                height = 800
                xaxis {
                    title = "Interaction Types"
                }
                yaxis {
                    title = "Interactions per Session"
                    type = AxisType.log
                    autorange = true
                }
                legend {
                    x = 0
                    y = 1.0
                    bgcolor("rgba(255, 255, 255, 0)")
                    bordercolor("rgba(255, 255, 255, 0)")
                }

                barmode = BarMode.group
                bargap = 0.15
                bargroupgap = 0.01
            }
        }
    }
}
