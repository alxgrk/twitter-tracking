package de.alxgrk.data.pca

import hep.dataforge.meta.Meta
import hep.dataforge.meta.configure
import hep.dataforge.meta.set
import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.layout
import kscience.plotly.models.Scatter
import smile.projection.PCA

class ScreePlot {

    fun create(pca: PCA): Plot {
        val xLabels = (0..pca.varianceProportion.size).map { it }

        val all = Scatter {
            x.set(xLabels)
            y.set(pca.varianceProportion)
            name = ""
        }

        val top20 = Scatter {
            x.set(xLabels.slice(0..19))
            y.set(pca.varianceProportion.sliceArray(0..19))
            configure {
                set("xaxis", "x2")
                set("yaxis", "y2")
            }
        }

        return Plotly.plot {
            traces(all, top20)

            layout {
                title = "Scree Plot"
                xaxis {
                    title = "Principal Components"
                }
                yaxis {
                    title = "Fraction of total variance"
                }
                configure {
                    set(
                        "xaxis2",
                        Meta {
                            "domain".put(doubleArrayOf(0.1, 0.95))
                            "anchor".put("y2")
                        }
                    )
                    set(
                        "yaxis2",
                        Meta {
                            "domain".put(doubleArrayOf(0.2, 0.95))
                            "anchor".put("x2")
                        }
                    )
                }
                showlegend = false
            }
        }
    }
}
