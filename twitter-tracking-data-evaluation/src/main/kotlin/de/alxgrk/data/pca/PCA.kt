package de.alxgrk.data.pca

import de.alxgrk.data.Analyse
import de.alxgrk.data.Session
import de.alxgrk.data.UserId
import de.alxgrk.data.durationInSeconds
import de.alxgrk.data.pca.Configuration.numberOfScrolledTweetsBuckets
import de.alxgrk.data.pca.Configuration.numberOfSessionLengthBuckets
import de.alxgrk.data.pca.Configuration.sizeOfScrolledTweetsBuckets
import de.alxgrk.data.pca.Configuration.startDateOfPeriod
import de.alxgrk.data.plots.Interactions
import de.alxgrk.data.plots.TweetsPerSessionPlot.Companion.scrolledTweetsOrNull
import de.alxgrk.data.plots.store
import de.alxgrk.data.sessionTypes
import kscience.plotly.Plotly
import kscience.plotly.models.Scatter
import kscience.plotly.scatter
import smile.math.matrix.Matrix
import smile.projection.PCA
import smile.projection.pca
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.streams.toList

object Configuration {
    val startDateOfPeriod: LocalDate = LocalDate.of(2020, 9, 1)
    const val numberOfSessionLengthBuckets = 90
    const val numberOfScrolledTweetsBuckets = 50
    const val sizeOfScrolledTweetsBuckets = 10
}

class PCA(sessionsPerUserId: Map<UserId, List<Session>>) {

    val file = File("./pca-matrix-${System.currentTimeMillis()}.csv")

    private val featureMatrix: Map<UserId, FeatureRow> = sessionsPerUserId.mapValues { (_, sessions) ->
        val sessionsPerDayOverPeriod = sessions
            .map { LocalDate.from(it.sessionStartEvent.zonedTimestamp()) }
            .foldTo(FeatureBuckets.OnehundredFourtyDays(startDateOfPeriod))

        val sessionLengths = sessions
            .map { session -> (session.durationInSeconds() / 60).toInt() }
            .foldTo(FeatureBuckets.ZeroToLimitBuckets(numberOfSessionLengthBuckets))

        val thirtyMinuteBucketsOverDay = sessions
            .map { session -> LocalTime.from(session.sessionStartEvent.zonedTimestamp()) }
            .foldTo(FeatureBuckets.ThirtyMinuteBucketsOverDay())

        val sessionTypesPerSession = sessions.sessionTypes()
            .entries
            .foldTo(FeatureBuckets.SessionTypes())

        val interactionTypesPerSession = sessions
            .fold(Interactions(sessions.size)) { i, session ->
                session.sessionEventsInChronologicalOrder.forEach {
                    i.increaseCountFor(it.target)
                }
                i
            }
            .let { FeatureBuckets.InteractionTypes().apply { insertIntoBucket(it) } }

        val scrolledTweetsPerSession = sessions
            .mapNotNull { session -> session.scrolledTweetsOrNull() }
            .foldTo(
                FeatureBuckets.ZeroToLimitBuckets(numberOfScrolledTweetsBuckets, sizeOfScrolledTweetsBuckets)
            )

        FeatureRow(
            sessionsPerDayOverPeriod,
            sessionLengths,
            thirtyMinuteBucketsOverDay,
            sessionTypesPerSession,
            interactionTypesPerSession,
            scrolledTweetsPerSession
        )
    }

    private fun <E, B : FeatureBuckets<E>> Collection<E>.foldTo(featureBuckets: B): B =
        fold(featureBuckets) { buckets, elements ->
            buckets.apply { insertIntoBucket(elements) }
        }

    private fun Analyse.transformMatrix(): Array<DoubleArray> {
        val withSimilarRows = featureMatrix
            .flatMap { (_, r) ->
                val rows = mutableListOf(r)
                repeat(10000) {
                    val similar = r.similarRow()
                    rows.add(similar)
                    trace { r.print("\t\t") }
                    trace { similar.print("\t\t") }
                }
                rows
            }

        val expectedAnomalousRows = (withSimilarRows.size * Random.nextDouble(0.02, 0.03))
        val anomalousRows = AnomalousRows.generate(withSimilarRows, expectedAnomalousRows.toInt())
        debug("Generated ${anomalousRows.size} anomalous rows from uniformly distributed fake user profiles.")

        return Stream.concat(withSimilarRows.stream(), anomalousRows.stream())
            .map { it.toDoubleArray() }
            .toList()
            .shuffled()
            .onEach { file.appendText("${it.joinToString()}\n") }
            .toTypedArray()
    }

    // private fun readMatrix(pcaInputMatrix: Path): Array<DoubleArray> =
    //     pcaInputMatrix.toFile().readLines()
    //         .map { line -> doubleArrayOf(*(line.split(", ").map { it.toDouble() }.toDoubleArray())) }
    //         .toTypedArray()

