package nala.rlq.backoff

import nala.rlq.ExperimentalRateLimitApi

/**
 * An implementation of [Backoff] that returns a sequence of backoff times starting at [initialValue],
 * increasing by a specified [factor] until reaching [maxValue].
 *
 * @param initialValue the initial backoff value in milliseconds
 * @param factor the multiplicative factor
 * @param maxValue the maximum backoff value in milliseconds
 */
@ExperimentalRateLimitApi
class ExponentialBackoff(val initialValue: Long, val factor: Int = 2, val maxValue: Long = Long.MAX_VALUE) : Backoff {

    init {
        require(factor > 1) { "factor must be > 1" }
        require(initialValue > 0) { "initialValue must be positive" }
    }

    override fun generateSequence() = sequence {
        var value = initialValue
        while (true) {
            yield(value)

            val lastAcceptable = maxValue / factor
            if (lastAcceptable == value) {
                val next = value * factor
                if (next < maxValue) yield(next)
                break
            } else if (lastAcceptable < value) break
            else value *= factor
        }

        while (true) yield(maxValue)
    }

}
