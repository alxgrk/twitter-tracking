package de.alxgrk.data.pca

import de.alxgrk.data.Session
import kotlin.math.roundToInt
import kotlin.random.Random

data class FeatureRow(
    val sessionsPerDayOverPeriod: FeatureBuckets.OnehundredFourtyDays,
    val sessionLengthsInBuckets: FeatureBuckets.ZeroToLimitBuckets,
    val thirtyMinuteBucketsOverDay: FeatureBuckets.ThirtyMinuteBucketsOverDay,
    val sessionTypes: FeatureBuckets.SessionTypes,
    val interactionTypes: FeatureBuckets.InteractionTypes,
    val scrolledTweetsPerSession: FeatureBuckets.ZeroToLimitBuckets
) {

    override fun toString(): String = print(",")

    fun print(separator: String) = StringBuilder()
        .append(sessionsPerDayOverPeriod.print(separator)).append(separator)
        .append(sessionLengthsInBuckets.print(separator)).append(separator)
        .append(thirtyMinuteBucketsOverDay.print(separator)).append(separator)
        .append(sessionTypes.print(separator)).append(separator)
        .append(interactionTypes.print(separator)).append(separator)
        .append(scrolledTweetsPerSession.print(separator))
        .append("\n")
        .toString()

    fun toDoubleArray() = doubleArrayOf(
        *sessionsPerDayOverPeriod.valuesAsDoubleArray(),
        *sessionLengthsInBuckets.valuesAsDoubleArray(),
        *thirtyMinuteBucketsOverDay.valuesAsDoubleArray(),
        *sessionTypes.valuesAsDoubleArray(),
        *interactionTypes.valuesAsDoubleArray(),
        *scrolledTweetsPerSession.valuesAsDoubleArray(),
    )

    fun similarRow() = FeatureRow(
        sessionsPerDayOverPeriod.generateSimilar() as FeatureBuckets.OnehundredFourtyDays,
        sessionLengthsInBuckets.generateSimilar() as FeatureBuckets.ZeroToLimitBuckets,
        thirtyMinuteBucketsOverDay.generateSimilar() as FeatureBuckets.ThirtyMinuteBucketsOverDay,
        sessionTypes.generateSimilar() as FeatureBuckets.SessionTypes,
        interactionTypes.generateSimilar() as FeatureBuckets.InteractionTypes,
        scrolledTweetsPerSession.generateSimilar() as FeatureBuckets.ZeroToLimitBuckets
    )
}

object AnomalousRows {

