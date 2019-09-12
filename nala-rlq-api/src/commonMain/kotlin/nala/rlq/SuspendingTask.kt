package nala.rlq

/**
 * An interface representing a `suspend () -> `[`TResult`][TResult].
 * Once Kotlin supports extending suspending function types, this will be replaced by a typealias.
 */
interface SuspendingTask<out TResult> {

    suspend operator fun invoke(): TResult

}
