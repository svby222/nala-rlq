package nala.rlq

import kotlinx.coroutines.*
import nala.common.internal.currentTimeMillis
import nala.common.internal.use
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import kotlin.test.*

/**
 * Tests for [CoroutineRateLimitQueue].
 *
 * These tests should verify:
 *  - correct dispatching
 *  - correct suspension/waiting
 *  - correct cancellation handling
 */
@UseExperimental(ExperimentalRateLimitApi::class)
class CoroutineRateLimitQueueTest {

    // region Functionality

    /**
     * This test verifies a queue's ability to correctly dispatch tasks and wait for rate limits to expire.
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a task with bucket limit x = 3
     *     WHEN:  the task is submitted 2x + 1 times where x = the bucket limit
     *     AND    execution timestamps are recorded
     *     THEN:  the task is executed 2x + 1 times
     *     AND    the timestamps of tasks x + 1 and 2x + 1 indicate correct backoff
     */
    @[Test PlatformIgnore]
    fun testWithManualClient() = runTest {
        // GIVEN
        val now = currentTimeMillis()

        val queue = CoroutineRateLimitQueue(this, 1)

        val limit = 3
        val interval = 500

        val timestamps = mutableListOf<Long>()

        val taskGenerator = { index: Int ->
            suspendingTask { timestamps.add(currentTimeMillis()) }.map {
                val remaining = (limit - 1) - index % limit
                val resetMillis = now + interval * (1 + index / limit)
                RateLimitResult.Success(index, RateLimitData(now, false, remaining, resetMillis))
            }.withBucket(RateLimitTask.GlobalBucket)
        }

        // WHEN
        queue.use { repeat(2 * limit + 1) { queue.submit(taskGenerator(it)) } }

        // THEN
        assertTrue(timestamps[limit] - now >= interval)
        assertTrue(timestamps[2 * limit] - now >= interval * 2)
    }

    /**
     * This test verifies a queue's ability to correctly wait for rate limits to expire.
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a mock rate-limit host with maxRequests = 3 and a client task
     *     WHEN:  the task is submitted maxRequests + 1 times, twice
     *     THEN:  no rate-limit violations are recorded by the host
     */
    @[Test PlatformIgnore]
    fun testWithMockClient() = runTest {
        // GIVEN
        val queue = CoroutineRateLimitQueue(this, 4)
        val host = MockRateLimitHost(maxRequests = 3, interval = 500L)
        val task = suspendingTask { host.request() }.withBucket(RateLimitTask.GlobalBucket)

        queue.use {
            repeat(2) {
                // WHEN
                repeat(4) { queue.submit(task) }

                // THEN
                assertEquals(0, host.violations)

                delay(500L)
            }
        }
    }

    // endregion

    // region Cancellation

    /**
     * This test verifies a queue's ability to cancel suspending calls to [submit][RateLimitQueue.submit]
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a task that blocks forever
     *     WHEN:  the task is submitted
     *     AND    the coroutine that submitted the task is cancelled
     *     THEN:  the job fails
     */
    @[Test PlatformIgnore]
    fun testCancelSubmit() = runTest {
        // GIVEN
        val queue = CoroutineRateLimitQueue(this, 1)

        val task = suspendingTask {
            delay(Long.MAX_VALUE)
            RateLimitResult.Success(Unit, null)
        }.withBucket(RateLimitTask.GlobalBucket)

        // WHEN
        lateinit var submitJob: Deferred<*>
        queue.use {
            withTimeout(5000L) {
                supervisorScope {
                    submitJob = async { queue.submit(task) }
                    submitJob.cancel()
                }
            }
        }

        // THEN
        assertFailsWith<CancellationException> { submitJob.await() }
    }

    /**
     * This test verifies a queue's ability to cancel suspending calls to [submit][RateLimitQueue.submit]
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a task that blocks forever
     *     WHEN:  the task is submitted
     *     AND    the coroutine that submitted the task is cancelled
     *     THEN:  the task does not complete
     */
    @[Test PlatformIgnore]
    fun testCancelSubmitIsNotExecuted() = runTest {
        // GIVEN
        val queue = CoroutineRateLimitQueue(this, 1)

        var executed = false
        val task = suspendingTask {
            delay(500L)
            executed = true
            RateLimitResult.Success(Unit, null)
        }.withBucket(RateLimitTask.GlobalBucket)

        // WHEN
        lateinit var submitJob: Deferred<*>
        queue.use {
            supervisorScope {
                submitJob = async { queue.submit(task) }
                delay(50L)
                submitJob.cancel()
                delay(500L)
            }
        }

        // THEN
        assertFalse(executed)
    }

    /**
     * This test verifies a queue's ability to correctly dispose of its resources and cancel the currently dispatched
     * task.
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a task that blocks forever
     *     WHEN:  the task is submitted
     *     AND    the queue is disposed
     *     THEN:  the job fails
     */
    @[Test PlatformIgnore]
    fun testDisposeCancelSubmit() = runTest {
        // GIVEN
        val queue = CoroutineRateLimitQueue(this, 1)

        val task = suspendingTask {
            delay(Long.MAX_VALUE)
            RateLimitResult.Success(Unit, null)
        }.withBucket(RateLimitTask.GlobalBucket)

        // WHEN
        lateinit var submitJob: Deferred<*>
        withTimeout(5000L) {
            supervisorScope {
                submitJob = async { queue.submit(task) }
                queue.dispose()
            }
        }

        // THEN
        assertFailsWith<IllegalStateException> { submitJob.await() }
    }

    /**
     * This test verifies a queue's ability to correctly dispose of its resources and cancel scheduled (i.e. future)
     * tasks.
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a task that blocks forever
     *     WHEN:  the task is submitted to block the queue
     *     AND    the task is submitted again
     *     AND    the queue is disposed
     *     THEN:  all tasks fail
     */
    @[Test PlatformIgnore]
    fun testDisposeCancelFuture() = runTest {
        // GIVEN
        val queue = CoroutineRateLimitQueue(this, 1)

        val task = suspendingTask {
            delay(Long.MAX_VALUE)
            RateLimitResult.Success(Unit, null)
        }.withBucket(RateLimitTask.GlobalBucket)

        // WHEN
        lateinit var jobs: List<Job>
        withTimeout(5000L) {
            supervisorScope {
                launch { queue.submit(task) }

                jobs = List(3) { launch { queue.submit(task) } }
                queue.dispose()
            }
        }

        // THEN
        assertTrue(jobs.all { it.isCancelled })
    }

    // endregion

    // region Error handling

    /**
     * This test verifies that a queue remains active after a task has failed.
     *
     * Test specification:
     *
     *     GIVEN: an empty queue, a failing task, a task
     *     WHEN:  the failing task is submitted and allowed to throw an exception
     *     AND    the second task is submitted
     *     THEN:  the first task fails
     *     AND    the second task is completed
     */
    @[Test PlatformIgnore]
    fun testFailureHandling() = runTest {
        val queue = CoroutineRateLimitQueue(this, 1)

        var executed = false
        queue.use {
            assertFailsWith<Exception> {
                queue.submit(suspendingTask { throw Exception() }.withBucket(RateLimitTask.GlobalBucket))
            }

            delay(50L)

            queue.submit(suspendingTask {
                executed = true
                RateLimitResult.Success(Unit, null)
            }.withBucket(RateLimitTask.GlobalBucket))
        }

        assertTrue(executed)
    }

    // endregion

}
