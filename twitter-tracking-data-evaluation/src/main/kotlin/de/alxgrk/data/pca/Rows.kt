package de.alxgrk.data.pca

import kotlin.math.roundToInt
import kotlin.random.Random

data class FeatureRow(
    val sessionsPerDayOverPeriod: FeatureBuckets.OnehundredFourtyDays,
    val sessionLengthsInBuckets: FeatureBuckets.ZeroToLimitBuckets,
    val thirtyMinuteBucketsOverDay: FeatureBuckets.ThirtyMinuteBucketsOverDay,
    val sessionTypesPerSession: FeatureBuckets.SessionTypesPerSession,
    val interactionTypesPerSession: FeatureBuckets.InteractionTypesPerSession,
    val scrolledTweetsPerSession: FeatureBuckets.ZeroToLimitBuckets
) {

    override fun toString(): String = print(",")

    fun print(separator: String) = StringBuilder()
        .append(sessionsPerDayOverPeriod.print(separator)).append(separator)
        .append(sessionLengthsInBuckets.print(separator)).append(separator)
        .append(thirtyMinuteBucketsOverDay.print(separator)).append(separator)
        .append(sessionTypesPerSession.print(separator)).append(separator)
        .append(interactionTypesPerSession.print(separator)).append(separator)
        .append(scrolledTweetsPerSession.print(separator))
        .append("\n")
        .toString()

    fun toDoubleArray() = doubleArrayOf(
        *sessionsPerDayOverPeriod.valuesAsDoubleArray(),
        *sessionLengthsInBuckets.valuesAsDoubleArray(),
        *thirtyMinuteBucketsOverDay.valuesAsDoubleArray(),
        *sessionTypesPerSession.valuesAsDoubleArray(),
        *interactionTypesPerSession.valuesAsDoubleArray(),
        *scrolledTweetsPerSession.valuesAsDoubleArray(),
    )

    fun similarRow() = FeatureRow(
        sessionsPerDayOverPeriod.generateSimilar(shuffled = true),
        sessionLengthsInBuckets.generateSimilar(),
        thirtyMinuteBucketsOverDay.generateSimilar(),
        sessionTypesPerSession.generateSimilar(),
        interactionTypesPerSession.generateSimilar(),
        scrolledTweetsPerSession.generateSimilar()
    )
}

object AnomalousRows {

    enum class Profiles {
        /**
         * Might be for crawling and indirectly promoting.
         */
        VeryActiveButNoInteractions {
            override fun modify(row: FeatureRow): FeatureRow {
                val minSessionsPerDay = Random.nextInt(3, 10)
                val sessionsPerDayOverPeriod = row.sessionsPerDayOverPeriod.map<FeatureBuckets.OnehundredFourtyDays> {
                    Random.nextInt(minSessionsPerDay, minSessionsPerDay * 2)
                }
                val sessions = sessionsPerDayOverPeriod.valuesAsDoubleArray().sum()

                fun distributeOverSessions(divisor: Int): Int = sessions.roundToInt() / divisor

                return FeatureRow(
                    sessionsPerDayOverPeriod,
                    row.sessionLengthsInBuckets.map { distributeOverSessions(row.sessionLengthsInBuckets.numberOfBuckets) },
                    row.thirtyMinuteBucketsOverDay.map { distributeOverSessions(row.thirtyMinuteBucketsOverDay.numberOfBuckets) },
                    row.sessionTypesPerSession.mapIndexed { i, _ ->
                        if (i == 2) // scrolling
                            distributeOverSessions(1) / sessions
                        else 0
                    },
                    FeatureBuckets.InteractionTypesPerSession(),
                    row.scrolledTweetsPerSession.mapIndexed { index, _ ->
                        if (index > (row.scrolledTweetsPerSession.numberOfBuckets / 2)) distributeOverSessions(row.scrolledTweetsPerSession.numberOfBuckets / 2) else 0
                    }
                )
            }
        },
        VeryActiveAndLotsOfInteractionsButNoScrolls {
            override fun modify(row: FeatureRow): FeatureRow {
                val minSessionsPerDay = Random.nextInt(3, 10)
                val sessionsPerDayOverPeriod = row.sessionsPerDayOverPeriod.map<FeatureBuckets.OnehundredFourtyDays> {
                    Random.nextInt(minSessionsPerDay, minSessionsPerDay * 2)
                }
                val sessions = sessionsPerDayOverPeriod.valuesAsDoubleArray().sum()

                fun distributeOverSessions(divisor: Int): Int = sessions.roundToInt() / divisor

                return FeatureRow(
                    sessionsPerDayOverPeriod,
                    row.sessionLengthsInBuckets.mapIndexed { i, _ ->
                        if (i == 0) // only very short sessions
                            distributeOverSessions(1)
                        else 0
                    },
                    row.thirtyMinuteBucketsOverDay.map { distributeOverSessions(row.thirtyMinuteBucketsOverDay.numberOfBuckets) },
                    row.sessionTypesPerSession.mapIndexed { i, _ ->
                        if (i < 2) // posting and follow
                            distributeOverSessions(2) / sessions
                        else 0
                    },
                    row.interactionTypesPerSession.mapIndexed { i, _ ->
                        if (i < 2) // likes and retweets
                            distributeOverSessions(2) * Random.nextInt(1, 3) / sessions
                        else 0
                    },
                    row.scrolledTweetsPerSession.map { 0 }
                )
            }
        },
        TooManyLikesAndRetweets {
            override fun modify(row: FeatureRow): FeatureRow =
                row.copy(
                    interactionTypesPerSession = row.interactionTypesPerSession.mapIndexed { i, _ ->
                        // likes and retweets
                        if (i < 2) {
                            val sessions = row.sessionsPerDayOverPeriod.valuesAsDoubleArray().sum()
                            sessions * Random.nextInt(500, 1000) / sessions
                        } else
                            Random.nextInt(1)
                    }
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
