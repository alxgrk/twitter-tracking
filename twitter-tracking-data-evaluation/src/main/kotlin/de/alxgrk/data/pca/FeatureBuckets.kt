package de.alxgrk.data.pca

import de.alxgrk.data.Session
import de.alxgrk.data.plots.Interactions
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Influences the similarity.
 */
private const val ALPHA = 0.15

@Suppress("UNCHECKED_CAST")
sealed class FeatureBuckets<T>(
    val numberOfBuckets: Int,
    protected val values: MutableList<Number>
) {

    abstract fun insertIntoBucket(bucketable: T)

    abstract fun fromValues(newValues: MutableList<Number>): FeatureBuckets<*>

    protected abstract fun onFinalConversion(value: Number): Double

    override fun toString(): String = print()

    fun print(separator: String = ", "): String = values.joinToString(separator) { it.toString() }

    fun valuesAsDoubleArray(): DoubleArray = values.map { onFinalConversion(it) }.toDoubleArray()

    fun <T : FeatureBuckets<*>> map(fn: (Number) -> Number): T =
        fromValues(values.map { fn(it) }.toMutableList()) as T

    fun <T : FeatureBuckets<*>> mapIndexed(fn: (Int, Number) -> Number): T =
        fromValues(valuesAsDoubleArray().mapIndexed { index, d -> fn(index, d) }.toMutableList()) as T

    fun <T : FeatureBuckets<*>> generateSimilar(shuffled: Boolean = false): T {
        val similarValues = values.map {
            when (it) {
                is Int -> {
                    val factor = if (it == 0) 10 else it
                    val new =
                        it + Random.nextInt((factor * (-1 - ALPHA)).roundToInt(), (factor * (1 + ALPHA)).roundToInt())
                    if (new < 0) 0 else new
                }
                is Double -> {
                    val factor = if (it == 0) 1.0 else it
                    val new = it + Random.nextDouble(factor * -ALPHA, factor * ALPHA)
                    if (new < 0) 0.0 else new
                }
                else -> throw RuntimeException("only int or double")
            }
        }.let { if (shuffled) it.shuffled() else it }

        return fromValues(similarValues.toMutableList()) as T
    }

    class ZeroToLimitBuckets private constructor(
        limit: Int,
        values: MutableList<Number>,
        private val bucketSize: Int,
        private val onFinalConversion: (Number) -> Double
    ) : FeatureBuckets<Int>(limit, values) {

        constructor(limit: Int, bucketSize: Int = 1, onFinalConversion: (Number) -> Double = { it.toDouble() }) : this(
            limit,
            MutableList(limit) { 0 },
            bucketSize,
            onFinalConversion
        )

        override fun insertIntoBucket(bucketable: Int) {
            val bucketIndex = when {
                bucketable <= 0 -> 0
                bucketable >= ((numberOfBuckets * bucketSize) - 1) -> values.lastIndex
                else -> (bucketable / bucketSize)
            }

            values[bucketIndex] = values[bucketIndex].toInt() + 1
        }

        override fun fromValues(newValues: MutableList<Number>) =
            ZeroToLimitBuckets(numberOfBuckets, newValues, bucketSize, onFinalConversion)

        override fun onFinalConversion(value: Number): Double = onFinalConversion.invoke(value)
    }

    class OnehundredFourtyDays private constructor(
        values: MutableList<Number>,
        private val startDate: LocalDate
    ) : FeatureBuckets<LocalDate>(140, values) {

        constructor(startDate: LocalDate) : this(MutableList(140) { 0 }, startDate)

        private val endDateInclusive: LocalDate = startDate.plusDays(139)

        override fun insertIntoBucket(bucketable: LocalDate) {
            val bucketIndex = when {
                bucketable.isBefore(startDate) -> 0
                bucketable.isAfter(endDateInclusive) -> values.lastIndex
                else -> startDate.datesUntil(bucketable).count().toInt()
            }

            values[bucketIndex] = values[bucketIndex].toInt() + 1
        }

        override fun fromValues(newValues: MutableList<Number>) =
            OnehundredFourtyDays(newValues, startDate)

        override fun onFinalConversion(value: Number): Double = value.toDouble()
    }

    class ThirtyMinuteBucketsOverDay private constructor(
        values: MutableList<Number>
    ) : FeatureBuckets<LocalTime>(48, values) {

        constructor() : this(MutableList(48) { 0 })

        override fun insertIntoBucket(bucketable: LocalTime) {
            val firstOrSecondHalfOfHour = if (bucketable.minute < 30) 0 else 1
            val bucketIndex = bucketable.hour * 2 + firstOrSecondHalfOfHour

            values[bucketIndex] = values[bucketIndex].toInt() + 1
        }

        override fun fromValues(newValues: MutableList<Number>) =
            ThirtyMinuteBucketsOverDay(newValues)

        override fun onFinalConversion(value: Number): Double = value.toDouble() / (values.map { it.toDouble() }.sum())
    }

    class SessionTypesPerSession private constructor(
        values: MutableList<Number>
    ) : FeatureBuckets<Map.Entry<Session.SessionType, Double>>(3, values) {

        constructor() : this(MutableList(3) { 0.0 })

        override fun insertIntoBucket(bucketable: Map.Entry<Session.SessionType, Double>) {
            val bucketIndex = when (bucketable.key) {
                Session.SessionType.POSTING -> 0
                Session.SessionType.FOLLOW -> 1
                Session.SessionType.SCROLLING -> 2
            }

            values[bucketIndex] = bucketable.value
        }

        override fun fromValues(newValues: MutableList<Number>) =
            SessionTypesPerSession(newValues)

        override fun onFinalConversion(value: Number): Double = value.toDouble()
    }

    class InteractionTypesPerSession private constructor(
        values: MutableList<Number>
    ) : FeatureBuckets<Interactions>(6, values) {

        constructor() : this(MutableList(6) { 0.0 })

        override fun insertIntoBucket(bucketable: Interactions) {
            values[0] = bucketable.likesPerSession
            values[1] = bucketable.retweetsPerSession
            values[2] = bucketable.mediaClicksPerSession
            values[3] = bucketable.hashtagClicksPerSession
            values[4] = bucketable.detailViewsPerSession
            values[5] = bucketable.authorProfileClicksPerSession
        }

        override fun fromValues(newValues: MutableList<Number>) =
            InteractionTypesPerSession(newValues)

        override fun onFinalConversion(value: Number): Double = value.toDouble()
    }
}
