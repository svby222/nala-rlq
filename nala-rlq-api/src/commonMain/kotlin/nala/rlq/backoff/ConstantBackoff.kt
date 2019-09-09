package nala.rlq.backoff

import nala.rlq.ExperimentalRateLimitApi
import nala.rlq.internal.infiniteSequenceOf

/**
 * An implementation of [Backoff] that always returns an infinite sequence of a constant [value].
 *
 * @param value the constant backoff time in milliseconds
 */
@ExperimentalRateLimitApi
class ConstantBackoff(val value: Long) : Backoff {

    // Instantiate the sequence once (lazily) to avoid new sequence creation,
    // since this sequence needs no internal state.
    private val sequence by lazy(LazyThreadSafetyMode.NONE) { infiniteSequenceOf(value) }

    override fun generateSequence(): Sequence<Long> = sequence

}
