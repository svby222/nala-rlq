package nala.rlq.backoff

import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.RateLimitTask
import nala.rlq.RateLimitQueue
import nala.rlq.retry.Retry

/**
 * An interface representing a sequence of "backoff" times, delays between subsequent attempts to resubmit a
 * [task][RateLimitTask] to a [RateLimitQueue].
 *
 * This interface contains a single method, [generateSequence],
 * which returns a [Sequence] of backoff times in milliseconds.
 *
 * Implementations of this interface should be stateless.
 *
 * @see generateSequence
 * @see Retry
 */
@ExperimentalRateLimitApi
interface Backoff {

    /**
     * @return a [Sequence] of backoff times in milliseconds.
     */
    fun generateSequence(): Sequence<Long>

}
