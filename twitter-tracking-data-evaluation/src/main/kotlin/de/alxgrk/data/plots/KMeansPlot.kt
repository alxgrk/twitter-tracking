package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import de.alxgrk.data.plots.SessionsPerDayPlot.Companion.byDay
import hep.dataforge.values.asValue
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.Scatter
import kscience.plotly.models.ScatterMode
import kscience.plotly.models.TextPosition
import org.nield.kotlinstatistics.Centroid
import org.nield.kotlinstatistics.DoublePoint
import org.nield.kotlinstatistics.kMeansCluster
import kotlin.math.pow
import kotlin.math.sqrt

class KMeansPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {
        val asDoubleList = sessionsPerUserId
            .map { (_, sessions) ->
                sessions.byDay().average() to clicksPerSession(sessions)
            }
            .toMap()

        val pointsToUserMap = asDoubleList.toList()
            .mapIndexed { index, pair ->
                pair to sessionsPerUserId.keys.toList()[index]
            }
            .toMap()

        val clusters = asDoubleList.toList().kMeansCluster(
            k = 2,
            maxIterations = 10000
        )

        // print out the clusters
        val traces = clusters.mapIndexed { index, item ->
            info("CENTROID: $index with center [${item.center.x}, ${item.center.y}]")
            item.points.forEach {
                info("\t${pointsToUserMap[it]}: [${it.first},${it.second}]")
            }
            Scatter {
                name = if (item.points.size < (sessionsPerUserId.size / 2)) "More Active" else "Less Active"
                x.set(listOf(item.center.x, *item.points.map { it.first }.toTypedArray()))
                y.set(listOf(item.center.y, *item.points.map { it.second }.toTypedArray()))
                textsList = listOf(
                    "C$index",
                    *item.points.map { "U${pointsToUserMap.values.toList().indexOf(pointsToUserMap[it])}" }
                        .toTypedArray()
                )
                textpositionsList = listOf(
                    TextPosition.`middle center`.name.asValue(),
                    *item.points.map { TextPosition.`top center`.name.asValue() }.toTypedArray()
                )
                marker {
                    sizesList = listOf(centroidMarkerSize(item), *item.points.map { 10 }.toTypedArray())
                    opacitiesList = listOf(0.1, *item.points.map { 1 }.toTypedArray())
                }
                mode = ScatterMode.`markers+text`
            }
        }

        return Plotly.plot {
            traces(traces)

            layout {
                title = "Users clustered by click behavior in relation to daily activity"
                width = 700
                height = 700
                xaxis {
                    title = "Clicks per Session"
                }
                yaxis {
                    title = "Activity in terms of average sessions per day"
                }
            }
        }
    }

    private fun centroidMarkerSize(item: Centroid<Pair<Double, Double>>) =
        item.points.map { DoublePoint(it.first, it.second) }
            .maxOf { sqrt((item.center.x - it.x).pow(2.0) + (item.center.y - it.y).pow(2.0)) }
            .div(2).times(100)

    private fun clicksPerSession(sessions: List<Session>): Double = sessions
        .flatMap { session ->
            session.sessionEventsInChronologicalOrder.filter { it.action == "click" }
        }
        .count() / sessions.size.toDouble()
}
