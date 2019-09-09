package nala.rlq.backoff

import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.internal.infiniteSequenceOf

/**
 * An implementation of [Backoff] that always returns an infinite sequence of 0 (i.e. no backoff).
 */
@ExperimentalRateLimitApi
object IgnoreBackoff : Backoff {

    private val sequence = infiniteSequenceOf(0L)

    override fun generateSequence() = sequence

}
