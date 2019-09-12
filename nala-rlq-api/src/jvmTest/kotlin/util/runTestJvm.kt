package util

import kotlinx.coroutines.runBlocking

actual inline fun runTest(crossinline block: suspend () -> Unit) = runBlocking { block() }
