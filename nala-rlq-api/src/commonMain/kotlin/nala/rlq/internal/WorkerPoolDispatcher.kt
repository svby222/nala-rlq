package nala.rlq.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.SuspendingTask

/**
 * An implementation of [TaskDispatcher] that dispatches tasks to a fixed number of worker coroutines.
 *
 * @param workers the amount of worker coroutines to start
 * @param parentJob the parent for this dispatcher's [CoroutineScope]
 */
@ExperimentalRateLimitApi
internal class WorkerPoolDispatcher(workers: Int, parentJob: Job? = null) : TaskDispatcher {

    private val poolJob = SupervisorJob(parentJob)
    private val scope = CoroutineScope(Dispatchers.Default + poolJob)
    private val queue = Channel<Pair<SuspendingTask<*>, CompletableDeferred<*>>>(Channel.RENDEZVOUS)

    init {
        require(workers > 0) { "workers must be positive" }

        repeat(workers) { i ->
            scope.launch(context = CoroutineName("WorkerPoolDispatcher/Worker-$i")) {
                for ((task, deferred) in queue) {
                    if (deferred.isCompleted) continue
                    supervisorScope {
                        try {
                            val result = withContext(deferred) { task.invoke() }

                            // This cast is fine since the signature of [submit] guarantees
                            // that the job and the task are of compatible types.
                            @Suppress("UNCHECKED_CAST")
                            (deferred as CompletableDeferred<Any?>).complete(result)
                        } catch (e: Throwable) {
                            deferred.completeExceptionally(e)
                        }
                    }
                }
            }
        }
    }

    override suspend fun <T> submit(task: SuspendingTask<T>): T {
        val deferred = CompletableDeferred<T>(poolJob)

        try {
            queue.send(Pair(task, deferred))
            return deferred.await()
        } finally {
            if (!deferred.isCompleted) deferred.cancel()
        }
    }

    override fun <T> submitAsync(task: SuspendingTask<T>) = scope.async { submit(task) }

    override fun dispose() {
        scope.cancel()
    }

}
