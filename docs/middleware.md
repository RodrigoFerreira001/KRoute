# Middleware

Middlewares intercept the request pipeline. Each middleware can inspect or modify the request, enrich it with additional data, or halt processing entirely.

## Interface

```kotlin
interface HttpFunctionMiddleware {
    fun Scope.intercept(request: HttpRequest, response: HttpResponse): HttpRequest

    class Scope {
        fun proceed(request: HttpRequest): HttpRequest
        fun halt(statusCode: HttpStatusCode): Nothing
    }
}
```

Every middleware runs inside a `Scope` that provides two operations:

| Operation | Description |
|---|---|
| `proceed(request)` | Continue to the next middleware or `handleRequest`, optionally with a modified request |
| `halt(statusCode)` | Immediately terminate the pipeline and respond with the given status code |

## Writing a middleware

```kotlin
val AuthMiddleware = object : HttpFunctionMiddleware {
    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val token = request.headerOrNull("authorization")
            ?: halt(HttpStatusCode.Unauthorized)

        val userId = verifyToken(token) ?: halt(HttpStatusCode.Unauthorized)

        return proceed(request.withHeader("userId", userId)) // (1)
    }
}
```

1. `withHeader` returns a new `HttpRequest` with the additional header — the original is not mutated.

## Chaining data between middlewares

Use `withHeader` to pass data from one middleware to the next without mutating the original request:

```kotlin
// Middleware A — validates token, passes userId forward
val ValidateTokenMiddleware = object : HttpFunctionMiddleware {
    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val userId = validateToken(request.headerOrNull("authorization"))
            ?: halt(HttpStatusCode.Unauthorized)
        return proceed(request.withHeader("userId", userId))
    }
}

// Middleware B — reads userId set by A, adds role
val LoadUserMiddleware = object : HttpFunctionMiddleware {
    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val userId by request.headerDelegate() // reads "userId" header
        val role = userRepository.getRole(userId)
        return proceed(request.withHeader("userRole", role))
    }
}

// Middleware C — reads role set by B
val RequireAdminMiddleware = object : HttpFunctionMiddleware {
    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val role = request.headerOrNull("userRole")
        if (role != "ADMIN") halt(HttpStatusCode.Forbidden)
        return proceed(request)
    }
}
```

Register them in order in `defaultMiddlewares` or per-route `middlewares`:

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    defaultMiddlewares = listOf(
        ValidateTokenMiddleware,
        LoadUserMiddleware,
        RequireAdminMiddleware
    )
)
```

## Placement

Where a middleware is registered determines when it runs:

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    preRoutingMiddlewares = listOf(CORSMiddleware),   // before route matching
    defaultMiddlewares = listOf(AuthMiddleware),       // after matching, for every route
)

object AdminOnlyFunction : HttpFunction(
    path = "/admin/users",
    httpMethod = HttpMethod.Get,
    middlewares = listOf(RequireAdminMiddleware)        // only for this route
) { ... }
```

## Built-in middlewares

### CORSMiddleware

Adds CORS headers to all responses and handles `OPTIONS` preflight requests automatically.

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    preRoutingMiddlewares = listOf(CORSMiddleware)
)
```

**Headers set on every response:**

| Header | Value |
|---|---|
| `Access-Control-Allow-Origin` | `*` |

**Additional headers on `OPTIONS` preflight:**

| Header | Value |
|---|---|
| `Access-Control-Allow-Methods` | `*` |
| `Access-Control-Allow-Headers` | `*` |
| `Access-Control-Max-Age` | `86400` |

Preflight requests are terminated with `204 No Content`.

!!! tip
    Register `CORSMiddleware` in `preRoutingMiddlewares` so it runs before authentication and other middleware that might halt the pipeline.
