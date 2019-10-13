package nala.rlq

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import nala.common.internal.Disposable
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
interface RateLimitQueue : Disposable {

    /**
     * Submits the [task] to this queue with the specified [retry] and [backoff] strategies,
     * suspends until completion, and returns its result or throws the corresponding exception if the task failed.
     *
     * This suspending function is cancellable.
     * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting,
     * this function will immediately resume with [CancellationException]
     * and the task will be cancelled and removed from this queue.
     *
     * @param retry the retry instance.
     *  If it is `null`, the task will never be resubmitted.
     * @param backoff the backoff strategy.
     *  If it is `null`, in the event of a retry the task will be resubmitted immediately.
     *
     * @return the result of the [task].
     */
    suspend fun <TData> submit(task: RateLimitTask<TData>, retry: Retry? = null, backoff: Backoff? = null): TData

    /**
     * Submits the [task] to this queue with the specified [retry] and [backoff] strategies.
     * The returned [Deferred] may be cancelled to cancel the task and remove it from this queue.
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
     * Closes this rate-limit queue and cancels all queued tasks.
     *
     * This function is idempotent;
     * multiple attempts to dispose the same queue have no effect,
     * unless documented as such.
     */
    override fun dispose()

}
