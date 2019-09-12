package util

/**
 * Blocks while the suspending test [block] runs.
 *
 * On the JVM platform, this is implemented by `runBlocking`.
 * As there is no similar method on the JS platform, its implementation does nothing.
 */
expect inline fun runTest(crossinline block: suspend () -> Unit)
