package nala.rlq.http

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.response.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import nala.rlq.SuspendingTask

class HttpTask(private val client: HttpClient, val request: HttpRequestData) : SuspendingTask<HttpResponse> {

    companion object Factory {

        inline fun get(client: HttpClient, url: String, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Get; url(url); builder() }.build())

        inline fun post(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Post; url(url); this.body = body; builder() }.build())

        inline fun put(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Put; url(url); this.body = body; builder() }.build())

        inline fun patch(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Patch; url(url); this.body = body; builder() }.build())

        inline fun delete(client: HttpClient, url: String, body: Any = EmptyContent, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Delete; url(url); this.body = body; builder() }.build())

        inline fun head(client: HttpClient, url: String, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Head; url(url); builder() }.build())

        inline fun options(client: HttpClient, url: String, builder: HttpRequestBuilder.() -> Unit = {}) =
                HttpTask(client, HttpRequestBuilder().apply { method = HttpMethod.Options; url(url); builder() }.build())

    }

    override suspend fun invoke(): HttpResponse = client.request(HttpRequestBuilder().takeFrom(request))

}
