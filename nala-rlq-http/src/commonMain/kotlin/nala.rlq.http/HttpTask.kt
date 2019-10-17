package nala.rlq.http

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.response.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import nala.rlq.RateLimitTask
import nala.rlq.SuspendingTask
import kotlin.jvm.JvmOverloads

/**
 * An implementation of [RateLimitTask] that asynchronously sends an HTTP request using the given Ktor [client].
 *
 * @param request the data for the HTTP request; obtained via [HttpRequestBuilder.build]
 */
class HttpTask @JvmOverloads constructor(private val client: HttpClient = HttpClient(), val request: HttpRequestData) : SuspendingTask<HttpResponse> {

    /** A companion object containing [HttpTask] factory functions. */
    companion object Factory {

        /**
         * A factory function that creates a [`GET`][HttpMethod.Get] [HttpTask] with the specified [url].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun get(client: HttpClient, url: String, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Get; url(url); builder() }.build())

        /**
         * A factory function that creates a [`POST`][HttpMethod.Post] [HttpTask] with the specified [url] and [body].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun post(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Post; url(url); this.body = body; builder() }.build())

        /**
         * A factory function that creates a [`PUT`][HttpMethod.Put] [HttpTask] with the specified [url] and [body].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun put(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Put; url(url); this.body = body; builder() }.build())

        /**
         * A factory function that creates a [`PATCH`][HttpMethod.Patch] [HttpTask] with the specified [url] and [body].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun patch(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Patch; url(url); this.body = body; builder() }.build())

        /**
         * A factory function that creates a [`DELETE`][HttpMethod.Delete] [HttpTask] with the specified [url] and [body].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun delete(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Delete; url(url); this.body = body; builder() }.build())

        /**
         * A factory function that creates a [`HEAD`][HttpMethod.Head] [HttpTask] with the specified [url].
         * @param builder an optional configuration block for the [HttpRequestBuilder]
         */
        inline fun head(client: HttpClient, url: String, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Head; url(url); builder() }.build())

    }

    override suspend fun invoke(): HttpResponse = client.request(HttpRequestBuilder().takeFrom(request))

}
