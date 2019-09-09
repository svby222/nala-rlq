package nala.rlq

/**
 * A class representing rate-limit information returned by a successful or rate-limited [task][RateLimitTask].
 */
@ExperimentalRateLimitApi
data class RateLimitData(
        val global: Boolean,
        val remaining: Int,
        val resetMillis: Long
)
