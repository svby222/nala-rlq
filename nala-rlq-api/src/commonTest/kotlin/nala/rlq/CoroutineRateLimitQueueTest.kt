package nala.rlq

import nala.common.internal.currentTimeMillis
import nala.common.internal.use
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@UseExperimental(ExperimentalRateLimitApi::class)
class CoroutineRateLimitQueueTest {

    @[Test PlatformIgnore]
    fun test() = runTest {
        CoroutineRateLimitQueue(this, 4).use { queue ->
            val now = currentTimeMillis()
            val delay = 1000

            var index = 0

            val timestamps = mutableListOf<Long>()

            val task = suspendingTask {
                // delay(500L)
                timestamps.add(currentTimeMillis())
                println("Completed task ${index + 1}")
                index++
            }
                    .map { RateLimitResult.Success(it, RateLimitData(now, false, 4 - it % 5, now + delay * (1 + it / 5))) }
                    .withBucket()

            repeat(11) { queue.submit(task) }

            assertTrue(timestamps[5] - now >= delay)
            assertTrue(timestamps[10] - now >= delay * 2)
        }
    }

}
