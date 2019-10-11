package nala.rlq

/**
 * A sealed class representing the result of a [RateLimitTask].
 *
 * Values of this type can be:
 *  - successful ([Success]): contains [data][RateLimitResult.Success.data] of type [TData] and optional [RateLimitData].
 *  - failed due to an exception ([Failure.Exception]): contains an [exception][Failure.Exception.exception] and
 *  optional [RateLimitData].
 *  - failed due to rate limiting ([Failure.RateLimited]): contains [RateLimitData].
 */
@ExperimentalRateLimitApi
sealed class RateLimitResult<in TData> {

    /**
     * The rate-limit information associated with this result.
     */
    abstract val rateLimit: RateLimitData?

    /**
     * A [RateLimitResult] representing success.
     * Contains [data][RateLimitResult.Success.data] of type [TData] and optional [RateLimitData].
     *
     * @see RateLimitResult
     */
    data class Success<TData>(val data: TData, override val rateLimit: RateLimitData?) : RateLimitResult<TData>()

    /**
     * A sealed class representing the result of a failed [RateLimitTask].
     *
     * @see RateLimitResult
     */
    sealed class Failure<TData> : RateLimitResult<TData>() {

        /**
         * A [RateLimitResult] representing a failure due to an exception.
         * Contains an [exception][Failure.Exception.exception] and optional [RateLimitData].
         *
         * @see RateLimitResult
         */
        data class Exception(val exception: Throwable, override val rateLimit: RateLimitData? = null) : RateLimitResult.Failure<Any?>()

        /**
         * A [RateLimitResult] representing a failure due to rate limiting.
         * Contains [RateLimitData].
         *
         * @see RateLimitResult
         */
        data class RateLimited(override val rateLimit: RateLimitData) : RateLimitResult.Failure<Any?>()

    }

}
