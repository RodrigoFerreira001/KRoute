<p align="center">
  <img src="docs/media/logo.png" alt="KRoute" width="300">
</p>

<p align="center">
  <strong>Lightweight, pure Kotlin HTTP routing library.</strong>
</p>

<p align="center">
  <a href="https://search.maven.org/search?q=g:dev.catbit+a:kroute"><img src="https://img.shields.io/maven-central/v/dev.catbit/kroute?label=Maven%20Central&color=2A6DB2" alt="Maven Central"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://github.com/RodrigoFerreira001/KRoute/actions"><img src="https://img.shields.io/github/actions/workflow/status/RodrigoFerreira001/KRoute/build.yml?label=Build" alt="Build"></a>
  <a href="https://github.com/RodrigoFerreira001/KRoute/blob/main/LICENSE"><img src="https://img.shields.io/github/license/RodrigoFerreira001/KRoute" alt="License"></a>
</p>

<br>

KRoute is a lightweight, pure Kotlin HTTP routing library. It provides a clean, function-based model for defining routes, composable middleware pipelines, and powerful path pattern matching — with no runtime reflection and no framework lock-in.

- **Pure Kotlin** — zero external dependencies in the core library.
- **Function-based routing** — each route is a self-contained `HttpFunction` with its own middleware chain.
- **Smart route matching** — specificity-based routing ensures the most precise route always wins, regardless of registration order.
- **Composable middleware** — request flows through typed pipelines where each middleware can enrich, modify, or halt processing.
- **Extensible** — the core is built on interfaces. Platform adapters (like Google Cloud Functions) plug in as separate modules.

---

## Download

Add the dependency using Gradle version catalog:

**`gradle/libs.versions.toml`**
```toml
[versions]
kroute = "1.0.0"

[libraries]
kroute = { module = "dev.catbit:kroute", version.ref = "kroute" }
kroute-google-cloud-extension = { module = "dev.catbit:kroute-google-cloud-extension", version.ref = "kroute" }
```

**`build.gradle.kts`**
```kotlin
dependencies {
    // Core library
    implementation(libs.kroute)

    // Google Cloud Functions adapter (optional)
    implementation(libs.kroute.google.cloud.extension)
}
```

Or with direct coordinates:

```kotlin
dependencies {
    implementation("dev.catbit:kroute:1.0.0")
    implementation("dev.catbit:kroute-google-cloud-extension:1.0.0") // optional
}
```

---

## Quick Start

```kotlin
// 1. Define your routes
object GetUsersFunction : HttpFunction(
    path = "/users",
    httpMethod = HttpMethod.Get
) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        response.setBody("""[{"id": 1, "name": "Alice"}]""")
        response.setStatusCode(HttpStatusCode.OK)
    }
}

object GetUserFunction : HttpFunction(
    path = "/users/{id}",
    httpMethod = HttpMethod.Get
) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        val id by request.pathDelegate(this)
        response.setBody("""{"id": "$id"}""")
        response.setStatusCode(HttpStatusCode.OK)
    }
}

// 2. Create the router
val router = HttpFunctionRouter(
    functions = listOf(GetUsersFunction, GetUserFunction),
    preRoutingMiddlewares = listOf(CORSMiddleware)
)

// 3. Route a request
router.route(request, response)
```

---

## Core Concepts

### Defining Routes

Every route is a class or object that extends `HttpFunction`. You declare the path pattern, the HTTP method, and an optional list of middlewares scoped to that route.

```kotlin
object CreateUserFunction : HttpFunction(
    path = "/users",
    httpMethod = HttpMethod.Post,
    middlewares = listOf(AuthMiddleware)
) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        // handle request
        response.setStatusCode(HttpStatusCode.Created)
    }
}
```

### The Router

`HttpFunctionRouter` accepts your functions and dispatches incoming requests. It supports three middleware layers that execute in order:

```
preRoutingMiddlewares → [match route] → defaultMiddlewares → function.middlewares → handleRequest
```

```kotlin
val router = HttpFunctionRouter(
    functions = listOf(
        GetUsersFunction,
        GetUserFunction,
        CreateUserFunction,
        DeleteUserFunction
    ),
    preRoutingMiddlewares = listOf(CORSMiddleware),   // run before matching
    defaultMiddlewares = listOf(AuthMiddleware),       // run after matching, before every function
    onError = { throwable -> logger.error(throwable) }
)
```

**Specificity-based routing** — when multiple patterns match a request, KRoute picks the most specific one automatically. No need to worry about registration order.

```
/users/admin      →  score 20   ← wins for GET /users/admin
/users/{id}       →  score 13
/users/**         →  score 10
/**               →  score 0    ← catch-all fallback
```

### Middleware

