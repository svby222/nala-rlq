package nala.common.internal

import kotlinx.coroutines.CoroutineScope

/**
 * An interface representing an object with a lifetime (i.e. a [CoroutineScope])
 * that can be manually disposed.
 */
interface Disposable {

    /**
     * Closes all resources associated with this object and disposes it.
     *
     * This function is idempotent;
     * multiple attempts to dispose the same object have no effect,
     * unless documented as such.
     */
    fun dispose()

}

/**
 * Executes the given [block] on this disposable and disposes it regardless of any exceptions thrown.
 *
 * @return the result of invoking [block]
 */
inline fun <T : Disposable?, R> T.use(block: (T) -> R) = try {
    block(this)
} finally {
    this?.dispose()
}
