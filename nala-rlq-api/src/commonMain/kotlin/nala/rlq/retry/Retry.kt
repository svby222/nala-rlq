package nala.rlq.retry

import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.RateLimitQueue
import nala.rlq.RateLimitResult
import nala.rlq.RateLimitTask

/**
 * An interface for stateful objects encapsulating the retry state of [RateLimitTask]s submitted to a
 * [rate-limit queue][RateLimitQueue].
 *
 * This interface contains a single method, [shouldRetry],
 * which accepts a task and its failure data and returns whether the task should be resubmitted to the queue.
 *
 * Implementations of this interface may be stateful.
 */
@ExperimentalRateLimitApi
interface Retry {

    /**
     * Called when a task has been submitted for execution, has been executed, and has failed.
     * @param result the failed task result (an instance of either [RateLimitResult.Failure])
     * @return whether the task should be resubmitted to the queue.
     */
    fun shouldRetry(task: RateLimitTask<*>, result: RateLimitResult.Failure<*>): Boolean

}
