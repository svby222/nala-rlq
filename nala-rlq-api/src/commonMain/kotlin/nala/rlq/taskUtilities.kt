package nala.rlq

/**
 * Constructs a new [SuspendingTask] using the specified [block] as the implementation of [SuspendingTask.invoke].
 * @see SuspendingTask
 */
inline fun <T> suspendingTask(crossinline block: suspend () -> T) = object : SuspendingTask<T> {
    override suspend fun invoke() = block()
}

/**
 * Applies the specified [mapper] to the receiver's encapsulated value and returns a new [RateLimitResult],
 * if the receiver indicates [a successful result][RateLimitResult.Success]. Otherwise, returns the receiver unchanged.
 * @see RateLimitResult.Success
 */
@ExperimentalRateLimitApi
fun <T, R> RateLimitResult<T>.map(mapper: (T) -> R): RateLimitResult<R> = when (this) {
    is RateLimitResult.Success -> RateLimitResult.Success(mapper(data), rateLimit)
    is RateLimitResult.Failure.Exception -> this
    is RateLimitResult.Failure.RateLimited -> this
}

/**
 * Returns a new instance of [SuspendingTask] with an implementation of [SuspendingTask.invoke]
 * that first invokes the receiver, applies the specified [mapper] to it, and returns the transformed result.
 */
fun <T, R> SuspendingTask<T>.map(mapper: (T) -> R): SuspendingTask<R> = object : SuspendingTask<R> {
    override suspend fun invoke() = mapper(this@map.invoke())
}

/**
 * Returns a new instance of [RateLimitTask] with an implementation of [RateLimitTask.invoke]
 * that first invokes the receiver, applies the specified [mapper] to it, and returns the transformed result.
 */
@ExperimentalRateLimitApi
inline fun <T, R> RateLimitTask<T>.map(crossinline mapper: (T) -> R): RateLimitTask<R> = object : RateLimitTask<R> {
    override val bucket = this@map.bucket
    override suspend fun invoke() = this@map.invoke().map { mapper(it) }
}

/**
 * Returns a new instance of [RateLimitTask] with the specified [bucket]
 * that delegates its implementation of [RateLimitTask.invoke] to the receiver.
 *
 * This can be used to construct a [RateLimitTask] from a [SuspendingTask],
 * or to copy a [RateLimitTask] with a different bucket.
 */
@ExperimentalRateLimitApi
fun <T> SuspendingTask<RateLimitResult<T>>.withBucket(bucket: Any = RateLimitTask.GlobalBucket) = object : RateLimitTask<T> {
    override val bucket = bucket
    override suspend fun invoke() = this@withBucket.invoke()
}
