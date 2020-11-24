package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.BarMode
import kscience.plotly.models.Box
import kscience.plotly.models.BoxMean
import kscience.plotly.models.BoxPoints

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

        val boxes =
            listOf(
                "likes" to Interactions::likesPerSession,
                "retweets" to Interactions::retweetsPerSession,
                "clicksOnMedia" to Interactions::mediaClicksPerSession,
                "clicksOnHashtag" to Interactions::hashtagClicksPerSession,
                "openDetailsViews" to Interactions::detailViewsPerSession,
                "visitAuthorsProfiles" to Interactions::authorProfileClicksPerSession
            )
                .mapIndexed { i, (label, prop) ->
                    Box {
                        y.set(interactionsWithTweetsPerUser.values.map { prop(it) * 100.0 }.filter { it > 0.0 })
                        name = label
                        marker {
                            color(i.toRandomColor())
                        }
                        boxpoints = BoxPoints.outliers
                        boxmean = BoxMean.sd
                    }
                }

        return Plotly.plot {
            traces(boxes)

            layout {
                title = "Tweet Interaction Probability by Interaction Type"
                height = 800
                xaxis {
                    title = "Interaction Types"
                }
                yaxis {
                    title = "Interaction Probability in %"
                    type = AxisType.log
                    autorange = true
                }
                legend {
                    x = 0
                    y = 1.0
                    bgcolor("rgba(255, 255, 255, 0)")
                    bordercolor("rgba(255, 255, 255, 0)")
                }
            }
        }
    }
}