Middlewares implement `HttpFunctionMiddleware` and operate on a typed `Scope` that exposes two operations:

- `proceed(request)` — continue with a (potentially modified) request.
- `halt(statusCode)` — short-circuit the pipeline and return immediately.

```kotlin
val AuthMiddleware = object : HttpFunctionMiddleware {
    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val token = request.headerOrNull("authorization")
            ?: halt(HttpStatusCode.Unauthorized)

        val userId = verifyToken(token) ?: halt(HttpStatusCode.Unauthorized)

        // Enrich the request and continue
        return proceed(request.withHeader("userId", userId))
    }
}
```

Middlewares further down the pipeline receive the enriched request:

```kotlin
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

#### Built-in Middlewares

KRoute ships with `CORSMiddleware` out of the box. It adds `Access-Control-Allow-Origin: *` to all responses and handles `OPTIONS` preflight automatically:

```kotlin
val router = HttpFunctionRouter(
    functions = listOf(...),
    preRoutingMiddlewares = listOf(CORSMiddleware)
)
```

### Path Patterns

KRoute's path engine supports a rich pattern syntax:

| Pattern | Description | Example match |
|---|---|---|
| `/users/admin` | Literal segment | `/users/admin` |
| `/users/{id}` | Named parameter | `/users/42` |
| `/users/*` | Single wildcard | `/users/anything` |
| `/files/**` | Path wildcard (multi-segment) | `/files/a/b/c` |
| `/v1/{shelf}/books/{book}` | Multiple parameters | `/v1/science/books/cosmos` |
| `/v1/{id}:publish` | Custom verb | `/v1/42:publish` |
| `/v1/{user}~{project}` | Complex ID | `/v1/alice~my-repo` |

### Request Parameters

#### Path Parameters

```kotlin
object GetUserFunction : HttpFunction("/users/{id}", HttpMethod.Get) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        // Delegate (recommended) — throws if not present
        val id by request.pathDelegate(this)

        // Nullable delegate — returns null if not present
        val filter by request.nullablePathDelegate(this)

        // Direct access
        val id = request.path("id")
    }
}
```

#### Headers

```kotlin
override fun handleRequest(request: HttpRequest, response: HttpResponse) {
    // Delegate — throws NoSuchElementException if header is missing
    val authorization by request.headerDelegate()

    // Nullable delegate — returns null if header is missing
    val token by request.nullableHeaderDelegate()

    // Direct access
    val accept = request.header("accept")
    val etag = request.headerOrNull("if-none-match")
}
```

> By default, delegates look up headers by the property name lowercased. A property named `authorization` maps to the `"authorization"` header automatically.

#### Enriching Requests in Middleware

Use `withHeader` to pass data between middlewares without mutating the original request:

```kotlin
return proceed(request.withHeader("userId", userId))
```

---

## Google Cloud Functions

The `kroute-google-cloud-extension` module provides adapters that bridge KRoute's interfaces with the Google Cloud Functions framework API.

### Setup

```kotlin
dependencies {
    implementation("dev.catbit:kroute:1.0.0")
    implementation("dev.catbit:kroute-google-cloud-extension:1.0.0")
}
```

### Usage

Create your `HttpFunction` entry point and call `router.route()` — the extension automatically wraps the GCP request and response:

```kotlin
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import dev.catbit.kroute.google.cloud.extension.extensions.route
import dev.catbit.kroute.middleware.commons.CORSMiddleware
import dev.catbit.kroute.router.HttpFunctionRouter

class MyApi : HttpFunction {

    private val router = HttpFunctionRouter(
        functions = listOf(
            GetUsersFunction,
            GetUserFunction,
            CreateUserFunction,
            DeleteUserFunction
        ),
        preRoutingMiddlewares = listOf(CORSMiddleware),
        defaultMiddlewares = listOf(AuthMiddleware),
        onError = { e -> System.getLogger("MyApi").log(System.Logger.Level.ERROR, e) }
    )

    override fun service(request: HttpRequest, response: HttpResponse) {
        router.route(request, response)
    }
}
```

### Multipart Support

`GoogleCloudHttpRequest.parts()` is fully supported and lazily initialized. `body()` is cached — the underlying input stream is read at most once per request lifecycle.

---

## Error Handling

The router catches exceptions from the pipeline and maps them to status codes:

| Scenario | Status Code |
|---|---|
| Middleware calls `halt(statusCode)` | The given status code |
| No route matches the request | `404 Not Found` |
| Unhandled exception in `handleRequest` | `500 Internal Server Error` |

The `onError` callback is invoked for every exception, giving you a single place for logging:

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    onError = { throwable ->
        logger.error("Request failed", throwable)
    }
)
```

---

## License

```
Copyright 2025 CatBit

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
