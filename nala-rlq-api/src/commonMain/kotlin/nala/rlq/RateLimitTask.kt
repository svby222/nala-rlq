package nala.rlq

/**
 * An interface representing a [SuspendingTask] that returns a [RateLimitResult]
 * to provide rate-limit data to a [RateLimitQueue] upon completion.
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
     * The rate-limit bucket this task is associated with.
     * This is used to group tasks that share the same rate-limit.
     * @see GlobalBucket
     */
    val bucket: Any

}

