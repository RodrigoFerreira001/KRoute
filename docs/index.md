# KRoute

Lightweight, pure Kotlin HTTP routing library.

- **Pure Kotlin** — zero external dependencies in the core library.
- **Function-based routing** — each route is a self-contained `HttpFunction` with its own middleware chain.
- **Smart route matching** — specificity-based routing ensures the most precise route always wins, regardless of registration order.
- **Composable middleware** — request flows through typed pipelines where each middleware can enrich, modify, or halt processing.
- **Extensible** — the core is built on interfaces. Platform adapters (like Google Cloud Functions) plug in as separate modules.

## Quick Start

Add the dependency:

```kotlin title="build.gradle.kts"
dependencies {
    implementation("dev.catbit:kroute:1.0.0")
}
```

Define a route, create a router, and route requests:

```kotlin
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

val router = HttpFunctionRouter(
    functions = listOf(GetUserFunction),
    preRoutingMiddlewares = listOf(CORSMiddleware)
)

router.route(request, response)
```

Check out the [Getting Started](getting_started.md) guide for a complete walkthrough.
