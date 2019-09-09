package nala.rlq.retry

import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.RateLimitResult
import nala.rlq.RateLimitTask

/**
 * An implementation of [Retry] that always returns `true`.
 */
@ExperimentalRateLimitApi
object InfiniteRetry : Retry {

    override fun shouldRetry(task: RateLimitTask<*>, result: RateLimitResult.Failure<*>) = true

}
