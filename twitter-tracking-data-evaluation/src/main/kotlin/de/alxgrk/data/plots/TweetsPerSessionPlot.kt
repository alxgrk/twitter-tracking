package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Box
import kscience.plotly.models.BoxPoints

class TweetsPerSessionPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val scrolledTweets = mutableListOf<Int>()

        val boxes = sessionsPerUserId.entries.mapIndexed { i, (userId, sessions) ->
            val scrolledTweetsOfUser = sessions
                .mapNotNull { session ->
                    session.sessionEventsInChronologicalOrder
                        .lastOrNull { it.action == "scroll" }
                        ?.estimatedTweetsScrolled
                }
                .filter { it > 0 }
            scrolledTweets.addAll(scrolledTweetsOfUser)
            Box {
                y.set(scrolledTweetsOfUser)
                name = userId.id.substring(0, 8)
                marker {
                    color(i.toRandomColor())
                }
                boxpoints = BoxPoints.outliers
            }
        }

        val allScrolledTweet = Box {
            y.set(scrolledTweets)
            name = "All Users"
            marker {
                color("rgb(199, 174, 214)")
            }
            boxpoints = BoxPoints.outliers
        }

        return Plotly.plot {
            traces(allScrolledTweet, *boxes.toTypedArray())

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
            }
        }
    }
}
