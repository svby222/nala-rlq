package nala.rlq

import kotlinx.coroutines.*
import nala.rlq.internal.TaskDispatcher
import nala.rlq.internal.WorkerPoolDispatcher
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

    @Test
    fun testDispose() = runTest {
        var executed = false
        var cancelled = false

        val job = GlobalScope.launch {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    try {
                        delay(200L)
                        executed = true
                    } catch (e: CancellationException) {
                        cancelled = true
                    }
                }
            })
        }

        job.cancel()

        delay(300L)

        assertTrue(cancelled)
        assertFalse(executed)
    }

    @Test
    fun testSubmitCancel() = runTest {
        var executed = false
        var cancelled = false

        val job = GlobalScope.launch {
            dispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    try {
                        delay(200L)
                        executed = true
                    } catch (e: CancellationException) {
                        cancelled = true
                    }
                }
            })
        }

        job.cancel()

        delay(300L)

        assertTrue(cancelled)
        assertFalse(executed)
    }

    @Test
    fun testSubmit() = runTest {
        var executed = false
        dispatcher.submit(object : SuspendingTask<Unit> {
            override suspend fun invoke() {
                executed = true
            }
        })

        assertTrue(executed)
    }

    @Test
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

    @Test
    fun testCancelFuture() = runTest {
        val singleDispatcher = WorkerPoolDispatcher(1)

        var executed = false

        GlobalScope.launch {
            singleDispatcher.submit(object : SuspendingTask<Unit> {
                override suspend fun invoke() {
                    delay(300L)
                }
            })
        }

        delay(200L)

        val future = singleDispatcher.submitAsync(object : SuspendingTask<Unit> {
            override suspend fun invoke() {
                delay(200L)
                executed = true
            }
        })

        future.cancel()

        delay(500L)

        assertTrue(future.isCancelled)
        assertFalse(executed)
    }

}
