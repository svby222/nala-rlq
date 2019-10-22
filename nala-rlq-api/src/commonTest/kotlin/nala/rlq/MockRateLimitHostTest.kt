package nala.rlq

import kotlinx.coroutines.delay
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@UseExperimental(ExperimentalRateLimitApi::class)
class MockRateLimitHostTest {

    @[Test PlatformIgnore]
    fun testMockHost() = runTest {
        val host = MockRateLimitHost(maxRequests = 3, interval = 500L)

        repeat(3) { assertTrue(host.request() is RateLimitResult.Success) }
        assertTrue(host.request() is RateLimitResult.Failure)

        delay(500L)

        repeat(3) { assertTrue(host.request() is RateLimitResult.Success) }
        assertTrue(host.request() is RateLimitResult.Failure)
    }

}
