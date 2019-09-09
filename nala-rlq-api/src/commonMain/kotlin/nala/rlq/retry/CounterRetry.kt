package nala.rlq.retry

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.RateLimitResult
import nala.rlq.RateLimitTask

/**
 * An implementation of [Retry] that returns `true` [count] amount of times.
 * Subsequent invocations of [shouldRetry] will return `false`.
 *
 * @param count the maximum amount of times to retry the task
 */
@ExperimentalRateLimitApi
class CounterRetry(count: Int) : Retry {

    init {
        require(count > 0)
    }

    private val remaining = atomic(count)

    override fun shouldRetry(task: RateLimitTask<*>, result: RateLimitResult.Failure<*>): Boolean {
        return remaining.getAndUpdate { if (it <= 0) it else it - 1 } != 0
    }

}
