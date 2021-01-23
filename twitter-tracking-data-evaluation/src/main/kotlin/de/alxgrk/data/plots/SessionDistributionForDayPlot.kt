package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.layout
import kscience.plotly.models.BarMode
import java.time.temporal.ChronoField

class SessionDistributionForDayPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val numberOfSessionsPerBucketPerUser = sessionsPerUserId
            .mapValues { sessions ->
                sessions.value
                    .groupBy {
                        val zonedOrLocal = it.sessionStartEvent.zonedTimestamp()
                        val hour = zonedOrLocal.get(ChronoField.HOUR_OF_DAY).toString().padStart(2, '0')
                        val halfOrFull = if (zonedOrLocal.get(ChronoField.MINUTE_OF_HOUR) < 30) "00" else "30"
                        hour to halfOrFull
                    }
                    .map { (hourAndMinute, sessions) -> "${hourAndMinute.first}:${hourAndMinute.second}" to sessions.size }
            }

        return Plotly.plot {

            numberOfSessionsPerBucketPerUser.entries.forEachIndexed { i, (_, pairs) ->
                val asMap = pairs.toMap().toSortedMap()
                bar {
                    x.set(asMap.keys)
                    y.set(asMap.values)
                    name = "U${i + 1}"
                    marker {
                        color(i.toRandomColor())
                    }
                }
            }

            layout {
                title = "Number of Sessions per Time Slot"
                xaxis {
                    tickangle = -45
                    title = "Time slots"
                }
                yaxis {
                    gridwidth = 2
                    title = "Number of Sessions"
                }
                legend {
                    x = 0
                    y = 1.0
                    bgcolor("rgba(255, 255, 255, 0)")
                    bordercolor("rgba(255, 255, 255, 0)")
                }
                bargap = 0.05
                barmode = BarMode.stack
            }
        }
    }
}
