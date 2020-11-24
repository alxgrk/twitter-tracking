package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Event
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Box
import kscience.plotly.models.BoxMean
import kscience.plotly.models.BoxPoints
import kotlin.math.absoluteValue

typealias Millisecond = Long
typealias PreviousEvent = Event?

class TimeSpentPerTweetPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val millisecondsSpentOnTweets = sessionsPerUserId.entries.flatMap { (_, sessions) ->
            sessions
                .map { session ->
                    session.sessionEventsInChronologicalOrder
                        .filter { it.action == "scroll" }
                        .distinctBy { it.estimatedTweetsScrolled }
                        .fold(mutableListOf<Millisecond>() to null as PreviousEvent) { (seconds, prevEvent), event ->
                            prevEvent
                                ?.durationInMilliseconds(event)
                                ?.let {
                                    seconds.add(it / (event.estimatedTweetsScrolled!! - prevEvent.estimatedTweetsScrolled!!).absoluteValue)
                                }
                            seconds to event
                        }
                }
                .flatMap { it.first }
                .filter { it < 120000 } // eliminate intervals, that last longer than 2 minutes
        }

        val millisecondsSpentOnTweetsOfAllUsers = Box {
            y.set(millisecondsSpentOnTweets)
            name = "All Users"
            marker {
                color("rgb(199, 174, 214)")
            }
            boxpoints = BoxPoints.`false`
            boxmean = BoxMean.sd
        }

        return Plotly.plot {
            traces(millisecondsSpentOnTweetsOfAllUsers)

            layout {
                title = "Milliseconds spent on Tweets (cut off at 2 minute)"
                xaxis {
                    title = "Users"
                }
                yaxis {
                    title = "Time spent on Tweets in Milliseconds"
                    type = AxisType.log
                    autorange = true
                }
                showlegend = false
            }
        }
    }
}
