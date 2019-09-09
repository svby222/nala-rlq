package nala.rlq

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import nala.rlq.backoff.Backoff
import nala.rlq.retry.Retry

/**
 * An interface representing a [task][RateLimitTask] executor that respects rate limits and delays execution accordingly.
 *
 * Rate limit updates should occur dynamically,
 * i.e. if a task has been queued for execution and the queue receives newer rate-limit information
 * that would disallow the task from completing at the scheduled time,
 * the rate limit should be reevaluated and the task rescheduled at a later time.
 */
@ExperimentalRateLimitApi
interface RateLimitQueue {

    /**
     * Submits the scheduled [task] to this queue with the specified [retry] and [backoff] strategies.
     * The returned [Deferred] may be cancelled to remove the task from this queue.
     *
     * @param retry the retry instance.
     *  If it is `null`, the task will never be resubmitted.
     * @param backoff the backoff strategy.
     *  If it is `null`, in the event of a retry the task will be resubmitted immediately.
     *
     * @return a [Deferred] job holding the future result of the [task].
     */
    fun <TData> submitAsync(task: RateLimitTask<TData>, retry: Retry? = null, backoff: Backoff? = null): Deferred<TData>

    /**
     * Submits the scheduled [task] to this queue with the specified [retry] and [backoff] strategies,
     * and suspends until the task is completed.
     *
     * This suspending function is cancellable.
     * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting,
     * this function will immediately resume with [CancellationException] and the task will be removed from this queue.
     *
     * @param retry the retry instance.
     *  If it is `null`, the task will never be resubmitted.
     * @param backoff the backoff strategy.
     *  If it is `null`, in the event of a retry the task will be resubmitted immediately.
     *
     * @return the result of the [task].
     * @throws Exception the exception thrown by the [task],
     *  if it fails and the retry strategy does not allow it to be resubmitted.
     *
     * @see submitAsync
     */
    suspend fun <TData> submit(task: RateLimitTask<TData>, retry: Retry? = null, backoff: Backoff? = null): TData

    /**
     * Closes this rate-limit queue and cancels all queued tasks.
     */
    fun dispose()

}
