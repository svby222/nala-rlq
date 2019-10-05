package nala.common.test

import kotlinx.coroutines.runBlocking

actual inline fun runTest(crossinline block: suspend () -> Unit) = runBlocking { block() }
