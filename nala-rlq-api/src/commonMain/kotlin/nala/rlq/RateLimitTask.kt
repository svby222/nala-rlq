package nala.rlq

@ExperimentalRateLimitApi
interface RateLimitTask<TData> {

    object GlobalBucket

    val bucket: Any

    suspend operator fun invoke(): RateLimitResult<TData>

}
