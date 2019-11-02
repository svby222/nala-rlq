package nala.rlq

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nala.common.internal.currentTimeMillis
import nala.rlq.backoff.Backoff
import nala.rlq.internal.WorkerPoolDispatcher
import nala.rlq.retry.Retry

/**
 * An implementation of [RateLimitQueue] that internally delegates to a coroutine-based worker pool.
 *
 * Each bucket is internally assigned a task queue,
 * from which queued tasks are consumed and dispatched to a shared pool of [workers] worker coroutines.
 * All buckets are executed in parallel, whereas tasks within a given bucket are executed sequentially.
 *
 * @param parentScope the parent [CoroutineScope] to use for child coroutines
 * @param workers the amount of shared worker coroutines to launch
 */
@ExperimentalRateLimitApi
class CoroutineRateLimitQueue(parentScope: CoroutineScope, val workers: Int) : RateLimitQueue {

    private val queueJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = parentScope + queueJob
    private val dispatcher = WorkerPoolDispatcher(workers, queueJob)

    private val buckets = HashMap<Any, Bucket>()
    private val bucketMutex = Mutex()

    private val queue = Channel<QueuedTask<*>>(Channel.RENDEZVOUS)

    init {
        scope.launch(context = CoroutineName("CoroutineRateLimitQueue/Slurper")) {
            for (queued in queue) {
                bucketMutex.withLock {
                    val bucket = buckets.getOrPut(queued.task.bucket) { Bucket(queued.task.bucket) }
                    bucket.queue(queued)
                }
            }
        }
    }

    override suspend fun <TData> submit(task: RateLimitTask<TData>, retry: Retry?, backoff: Backoff?): TData {
        val deferred = CompletableDeferred<TData>(queueJob)

        try {
            queue.send(QueuedTask(task, deferred, retry, backoff?.generateSequence()?.iterator()))
            return deferred.await()
        } finally {
            if (!deferred.isCompleted) deferred.cancel()
        }
    }

    private data class QueuedTask<T>(
            val task: RateLimitTask<T>,
            val deferred: CompletableDeferred<T>,

            val retry: Retry?,
            val backoffIterator: Iterator<Long>?,
            val lastBackoff: Long = 0L
    )

    private data class BucketData(val timestamp: Long, val remaining: Int, val resetMillis: Long) {
        constructor(data: RateLimitData) : this(data.timestamp, data.remaining, data.resetMillis)
    }

    private inner class Bucket(bucketKey: Any) {

        private val bucketJob = Job(queueJob)
        private val bucketScope = scope + bucketJob

        private val data = atomic(BucketData(0, 0, 0))

        private val bucketQueue = Channel<QueuedTask<*>>(Channel.RENDEZVOUS)

        private val delayJob = atomic<Job>(Job().also { it.complete() })

        init {
            bucketScope.launch(context = CoroutineName("CoroutineRateLimitQueue/Bucket-$bucketKey")) {
                for (queued in bucketQueue) {
                    if (queued.deferred.isCompleted) continue

                    @Suppress("UNCHECKED_CAST")
                    queued as QueuedTask<Any?>

                    val result =
                            try {
                                withContext(queued.deferred) { tryDispatchImpl(queued.task) }
                            } catch (e: Throwable) {
                                if (queued.deferred.isCancelled) continue
                                RateLimitResult.Failure.Exception(e)
                            }

                    result.rateLimit?.let { updateData(BucketData(it)) }

                    when (result) {
                        is RateLimitResult.Success -> queued.deferred.complete(result.data)
                        is RateLimitResult.Failure -> tryResubmitImpl(queued, result)
                    }
                }
            }
        }

        private suspend fun <T> tryDispatchImpl(task: RateLimitTask<T>): RateLimitResult<T> {
            while (true) {
                val data = this.data.value

                if (data.remaining > 0) {
                    // Dispatch now
                    try {
                        return dispatcher.submit(task)
                    } finally {
                        updateData(data.copy(remaining = data.remaining - 1))
                    }
                } else {
                    val now = currentTimeMillis()
                    if (data.resetMillis <= now) {
                        // Dispatch now
                        try {
                            return dispatcher.submit(task)
                        } finally {
                            updateData(data.copy(remaining = data.remaining - 1))
                        }
                    } else {
                        // Wait
                        val newDelayJob = bucketScope.launch { delay(data.resetMillis - now) }
                        delayJob.getAndSet(newDelayJob).cancel()
                        newDelayJob.join()

                        continue
                    }
                }
            }
        }

        private fun tryResubmitImpl(queued: QueuedTask<*>, failure: RateLimitResult.Failure<*>) {
            if (queued.deferred.isCompleted) return

            if (queued.retry?.shouldRetry(queued.task, failure) != true) {
                // Cancel the task
                when (failure) {
                    is RateLimitResult.Failure.Exception -> queued.deferred.completeExceptionally(failure.exception)
                    // TODO special exception to better indicate the root cause?
                    else -> queued.deferred.cancel()
                }
                return
            }

            var discardBackoff = false
            val backoff = queued.backoffIterator?.let {
                if (it.hasNext()) it.next()
                else {
                    discardBackoff = true
                    queued.lastBackoff
                }
            } ?: queued.lastBackoff

            val newQueued = queued.copy(
                    backoffIterator = if (discardBackoff) null else queued.backoffIterator,
                    lastBackoff = backoff
            )

            bucketScope.launch {
                delay(backoff)
                queue(newQueued)
            }
        }

        private fun updateData(data: BucketData) {
            var updated = false
            this.data.update {
                if (data.timestamp >= it.timestamp) {
                    updated = true
                    data
                } else {
                    updated = false
                    it
                }
            }

            if (updated) {
                // New data received, cancel the delay job and resume dispatch
                delayJob.value.cancel()
            }
        }

        suspend fun queue(task: QueuedTask<*>) = bucketQueue.send(task)

    }

    override fun dispose() {
        scope.cancel()
        queue.close()
    }

}
