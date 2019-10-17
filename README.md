# rlq

![Build status (Travis)](https://travis-ci.org/friiday/nala-rlq.svg?branch=master)

A LGPLv3 dynamic task dispatching library for clients of rate-limited services.

## What is this for?

The main purpose of this library is to provide a usable mechanism to execute tasks with rate-limited endpoints
(e.g. RESTful APIs).

## How can I use it?

The most important types defined in this library are `RateLimitQueue` ("queues") and `RateLimitTask` ("tasks").

A task is a `suspend`ing function that returns a result, along with optional rate-limit information.
Tasks can be submitted to queues for eventual execution as soon as the corresponding bucket
(i.e. a resource, an endpoint, etc.) is not being rate-limited. Queues solely rely on rate-limit information being
provided by successful or failed tasks, meaning that rate-limiting can be fully controlled by the server
(à la [HTTP 429][mozilla-429] headers) or manually by the client (as a sort of [token bucket][wikipedia-tokenbucket]).

Buckets are keys that identify a unique rate-limitable resource (e.g. an endpoint). If no specific bucket is needed,
the task should either be executed independently without a queue or assigned the global bucket
(`RateLimitQueue.GlobalBucket`).

Currently, the only provided task implementation is in the `rlq-http` module, which provides a task wrapper for
[Ktor][github-ktor] HTTP requests.

### Examples

The `rlq-http` module provides a basic HTTP task implementation using a [Ktor][github-ktor] `HttpClient`.

JVM consumers of this module will need to include one of the Ktor [engines][ktor-engines] in their dependencies.

To create a basic HTTP request task, use one of the factory functions available on HttpTask's companion object:

```kotlin
val task: SuspendingTask = HttpTask.get("http://example.com/")
```

To submit this task to a queue, we need to convert it into a `RateLimitTask`, which is nothing more than a plain task
with a bucket and a return type of `RateLimitResult`.

```kotlin
val task: RateLimitTask<Foo> = HttpTask.get("http://example.com/")
        .map {
            when (it.status) {
                HttpStatusCode.TooManyRequests -> RateLimitResult.Failure.RateLimited(...)
                else -> RateLimitResult.Success(...)
            }
        }
        // Converts a SuspendingTask<RateLimitResult<T>>
        // to a RateLimitTask<T>
        .withBucket(RateLimitTask.GlobalBucket)
```

Now we can submit this task to a queue:

```kotlin
val queue: RateLimitQueue = CoroutineRateLimitQueue(coroutineScope, workers = 8)

// Suspends until completion
val result: Foo = queue.submit(task)

...
```

To instantiate a `CoroutineRateLimitQueue`, you should [use your application's `CoroutineScope`][elizarov-globalscope]
if possible.

[mozilla-429]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429
[wikipedia-tokenbucket]: https://en.wikipedia.org/wiki/Token_bucket
[github-ktor]: https://github.com/ktorio/ktor
[ktor-engines]: https://ktor.io/clients/http-client/engines.html#jvm
[elizarov-globalscope]: https://medium.com/@elizarov/the-reason-to-avoid-globalscope-835337445abc
