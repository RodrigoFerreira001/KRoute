# Getting Started

## Installation

=== "Version Catalog"

    **`gradle/libs.versions.toml`**
    ```toml
    [versions]
    kroute = "{{ version }}"

    [libraries]
    kroute = { module = "dev.catbit:kroute", version.ref = "kroute" }
    kroute-google-cloud-extension = { module = "dev.catbit:kroute-google-cloud-extension", version.ref = "kroute" }
    ```

    **`build.gradle.kts`**
    ```kotlin
    dependencies {
        implementation(libs.kroute)
        implementation(libs.kroute.google.cloud.extension) // optional
    }
    ```

=== "Direct Coordinates"

    ```kotlin
    dependencies {
        implementation("dev.catbit:kroute:{{ version }}")
        implementation("dev.catbit:kroute-google-cloud-extension:{{ version }}") // optional
    }
    ```

## Step 1 — Define your routes

Each route is a class or object that extends `HttpFunction`. You declare the path, the HTTP method, and optionally a list of middlewares scoped to that route.

```kotlin
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

object CreateUserFunction : HttpFunction(
    path = "/users",
    httpMethod = HttpMethod.Post,
    middlewares = listOf(AuthMiddleware) // (1)
) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        response.setStatusCode(HttpStatusCode.Created)
    }
}
```

1. Middlewares declared here run only for this route, after `defaultMiddlewares`.

## Step 2 — Create the router

```kotlin
val router = HttpFunctionRouter(
    functions = listOf(
        GetUsersFunction,
        GetUserFunction,
        CreateUserFunction
    ),
    preRoutingMiddlewares = listOf(CORSMiddleware),  // (1)
    defaultMiddlewares = listOf(AuthMiddleware),      // (2)
    onError = { e -> logger.error(e) }
)
```

1. Runs before route matching — ideal for CORS.
2. Runs after matching, for every function — ideal for authentication.

## Step 3 — Route requests

```kotlin
router.route(request, response)
```

That's it. The router matches the request, runs the middleware pipeline, and calls `handleRequest` on the matching function.

!!! tip
    See [Routing](routing.md) for the full path pattern reference, and [Middleware](middleware.md) for how to write your own.
