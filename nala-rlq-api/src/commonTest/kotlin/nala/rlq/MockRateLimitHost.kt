package nala.rlq

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nala.common.internal.currentTimeMillis
import kotlin.math.max

// A testing utility class that simulates a rate-limiting host.
@ExperimentalRateLimitApi
class MockRateLimitHost(private val maxRequests: Int = 5, private val interval: Long = 3000L) {

    init {
        require(maxRequests > 0) { "maxRequests must be positive" }
        require(interval > 0) { "interval must be positive" }
    }

    private val mutex = Mutex()

    private lateinit var limit: Pair<Int, Long>

    // The amount of rate-limit violations since the last reset.
    var violations = 0
        private set

    suspend fun request(): RateLimitResult<Unit> {
        val now = currentTimeMillis()
        val limit = mutex.withLock {
            limit = when {
                !::limit.isInitialized || now >= limit.second -> Pair(maxRequests - 1, now + interval)
                limit.first > 0 -> limit.copy(first = max(0, limit.first - 1))
                else -> {
                    violations++
                    return RateLimitResult.Failure.RateLimited(RateLimitData(now, false, 0, limit.second))
                }
            }

            limit
        }

        return RateLimitResult.Success(Unit, RateLimitData(now, false, limit.first, limit.second))
    }

    suspend fun reset() {
        mutex.withLock {
            limit = Pair(1, 0)
            violations = 0
        }
    }

}
