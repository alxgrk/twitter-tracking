package de.alxgrk.data.plots

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.layout
import kscience.plotly.models.TextPosition

class UnknownSelectorsPlot : Chart {

    override fun Analyse.createPlot(sessionsPerUserId: Map<UserId, List<Session>>): Plot {

        val unknownSelectorClicks = sessionsPerUserId.values.asSequence()
            .flatMap { sessions -> sessions.map { it.sessionEventsInChronologicalOrder }.flatten() }
            .filter { it.target == null && it.action == "click" }
            .groupBy { it.selector }
            .mapValues { (_, v) -> v.size }
            .toList()
            .sortedByDescending { (_, v) -> v }
            .take(20)

        return Plotly.plot {

            bar {
                x.set(unknownSelectorClicks.map { it.first.substringAtMost25() })
                y.set(unknownSelectorClicks.map { it.second })
                textsList = unknownSelectorClicks.map { it.second.toString() }
                textposition = TextPosition.auto
                marker {
                    color("rgb(107, 174, 214)")
                }
            }

            layout {
                title = "Unknown Selectors"
                margin { b = 200 }
                xaxis {
                    title = "Selectors"
                    bargap = 0.5
                    tickfont {
                        size = 8.5
                    }
                }
                yaxis {
                    title = "Occurences"
                }
            }
        }
    }

    private fun String?.substringAtMost25(): String? {
        if (this == null)
            return null

        val target =
            if (this.startsWith("com.twitter.android:id/"))
                this.replace("com.twitter.android:id/", "")
                    .plus("/android")
            else this

        return if (target.length < 25) target else "…" + target.substring((target.length - 24) until (target.length))
    }
}
