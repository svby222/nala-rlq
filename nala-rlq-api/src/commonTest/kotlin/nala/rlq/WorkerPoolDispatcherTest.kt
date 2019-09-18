package nala.rlq

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nala.rlq.internal.TaskDispatcher
import nala.rlq.internal.WorkerPoolDispatcher
import nala.rlq.internal.use
import util.PlatformIgnore
import util.runTest
import kotlin.random.Random
import kotlin.test.*

@UseExperimental(ExperimentalRateLimitApi::class)
class WorkerPoolDispatcherTest {

    private lateinit var dispatcher: TaskDispatcher

    @BeforeTest
    fun beforeTest() {
        dispatcher = WorkerPoolDispatcher(4)
    }

    @AfterTest
    fun afterTest() = dispatcher.dispose()

    @[Test PlatformIgnore]
    fun testDispose() = runTest {
        var executed = false

        val job = GlobalScope.launch {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(200L)
                    executed = true
                }
            })
        }

        job.cancel()

        delay(500L)

        assertFalse(executed)
    }

    @[Test PlatformIgnore]
    fun testSubmitCancel() = runTest {
        var executed = false

        val job = GlobalScope.launch {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(200L)
                    executed = true
                }
            })
        }

        job.cancel()

        delay(500L)

        assertFalse(executed)
    }

    @[Test PlatformIgnore]
    fun testSubmit() = runTest {
        var executed = false
        dispatcher.submit(object : SuspendingTask<Unit> {
            override suspend fun invoke() {
                executed = true
            }
        })

        assertTrue(executed)
    }

    @[Test PlatformIgnore]
    fun testSubmitMultiple() = runTest {
        suspend fun submit() {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(Random.nextLong(100L))
                }
            })
        }
        coroutineScope {
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
            launch { submit() }
        }
    }

    @[Test PlatformIgnore]
    fun testCancelFuture() = runTest {
        WorkerPoolDispatcher(1).use { singleDispatcher ->

            var executed = false

            GlobalScope.launch {
                singleDispatcher.submit(object : SuspendingTask<Unit> {
                    override suspend fun invoke() {
                        delay(300L)
                    }
                })
            }

            delay(100L)

            val future = singleDispatcher.submitAsync(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(200L)
                    executed = true
                }
            })

            future.cancel()

            delay(1000L)

            assertTrue(future.isCancelled)
            assertFalse(executed)
        }
    }

}
