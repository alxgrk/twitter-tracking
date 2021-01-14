package de.alxgrk.data.pca

import de.alxgrk.data.Session
import de.alxgrk.data.plots.Interactions
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Influences the similarity: should be higher than 0, but probably less than 1.
 */
private const val ALPHA = 0.15

sealed class FeatureBuckets<T>(
    val numberOfBuckets: Int,
    protected val values: MutableList<Int>
) {

    abstract fun insertIntoBucket(bucketable: T)

    abstract fun fromValues(newValues: MutableList<Int>): FeatureBuckets<*>

    override fun toString(): String = print()

    fun print(separator: String = ", "): String = values.joinToString(separator) { it.toString() }

    fun valuesAsDoubleArray(): DoubleArray = values.map { it.toDouble() }.toDoubleArray()

    fun generateSimilar(): FeatureBuckets<*> {
        val similarValues = values.map {
            val factor = if (it == 0) 1 else it
            val new = it + Random.nextInt((factor * (-1 - ALPHA)).roundToInt(), (factor * (1 + ALPHA)).roundToInt())
            if (new < 0) 0 else new
        }

        return fromValues(similarValues.toMutableList())
    }

    class ZeroToLimitBuckets private constructor(
        limit: Int,
        values: MutableList<Int>,
        private val bucketSize: Int
    ) : FeatureBuckets<Int>(limit, values) {

        constructor(limit: Int, bucketSize: Int = 1) : this(limit, MutableList(limit) { 0 }, bucketSize)

        override fun insertIntoBucket(bucketable: Int) {
            val bucketIndex = when {
                bucketable <= 0 -> 0
                bucketable >= ((numberOfBuckets * bucketSize) - 1) -> values.lastIndex
                else -> (bucketable / bucketSize)
            }

            values[bucketIndex] = values[bucketIndex] + 1
        }

        override fun fromValues(newValues: MutableList<Int>) =
            ZeroToLimitBuckets(numberOfBuckets, newValues, bucketSize)
    }

    class OnehundredFourtyDays private constructor(
        values: MutableList<Int>,
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

            values[bucketIndex] = values[bucketIndex] + 1
        }

        override fun fromValues(newValues: MutableList<Int>) =
            OnehundredFourtyDays(newValues, startDate)
    }

    class ThirtyMinuteBucketsOverDay private constructor(
        values: MutableList<Int>
    ) : FeatureBuckets<LocalTime>(48, values) {

        constructor() : this(MutableList(48) { 0 })

        override fun insertIntoBucket(bucketable: LocalTime) {
            val firstOrSecondHalfOfHour = if (bucketable.minute < 30) 0 else 1
            val bucketIndex = bucketable.hour * 2 + firstOrSecondHalfOfHour

            values[bucketIndex] = values[bucketIndex] + 1
        }

        override fun fromValues(newValues: MutableList<Int>) =
            ThirtyMinuteBucketsOverDay(newValues)
    }

    class SessionTypes private constructor(
        values: MutableList<Int>
    ) : FeatureBuckets<Map.Entry<Session.SessionType, Int>>(3, values) {

        constructor() : this(MutableList(3) { 0 })

        override fun insertIntoBucket(bucketable: Map.Entry<Session.SessionType, Int>) {
            val bucketIndex = when (bucketable.key) {
                Session.SessionType.POSTING -> 0
                Session.SessionType.FOLLOW -> 1
                Session.SessionType.SCROLLING -> 2
            }

            values[bucketIndex] = bucketable.value
        }

        override fun fromValues(newValues: MutableList<Int>) =
            SessionTypes(newValues)
    }

    class InteractionTypes private constructor(
        values: MutableList<Int>
    ) : FeatureBuckets<Interactions>(6, values) {

        constructor() : this(MutableList(6) { 0 })

        override fun insertIntoBucket(bucketable: Interactions) {
            values[0] = bucketable.likes
            values[1] = bucketable.retweets
            values[2] = bucketable.mediaClicks
            values[3] = bucketable.hashtagClicks
            values[4] = bucketable.detailViews
            values[5] = bucketable.authorProfileClicks
        }

        override fun fromValues(newValues: MutableList<Int>) =
            InteractionTypes(newValues)
    }
}
