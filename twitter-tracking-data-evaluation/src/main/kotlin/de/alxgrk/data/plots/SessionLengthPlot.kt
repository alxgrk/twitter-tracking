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

class SessionLengthPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionLengths = mutableListOf<Long>()

        val boxes = sessionsPerUserId.entries.mapIndexed { i, (userId, sessions) ->
            val sessionLengthsPerUser = sessions
                .map { session -> session.durationInSeconds() }
                .filter { it < 86400 } // eliminate sessions, that last longer than one day
            sessionLengths.addAll(sessionLengthsPerUser)
            Box {
                y.set(sessionLengthsPerUser)
                name = userId.id.substring(0, 8)
                marker {
                    color(i.toRandomColor())
                }
                boxpoints = BoxPoints.outliers
            }
        }

        val sessionLengthsOfAllUsers = Box {
            y.set(sessionLengths)
            name = "All Users"
            marker {
                color("rgb(199, 174, 214)")
            }
            boxpoints = BoxPoints.outliers
        }

        return Plotly.plot {
            traces(sessionLengthsOfAllUsers, *boxes.toTypedArray())

            layout {
                title = "Session Lengths per User (cut off at 1 day)"
                xaxis {
                    title = "Users"
                    tickangle = -45
                }
                yaxis {
                    title = "Logarithmic Session Length in Seconds"
                    type = AxisType.log
                    autorange = true
                }
            }
        }
    }
}
