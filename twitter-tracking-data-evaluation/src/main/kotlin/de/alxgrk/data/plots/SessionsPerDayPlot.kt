package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import hep.dataforge.values.Value
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.layout
import kscience.plotly.models.Shape
import kscience.plotly.models.ShapeType
import kscience.plotly.models.TextPosition
import org.nield.kotlinstatistics.countBy
import java.time.temporal.ChronoField

class SessionsPerDayPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val sessionsPerDay = sessionsPerUserId
            .flatMap { (_, sessions) -> sessions.byDay() }
            .sorted()

        val average = sessionsPerDay.average()

        return Plotly.plot {

            val counted = sessionsPerDay.countBy()
            bar {
                x.set(counted.keys)
                y.set(counted.values)
                textsList = counted.values.map { it.toString() }
                textposition = TextPosition.outside
                marker {
                    color("rgb(107, 174, 214)")
                }
            }

            layout {
                title = "Number of Sessions per Day"
                xaxis {
                    title = "Sessions on one Day"
                }
                yaxis {
                    gridwidth = 2
                    title = "Occurrences"
                }
                shapes = listOf(verticalLine(average, sessionsPerDay), verticalLine(average, sessionsPerDay))
                bargap = 0.05
            }
        }
    }

    private fun verticalLine(
        average: Double,
        sessionsPerDay: List<Int>
    ) = Shape {
        type = ShapeType.line
        x0 = Value.of(average)
        y0 = Value.of(0)
        x1 = Value.of(average)
        y1 = Value.of(sessionsPerDay.countBy { it }.values.maxOrNull())
        line {
            color("rgb(210, 124, 107)")
            width = 3.5
        }
    }

    companion object {

        fun List<Session>.byDay() = this
            .groupBy { it.sessionStartEvent.zonedTimestamp().get(ChronoField.DAY_OF_YEAR) }
            .map { (_, sessions) -> sessions.size }
    }
}
