package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.Session.SessionType.FOLLOW
import de.alxgrk.data.Session.SessionType.POSTING
import de.alxgrk.data.Session.SessionType.SCROLLING
import de.alxgrk.data.UserId
import de.alxgrk.data.fractionOfSessionTypes
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.AxisType
import kscience.plotly.models.Bar
import kscience.plotly.models.BarMode

class SessionTypeDistributionPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionTypesPerUser = sessionsPerUserId
            .map { (userId, sessions) ->
                userId to sessions.fractionOfSessionTypes()
            }
            .toMap()

        val bars = sessionTypesPerUser.entries.mapIndexed { i, (userId, sessionTypesFreq) ->
            Bar {
                x.set(
                    listOf(
                        "Posting Session",
                        "Follow Session",
                        "Scrolling Session"
                    )
                )
                val freqs = listOf(
                    sessionTypesFreq[POSTING]?.times(100.0) ?: 0.0,
                    sessionTypesFreq[FOLLOW]?.times(100.0) ?: 0.0,
                    sessionTypesFreq[SCROLLING]?.times(100.0) ?: 0.0
                )
                y.set(
                    freqs
                )
                name = userId.id.substring(0, 8)
                // textsList = freqs.map { "%.3f".format(it)}
                // textposition = TextPosition.auto
                marker {
                    color(i.toRandomColor())
                }
            }
        }

        fun averageOf(sessionType: Session.SessionType) =
            sessionTypesPerUser.values.map { it[sessionType] ?: 0.0 }.filter { it > 0.0 }.average()

        val averageBar = Bar {
            x.set(
                listOf(
                    "Posting Session",
                    "Follow Session",
                    "Scrolling Session"
                )
            )
            val freqs = listOf(
                averageOf(POSTING) * 100.0,
                averageOf(FOLLOW) * 100.0,
                averageOf(SCROLLING) * 100.0
            )
            y.set(
                freqs
            )
            name = "Average over all Users"
            // textsList = freqs.map { "%.3f".format(it)}
            // textposition = TextPosition.auto
            marker {
                color("rgb(129, 24, 75)")
            }
        }

        return Plotly.plot {
            traces(averageBar, *bars.toTypedArray())

            layout {
                title = "Probability of Session Types"
                xaxis {
                    title = "Session Types"
                }
                yaxis {
                    title = "Session Type Probability in %"
                    type = AxisType.log
                }
                showlegend = false

                barmode = BarMode.group
                bargap = 0.15
                bargroupgap = 0.5
            }
        }
    }

    /*
    enum class SessionType {
        Posting,
        Follow,
        Scrolling
    }

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionTypesPerUser = sessionsPerUserId
            .map { (userId, sessions) ->
                userId to sessions
                    .flatMap { session ->
                        val sessionTypes = mutableListOf<SessionType>()
                        if (session.sessionEventsInChronologicalOrder.any { it.target == "posting" })
                            sessionTypes.add(Posting)
                        if (session.sessionEventsInChronologicalOrder.any { it.target == "followByTweet" || it.target == "follow" })
                            sessionTypes.add(Follow)
                        if (session.sessionEventsInChronologicalOrder.none { it.target == "posting" || it.target == "followByTweet" || it.target == "follow" })
                            sessionTypes.add(Scrolling)
                        sessionTypes
                    }
                    .groupingBy { it }
                    .eachCount()
                    .mapValues { it.value / sessions.size.toDouble() }
            }
            .toMap()

        val boxes = listOf(Posting, Follow).mapIndexed { i, sessionType ->
            Box {
                y.set(sessionTypesPerUser.values.mapNotNull { it[sessionType]?.times(100.0) })
                name = "$sessionType Session"
                marker {
                    color(((i * 4)).toRandomColor())
                }
                boxpoints = BoxPoints.outliers
                boxmean = BoxMean.sd
            }
        }

        return Plotly.plot {
            traces(boxes)

            layout {
                title = "Probability of Session Types"
                xaxis {
                    title = "Session Types"
                }
                yaxis {
                    title = "Session Type Probability in %"
                }
                showlegend = false
            }
        }
    }
     */
}
