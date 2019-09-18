package nala.rlq.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.RateLimitQueue
import nala.rlq.RateLimitTask
import nala.rlq.SuspendingTask

/**
 * An interface representing a task dispatcher to which [SuspendingTask]s can be submitted for eventual execution.
 * This may be used by [RateLimitQueue]s to submit [RateLimitTask]s for execution.
 *
 * For a simple concurrent task dispatcher with a fixed amount of worker coroutines, see [WorkerPoolDispatcher].
 *
 * @see WorkerPoolDispatcher
 * @see RateLimitQueue
 */
@ExperimentalRateLimitApi
internal interface TaskDispatcher : Disposable {

    /**
     * Submits the [task] for execution to this dispatcher, suspends until completion,
     * and returns its result or throws the corresponding exception if the task failed.
     *
     * This suspending function is cancellable.
     * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting,
     * this function will immediately resume with [CancellationException] and the task will be cancelled.
     */
    suspend fun <T> submit(task: SuspendingTask<T>): T

    /**
     * Submits the [task] for execution to this dispatcher.
     * The returned [Deferred] may be cancelled to cancel the task.
     *
     * @return a [Deferred] job holding the future result of the [task].
     *
     * @see submit
     */
    fun <T> submitAsync(task: SuspendingTask<T>): Deferred<T>

    /**
     * Closes this task dispatcher and cancels all tasks.
     *
     * This function is idempotent;
     * multiple attempts to dispose the same queue have no effect,
     * unless documented as such.
     */
    override fun dispose()

}
