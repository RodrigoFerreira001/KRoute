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

#### Default usage

Allows all origins with standard methods — equivalent to Express's `cors()` with no arguments:

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    preRoutingMiddlewares = listOf(CORSMiddleware())
)
```

#### Configuration

Pass a `CorsOptions` instance to customize behavior:

```kotlin
CORSMiddleware(
    CorsOptions(
        origin = CorsOrigin.Single("https://myapp.com"),
        methods = listOf(HttpMethod.Get, HttpMethod.Post),
        allowedHeaders = listOf("Content-Type", "Authorization"),
        exposedHeaders = listOf("X-Custom-Header"),
        credentials = true,
        maxAge = 3600,
        optionsSuccessStatus = HttpStatusCode.NoContent
    )
)
```

#### `CorsOptions` reference

| Option | Type | Default | Description |
|---|---|---|---|
| `origin` | `CorsOrigin` | `CorsOrigin.Any` | Controls `Access-Control-Allow-Origin` |
| `methods` | `List<HttpMethod>` | GET, HEAD, PUT, PATCH, POST, DELETE | Controls `Access-Control-Allow-Methods` |
| `allowedHeaders` | `List<String>?` | `null` | Controls `Access-Control-Allow-Headers`. When `null`, reflects the request's `Access-Control-Request-Headers` |
| `exposedHeaders` | `List<String>` | `[]` | Controls `Access-Control-Expose-Headers` |
| `credentials` | `Boolean` | `false` | Sets `Access-Control-Allow-Credentials: true` |
| `maxAge` | `Int?` | `null` | Sets `Access-Control-Max-Age` in seconds |
| `optionsSuccessStatus` | `HttpStatusCode` | `204 No Content` | Status code returned for preflight responses |

#### `CorsOrigin` variants

| Variant | Behavior |
|---|---|
| `CorsOrigin.Any` | Sets `Access-Control-Allow-Origin: *` |
| `CorsOrigin.None` | Does not set the header |
| `CorsOrigin.Single("https://example.com")` | Sets the given origin and adds `Vary: Origin` |
| `CorsOrigin.Multiple(listOf(...))` | Reflects the request origin if it matches the list |
| `CorsOrigin.Predicate { origin -> ... }` | Reflects the request origin if the predicate returns `true` |

#### Examples

**Allow a specific origin:**
```kotlin
CORSMiddleware(CorsOptions(origin = CorsOrigin.Single("https://myapp.com")))
```

**Allow multiple origins:**
```kotlin
CORSMiddleware(CorsOptions(
    origin = CorsOrigin.Multiple(listOf("https://myapp.com", "https://staging.myapp.com"))
))
```

**Dynamic origin with predicate:**
```kotlin
CORSMiddleware(CorsOptions(
    origin = CorsOrigin.Predicate { origin -> origin?.endsWith(".myapp.com") == true }
))
```

**With credentials:**
```kotlin
CORSMiddleware(CorsOptions(
    origin = CorsOrigin.Single("https://myapp.com"),
    credentials = true
))
```

!!! tip
    Register `CORSMiddleware` in `preRoutingMiddlewares` so it runs before authentication and other middleware that might halt the pipeline.

!!! warning
    `CorsOrigin.Any` (`*`) cannot be combined with `credentials = true`. Browsers will reject the response. Use `CorsOrigin.Single` or `CorsOrigin.Predicate` when credentials are required.
