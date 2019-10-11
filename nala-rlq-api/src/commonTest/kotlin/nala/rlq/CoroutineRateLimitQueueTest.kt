package nala.rlq

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import nala.rlq.internal.currentTimeMillis
import kotlin.math.max
import kotlin.test.Test

@UseExperimental(ExperimentalRateLimitApi::class)
class CoroutineRateLimitQueueTest {

    @[Test PlatformIgnore]
    fun test() = runTest {
        val queue = CoroutineRateLimitQueue(GlobalScope, 4)

        val now = currentTimeMillis()
        val end = now + 5000
        var count = 0

        val task = suspendingTask {
            delay(500L)
            println("Task ${++count}")
            count
        }
                .map { RateLimitResult.Success(it, RateLimitData(now, false, max(0, 5 - it), end)) }
                .withBucket()

        val result1 = queue.submit(task)
        val result2 = queue.submit(task)
        val result3 = queue.submit(task)
        val result4 = queue.submit(task)
        val result5 = queue.submit(task)
        val result6 = queue.submit(task)

        println(result1)
        println(result2)
        println(result3)
        println(result4)
        println(result5)
        println(result6)
    }

}
