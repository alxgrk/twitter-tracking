package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Box
import kscience.plotly.models.BoxMean
import kscience.plotly.models.BoxPoints

class TweetsPerSessionPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val scrolledTweets = sessionsPerUserId.entries.flatMap { (_, sessions) ->
            sessions
                .mapNotNull { session ->
                    session.sessionEventsInChronologicalOrder
                        .lastOrNull { it.action == "scroll" }
                        ?.estimatedTweetsScrolled
                }
                .filter { it > 0 }
        }

        val allScrolledTweet = Box {
            y.set(scrolledTweets)
            name = "All Users"
            marker {
                color("rgb(199, 174, 214)")
            }
            boxpoints = BoxPoints.outliers
            boxmean = BoxMean.sd
        }

        return Plotly.plot {
            traces(allScrolledTweet)

            layout {
                title = "Scrolled Tweets Per Session"
                xaxis {
                    title = "Users"
                }
                yaxis {
                    title = "Scrolled Tweets"
                    type = AxisType.log
                    autorange = true
                }
                showlegend = false
            }
        }
    }
}
