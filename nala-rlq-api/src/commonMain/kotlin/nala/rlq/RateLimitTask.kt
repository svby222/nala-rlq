package nala.rlq

/**
 * An interface representing a [SuspendingTask] with an assigned [bucket] that returns a [RateLimitResult]
 * to provide rate-limit data to a [RateLimitQueue] upon completion.
 *
 * As implementations of [RateLimitQueue] may use hash tables to group tasks,
 * task [bucket identifiers][RateLimitTask.bucket] should have a sensible [hashCode] implementation.
 */
@ExperimentalRateLimitApi
interface RateLimitTask<TData> : SuspendingTask<RateLimitResult<TData>> {

    /**
     * The predefined global rate-limit bucket.
     * Use this when a task does not otherwise require its own rate-limit bucket (e.g. global endpoint limiting.)
     * @see [RateLimitTask.bucket]
     */
    object GlobalBucket

    /**
     * The rate-limit bucket this task is associated with, used to group tasks that share the same rate-limit.
     *
     * As implementations of [RateLimitQueue] may use hash tables to group tasks,
     * task [bucket identifiers][RateLimitTask.bucket] should have a sensible [hashCode] implementation.
     *
     * @see GlobalBucket
     */
    val bucket: Any

}

