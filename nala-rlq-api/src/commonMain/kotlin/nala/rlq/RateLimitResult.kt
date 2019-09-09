package nala.rlq

@ExperimentalRateLimitApi
sealed class RateLimitResult<TData> {

    abstract val rateLimit: RateLimitData?

    data class Success<TData>(val data: TData, override val rateLimit: RateLimitData) : RateLimitResult<TData>()

    sealed class Failure<TData> : RateLimitResult<TData>() {

        data class Exception<TData>(val exception: Throwable, override val rateLimit: RateLimitData? = null) : RateLimitResult.Failure<TData>()
        data class RateLimited<TData>(override val rateLimit: RateLimitData) : RateLimitResult.Failure<TData>()

    }

}
