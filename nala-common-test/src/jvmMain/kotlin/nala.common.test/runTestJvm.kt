package nala.common.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual inline fun runTest(crossinline block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }
