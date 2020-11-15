package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.histogram
import kscience.plotly.layout
import java.time.temporal.ChronoField

class SessionsPerDayPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionsPerDay = sessionsPerUserId
            .flatMap { (_, sessions) ->
                sessions
                    .groupBy { it.sessionStartEvent.timestamp.zonedOrLocal().get(ChronoField.DAY_OF_YEAR) }
                    .map { (_, sessions) -> sessions.size }
            }
            .sorted()

        return Plotly.plot {

            histogram {
                x.set(sessionsPerDay)
                marker {
                    color("rgb(107, 174, 214)")
                }
            }

            layout {
                title = "Number of Sessions per Day"
                xaxis {
                    tickangle = -45
                    title = "Sessions on one Day"
                }
                yaxis {
                    gridwidth = 2
                    title = "Occurrences"
                }
                bargap = 0.05
            }
        }
    }
}
