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

class InteractionDistributionPerTweetPlot : Chart {

    companion object {
        const val medianOfAllUsersFromTweetsPerSessionPlot = 15
    }

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {
        val interactionsWithTweetsPerUser = sessionsPerUserId
            .map { (userId, sessions) ->
                val numberOfTweetsPerUser = sessions
                    .map { session ->
                        session.sessionEventsInChronologicalOrder
                            .lastOrNull { it.estimatedTweetsScrolled != null }
                            ?.estimatedTweetsScrolled
                            ?: medianOfAllUsersFromTweetsPerSessionPlot
                    }
                    .sum()
                userId to sessions.fold(Interactions(numberOfTweetsPerUser)) { i, session ->
                    session.sessionEventsInChronologicalOrder.forEach {
                        i.increaseCountFor(it.target)
                    }
                    i
                }
            }
            .toMap()

        val bars = interactionsWithTweetsPerUser.entries.mapIndexed { i, (userId, interactions) ->
            Bar {
                x.set(
                    listOf(
                        "likes / session",
                        "retweets / session",
                        "clicksOnMedia / session",
                        "clicksOnHashtag / session",
                        "openDetailsViews / session",
                        "visitAuthorsProfiles / session"
                    )
                )
                y.set(
                    listOf(
                        interactions.likesPerSession,
                        interactions.retweetsPerSession,
                        interactions.mediaClicksPerSession,
                        interactions.hashtagClicksPerSession,
                        interactions.detailViewsPerSession,
                        interactions.authorProfileClicksPerSession,
                    )
                )
                name = userId.id.substring(0, 8)
                marker {
                    color(i.toRandomColor())
                }
            }
        }

        fun averageOf(prop: (Interactions) -> Double) =
            interactionsWithTweetsPerUser.values.map { prop(it) }.filter { it > 0 }.average()

        val averageBar = Bar {
            x.set(
                listOf(
                    "likes / session",
                    "retweets / session",
                    "clicksOnMedia / session",
                    "clicksOnHashtag / session",
                    "openDetailsViews / session",
                    "visitAuthorsProfiles / session"
                )
            )
            y.set(
                listOf(
                    averageOf { it.likesPerSession },
                    averageOf { it.retweetsPerSession },
                    averageOf { it.mediaClicksPerSession },
                    averageOf { it.hashtagClicksPerSession },
                    averageOf { it.detailViewsPerSession },
                    averageOf { it.authorProfileClicksPerSession }
                )
            )
            name = "Average over all Users"
            marker {
                color("rgb(129, 24, 75)")
            }
        }

        return Plotly.plot {
            traces(averageBar, *bars.toTypedArray())

            layout {
                title = "Interactions per Tweet by Interaction Type"
                height = 800
                xaxis {
                    title = "Interaction Types"
                }
                yaxis {
                    title = "Interactions per Tweet"
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
                bargroupgap = 0.1
            }
        }
    }
}
