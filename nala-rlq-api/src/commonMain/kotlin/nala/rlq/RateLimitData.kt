package nala.rlq

/**
 * A class representing rate-limit information returned by a [RateLimitTask].
 */
@ExperimentalRateLimitApi
data class RateLimitData(
        val timestamp: Long,

        val global: Boolean,
        val remaining: Int,
        val resetMillis: Long
)