    enum class Profiles {
        VeryActiveButNoInteractions {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    sessionsPerDayOverPeriod = FeatureBuckets.OnehundredFourtyDays(Configuration.startDateOfPeriod)
                        .fromValues(
                            MutableList(140) {
                                val rowAverage = row.sessionsPerDayOverPeriod.valuesAsDoubleArray().average().toInt()
                                Random.nextInt(rowAverage, (rowAverage + 1) * 2)
                            }
                        ),
                    thirtyMinuteBucketsOverDay = FeatureBuckets.ThirtyMinuteBucketsOverDay()
                        .fromValues(
                            MutableList(48) {
                                val rowAverage = row.thirtyMinuteBucketsOverDay.valuesAsDoubleArray().average().toInt()
                                if (Random.nextBoolean()) Random.nextInt(rowAverage, (rowAverage + 1) * 2) else 0
                            }
                        ),
                    interactionTypes = FeatureBuckets.InteractionTypes()
                )
        },
        VeryInactiveButLotsOfInteractions {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    sessionsPerDayOverPeriod = FeatureBuckets.OnehundredFourtyDays(Configuration.startDateOfPeriod)
                        .fromValues(
                            MutableList(140) {
                                Random.nextInt(1)
                            }
                        ),
                    thirtyMinuteBucketsOverDay = FeatureBuckets.ThirtyMinuteBucketsOverDay()
                        .fromValues(
                            MutableList(48) {
                                Random.nextInt(1)
                            }
                        ),
                    interactionTypes = FeatureBuckets.InteractionTypes()
                        .fromValues(
                            MutableList(6) {
                                val rowAverage = row.interactionTypes.valuesAsDoubleArray().average().toInt()
                                Random.nextInt(rowAverage, (rowAverage + 1) * 2)
                            }
                        )
                )
        },
        TooManyTooShortSessions {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    sessionLengthsInBuckets = FeatureBuckets.ZeroToLimitBuckets(Configuration.numberOfSessionLengthBuckets)
                        .fromValues(
                            row.sessionLengthsInBuckets.valuesAsDoubleArray()
                                .map {
                                    if (it < 2) Random.nextInt(it.toInt(), (it.toInt() + 1) * 2) else 0
                                }
                                .toMutableList()
                        ),
                    thirtyMinuteBucketsOverDay = FeatureBuckets.ThirtyMinuteBucketsOverDay()
                        .fromValues(
                            MutableList(48) {
                                val rowAverage = row.thirtyMinuteBucketsOverDay.valuesAsDoubleArray().average().toInt()
                                Random.nextInt(rowAverage, (rowAverage + 1) * 2)
                            }
                        )
                )
        },
        NoScrollingSessionsSoNoScrolledTweets {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    sessionTypes = FeatureBuckets.SessionTypes()
                        .apply {
                            val max = row.sessionTypes.valuesAsDoubleArray().maxOrNull() ?: row.sessionsPerDayOverPeriod.valuesAsDoubleArray().sum()
                            mapOf(
                                Session.SessionType.SCROLLING to 0,
                                Session.SessionType.POSTING to (max / 2).toInt(),
                                Session.SessionType.FOLLOW to (max / 2).toInt()
                            ).entries.forEach(::insertIntoBucket)
                        },
                    scrolledTweetsPerSession = FeatureBuckets.ZeroToLimitBuckets(
                        Configuration.numberOfScrolledTweetsBuckets,
                        Configuration.sizeOfScrolledTweetsBuckets
                    )
                )
        },
        TooFewScrollingSessionsAndFewScrolledTweets {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    sessionTypes = FeatureBuckets.SessionTypes()
                        .apply {
                            val max = row.sessionTypes.valuesAsDoubleArray().maxOrNull() ?: row.sessionsPerDayOverPeriod.valuesAsDoubleArray().sum()
                            val fraction = max - Random.nextDouble(max * 0.9, max * 0.99 + 1).roundToInt()
                            val rest = (max - fraction) / 2
                            mapOf(
                                Session.SessionType.SCROLLING to fraction.toInt(),
                                Session.SessionType.POSTING to rest.toInt(),
                                Session.SessionType.FOLLOW to rest.toInt()
                            ).entries.forEach(::insertIntoBucket)
                        },
                    scrolledTweetsPerSession = FeatureBuckets.ZeroToLimitBuckets(
                        Configuration.numberOfScrolledTweetsBuckets,
                        Configuration.sizeOfScrolledTweetsBuckets
                    ).fromValues(
                        MutableList(Configuration.numberOfScrolledTweetsBuckets) {
                            if (it < 5) Random.nextInt(
                                5
                            ) else 0
                        }
                    )
                )
        },
        TooManyLikesAndRetweets {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    interactionTypes = FeatureBuckets.InteractionTypes()
                        .fromValues(
                            MutableList(6) {
                                if (it < 2)
                                    (row.interactionTypes.valuesAsDoubleArray()[it] * Random.nextInt(2, 7)).toInt()
                                else
                                    Random.nextInt(1)
                            }
                        )
                )
        };

        abstract fun modify(row: FeatureRow): FeatureRow

        companion object {
            fun randomProfile() = values().random()
        }
    }

    fun generate(reference: List<FeatureRow>, expectedAnomalousRows: Int): List<FeatureRow> =
        (0..expectedAnomalousRows).map {
            val normalRow = reference.random().similarRow()
            val randomProfile = Profiles.randomProfile()
            val anomalous = randomProfile.modify(normalRow)
            anomalous
        }
}
