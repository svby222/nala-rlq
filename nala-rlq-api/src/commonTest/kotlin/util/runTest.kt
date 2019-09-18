package util

/**
 * Marks a function as a test.
 *
 * On the JVM platform, this is empty.
 * On the JS platform, this is a typealias for [kotlin.test.Ignore] due to implementation restrictions.
 *
 * @see runTest
 */
@UseExperimental(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class PlatformIgnore()

/**
 * Blocks while the suspending test [block] runs.
 *
 * On the JVM platform, this is implemented by `runBlocking`.
 * As there is no similar method on the JS platform, its implementation does nothing.
 */
expect inline fun runTest(crossinline block: suspend () -> Unit)
