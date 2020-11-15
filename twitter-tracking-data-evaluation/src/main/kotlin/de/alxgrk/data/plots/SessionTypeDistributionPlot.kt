package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import de.alxgrk.data.plots.SessionTypeDistributionPlot.SessionType.FOLLOW
import de.alxgrk.data.plots.SessionTypeDistributionPlot.SessionType.POSTING
import de.alxgrk.data.plots.SessionTypeDistributionPlot.SessionType.SCROLLING
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Bar
import kscience.plotly.models.BarMode

class SessionTypeDistributionPlot : Chart {

    enum class SessionType {
        SCROLLING,
        POSTING,
        FOLLOW
    }

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionTypesPerUser = sessionsPerUserId
            .map { (userId, sessions) ->
                userId to sessions
                    .flatMap { session ->
                        val sessionTypes = mutableListOf<SessionType>()
                        if (session.sessionEventsInChronologicalOrder.any { it.target == "posting" })
                            sessionTypes.add(POSTING)
                        if (session.sessionEventsInChronologicalOrder.any { it.target == "followByTweet" || it.target == "follow" })
                            sessionTypes.add(FOLLOW)
                        if (session.sessionEventsInChronologicalOrder.none { it.target == "posting" || it.target == "followByTweet" || it.target == "follow" })
                            sessionTypes.add(SCROLLING)
                        sessionTypes
                    }
                    .groupingBy { it }
                    .eachCount()
                    .mapValues { it.value / sessions.size.toDouble() }
            }
            .toMap()

        val bars = sessionTypesPerUser.entries.mapIndexed { i, (userId, sessionTypesFreq) ->
            Bar {
                x.set(
                    listOf(
                        "Posting Session Frequency",
                        "Follow Session Frequency",
                        "Scrolling Session Frequency"
                    )
                )
                y.set(
                    listOf(
                        sessionTypesFreq[POSTING] ?: 0.0,
                        sessionTypesFreq[FOLLOW] ?: 0.0,
                        sessionTypesFreq[SCROLLING] ?: 0.0
                    )
                )
                name = userId.id.substring(0, 8)
                marker {
                    color(i.toRandomColor())
                }
            }
        }

        fun averageOf(sessionType: SessionType) =
            sessionTypesPerUser.values.map { it[sessionType] ?: 0.0 }.filter { it > 0.0 }.average()

        val averageBar = Bar {
            x.set(
                listOf(
                    "Posting Session Frequency",
                    "Follow Session Frequency",
                    "Scrolling Session Frequency"
                )
            )
            y.set(
                listOf(
                    averageOf(POSTING),
                    averageOf(FOLLOW),
                    averageOf(SCROLLING)
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
                title = "Frequency of Session Types"
                xaxis {
                    title = "Session Types"
                }
                yaxis {
                    title = "Frequency per Session Type"
                    type = AxisType.log
                }
                legend {
                    x = 0
                    y = 1.0
                    bgcolor("rgba(255, 255, 255, 0)")
                    bordercolor("rgba(255, 255, 255, 0)")
                }

                barmode = BarMode.group
                bargap = 0.15
                bargroupgap = 0.5
            }
        }
    }
}
