package nala.rlq.http

import io.ktor.client.HttpClient
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpStatusCode
import nala.common.internal.currentTimeMillis
import nala.common.internal.use
import nala.common.test.runTest
import nala.rlq.*
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
@UseExperimental(ExperimentalRateLimitApi::class)
class CloudflareRateLimitTest {

    private val client = HttpClient()

    /**
     * This test uses a [test endpoint](https://www.cloudflare.com/rate-limit-test/) provided by Cloudflare.
     * Upon receiving 10 requests, the client's IP will be rate-limited for one minute.
     *
     * Depending on the number of requests, this test may take a while to complete.
     */
    @Test
    fun test() = runTest {
        val requests = 21
        var index = 0
        val delay = 100 * 1000L

        CoroutineRateLimitQueue(this, 4).use { queue ->
            val task = HttpTask.head(client, "https://www.cloudflare.com/rate-limit-test/")
                    .map<HttpResponse, RateLimitResult<Pair<Boolean, HttpStatusCode>>> {
                        val remaining = 9 - index % 10
                        index++

                        val data =
                                if (remaining == 0) RateLimitData(it.responseTime.timestamp, false, 0, currentTimeMillis() + delay)
                                else null

                        // Always return success in order to fail fast
                        when (it.status) {
                            HttpStatusCode.TooManyRequests -> RateLimitResult.Success(Pair(false, it.status), data)
                            else -> {
                                if (it.status.value in 400..599) RateLimitResult.Success(Pair(false, it.status), data)
                                else RateLimitResult.Success(Pair(true, it.status), data)
                            }
                        }
                    }
                    .withBucket(RateLimitTask.GlobalBucket)

            repeat(requests) {
                val (ok, status) = queue.submit(task)
                if (!ok) throw AssertionError("Request ${it + 1} was not successful: $status")
                else println("Request ${it + 1} was successful")
            }
        }
    }

}
