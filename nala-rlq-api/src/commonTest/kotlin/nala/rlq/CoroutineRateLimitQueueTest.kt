package nala.rlq

import kotlinx.coroutines.delay
import nala.common.internal.currentTimeMillis
import nala.common.internal.use
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import nala.rlq.retry.CounterRetry
import nala.rlq.retry.InfiniteRetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@UseExperimental(ExperimentalRateLimitApi::class)
class CoroutineRateLimitQueueTest {

    @[Test PlatformIgnore]
    fun testWithManualClient() = runTest {
        CoroutineRateLimitQueue(this, 4).use { queue ->
            val now = currentTimeMillis()
            val delay = 1000

            var index = 0

            val timestamps = mutableListOf<Long>()

            val task = suspendingTask {
                timestamps.add(currentTimeMillis())
                index++
            }
                    .map { RateLimitResult.Success(it, RateLimitData(now, false, 4 - it % 5, now + delay * (1 + it / 5))) }
                    .withBucket()

            repeat(11) { queue.submit(task) }

            assertTrue(timestamps[5] - now >= delay)
            assertTrue(timestamps[10] - now >= delay * 2)
        }
    }

    @[Test PlatformIgnore]
    fun testWithMockClient() = runTest {
        val host = MockRateLimitHost(maxRequests = 3, interval = 500L)
        val task = suspendingTask { host.request() }.withBucket(RateLimitTask.GlobalBucket)

        CoroutineRateLimitQueue(this, 4).use { queue ->
            repeat(4) { queue.submit(task) }
            assertEquals(0, host.violations)

            delay(500L)

            repeat(4) { queue.submit(task) }
            assertEquals(0, host.violations)
        }
    }

    @[Test PlatformIgnore]
    fun testDisposeCleanup() = runTest {
        val host = MockRateLimitHost(maxRequests = 1, interval = 99999L)
        val task = suspendingTask { host.request() }.withBucket(RateLimitTask.GlobalBucket)

        val queue = CoroutineRateLimitQueue(this, 4)
        
        task()

        val futures = List(3) { queue.submitAsync(task, CounterRetry(1)) }
        queue.dispose()

        assertTrue(futures.all { it.isCancelled })
    }

}
