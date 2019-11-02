package nala.rlq

import kotlinx.coroutines.*
import nala.common.internal.use
import nala.common.test.PlatformIgnore
import nala.common.test.runTest
import nala.rlq.internal.TaskDispatcher
import nala.rlq.internal.WorkerPoolDispatcher
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

        val job = launch {
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

        val job = launch {
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
        var executed = 0

        suspend fun submit() {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(Random.nextLong(100L))
                    executed++
                }
            })
        }
        coroutineScope {
            repeat(10) { launch { submit() } }
        }

        assertEquals(10, executed)
    }

    @[Test PlatformIgnore]
    fun testCancelFuture() = runTest {
        WorkerPoolDispatcher(1).use { singleDispatcher ->
            var executed = false

            launch {
                singleDispatcher.submit(object : SuspendingTask<Unit> {
                    override suspend fun invoke() {
                        delay(300L)
                    }
                })
            }

            delay(100L)

            val job = launch {
                singleDispatcher.submit(object : SuspendingTask<Unit> {
                    override suspend fun invoke() {
                        delay(200L)
                        executed = true
                    }
                })
            }

            job.cancel()

            delay(1000L)

            assertTrue(job.isCancelled)
            assertFalse(executed)
        }
    }

    @[Test PlatformIgnore]
    fun testFailureHandling() = runTest {
        val dispatcher = WorkerPoolDispatcher(1)

        var executed = false
        dispatcher.use {
            supervisorScope {
                launch {
                    dispatcher.submit(suspendingTask<Unit> { throw Exception() })
                }
            }

            delay(50L)

            supervisorScope {
                launch {
                    dispatcher.submit(suspendingTask { executed = true })
                }
            }
        }

        assertTrue(executed)
    }

}