    fun Analyse.execute() {
        val inputMatrix = transformMatrix() // if (`pca-input` == null) transformMatrix() else readMatrix(`pca-input`!!)

        val result = computePCs(inputMatrix)

        val chartFile = File("./${this@PCA::class.simpleName!!}.${if (export) "svg" else "html"}")
        store(ScreePlot().create(result), chartFile)
    }

    private fun Analyse.computePCs(inputMatrix: Array<DoubleArray>): PCA {
        val result = pca(inputMatrix)

        (3 until 7).forEach { kTopPCs ->
            info("Running evaluation for top $kTopPCs PCs.")
            debug { "Variances: " + result.variance.joinToString() }
            debug { "Variance proportions: " + result.cumulativeVarianceProportion.joinToString() }
            info("Top $kTopPCs PCs account for ${result.cumulativeVarianceProportion[kTopPCs - 1] * 100} % of the data's variance.")
            val pc1 = result.loadings.col(0)
            debug { "v1 (vector of PC1): norm=${pc1.norm2()} of vector ${pc1.joinToString()}" }

            val matrixP = result.loadings.col(*((0 until kTopPCs).toList().toIntArray()))
            val matrixC = matrixP.mm(matrixP.transpose())

            fun calcSquaredPredictionError(x: DoubleArray): Double {

                // threshold might be defined as 3*standardDeviation from the mean

                val xInNormalSubspace = matrixC.mv(x)
                val xInResidualSubspace = x - xInNormalSubspace
                debug("x in residual subspace: ${xInResidualSubspace.joinToString()}")
                val l2Norm = xInResidualSubspace.norm2()
                debug("L2 norm is: $l2Norm")
                val spe = l2Norm.pow(2)
                debug("Squared prediction error (L2 norm ^2) is: $spe")
                return spe
            }

            val numberOfUsers = 1000
            val speOfNormalUsers = (0 until numberOfUsers)
                .map {
                    val normalRow = featureMatrix.values.random().similarRow()
                    calcSquaredPredictionError(normalRow.toDoubleArray())
                }
            val speOfAnomalousUsers = AnomalousRows.Profiles.values().map { profile ->
                profile to (0 until numberOfUsers)
                    .map {
                        val anomalousRow = profile.modify(featureMatrix.values.random().similarRow())
                        calcSquaredPredictionError(anomalousRow.toDoubleArray())
                    }
            }.toMap()

            val falsePositiveRates = mutableMapOf<Int, Double>()
            val truePositiveRates = mutableMapOf<AnomalousRows.Profiles, MutableMap<Int, Double>>()
            (0 until 1250 step 25).map { threshold ->
                val fps = speOfNormalUsers.map { it >= threshold }.count { it }
                falsePositiveRates[threshold] = fps / numberOfUsers.toDouble()

                speOfAnomalousUsers.forEach { (profile, values) ->
                    val tps = values.map { it >= threshold }.count { it }
                    truePositiveRates.computeIfAbsent(profile) { mutableMapOf() }
                    truePositiveRates[profile]?.put(threshold, tps / numberOfUsers.toDouble())
                }
            }

            plotRates(kTopPCs, falsePositiveRates, truePositiveRates)

        }

        return result
    }

    private fun Analyse.plotRates(
        kTopPCs: Int,
        falsePositiveRates: MutableMap<Int, Double>,
        truePositiveRates: MutableMap<AnomalousRows.Profiles, MutableMap<Int, Double>>
    ) {
        val fpScatter = Scatter {
            x.set(falsePositiveRates.keys)
            y.set(falsePositiveRates.values)
            name = "FP-Rate by threshold"
        }

        val tpScatters = truePositiveRates.map { (k, v) ->
            Scatter {
                x.set(v.keys)
                y.set(v.values)
                name = "TP-Rate of Profile ${k.name} by threshold"
            }
        }
            .toTypedArray()

        store(
            Plotly.plot {
                traces(fpScatter, *tpScatters)
            },
            File("./PCA-Rates-top${kTopPCs}PCs.html")
        )
    }

    private fun Analyse.plotU(inputMatrix: Array<DoubleArray>, pc1: DoubleArray?, i: Int) {
        val Xv = Matrix(inputMatrix).mv(pc1)
        val XvNorm = Xv.norm2()
        val projectionU = Xv.map { it / XvNorm }
        store(
            Plotly.plot {
                scatter {
                    this.x.set(projectionU.indices)
                    this.y.set(projectionU)
                }
            },
            File("./u$i.html")
        )
    }
}

private operator fun DoubleArray.minus(other: DoubleArray): DoubleArray =
    if (this.size != other.size)
        throw IllegalArgumentException("Can't subtract vectors of different dimensions.")
    else mapIndexed { index, v1 -> v1 - other[index] }
        .toDoubleArray()

private fun DoubleArray.norm2(): Double = sqrt(sumByDouble { abs(it).pow(2) })
