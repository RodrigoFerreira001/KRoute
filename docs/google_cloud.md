# Google Cloud Functions

The `kroute-google-cloud-extension` module provides adapters that bridge KRoute's interfaces with the [Google Cloud Functions Framework API](https://github.com/GoogleCloudPlatform/functions-framework-java).

## Installation

```kotlin title="build.gradle.kts"
dependencies {
    implementation("dev.catbit:kroute:{{ version }}")
    implementation("dev.catbit:kroute-google-cloud-extension:{{ version }}")
}
```

## Usage

Create a class that implements `com.google.cloud.functions.HttpFunction`. Use the `route` extension from `dev.catbit.kroute.google.cloud.extension.extensions` to bridge the GCP types with KRoute automatically:

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
        router.route(request, response) // (1)
    }
}
```

1. The `route` extension wraps the GCP `HttpRequest` and `HttpResponse` in KRoute's adapters automatically.

The GCP Functions Framework instantiates your class via reflection using its no-arg constructor. The router is initialized once and reused across requests.

## How it works

The extension module provides lightweight adapters:

| GCP type | KRoute adapter |
|---|---|
| `com.google.cloud.functions.HttpRequest` | `GoogleCloudHttpRequest` |
| `com.google.cloud.functions.HttpResponse` | `GoogleCloudHttpResponse` |
| `com.google.cloud.functions.HttpRequest.HttpPart` | `GoogleCloudHttpPart` |

The `route` extension converts the GCP types transparently — your `HttpFunction` implementations never depend on GCP types directly.

### Convenience extensions

You can also convert types individually:

```kotlin
import dev.catbit.kroute.google.cloud.extension.extensions.toHttpRequest
import dev.catbit.kroute.google.cloud.extension.extensions.toHttpResponse
import dev.catbit.kroute.google.cloud.extension.extensions.toHttpPart

val krouteRequest = gcpRequest.toHttpRequest()
val krouteResponse = gcpResponse.toHttpResponse()
```

## Body and parts caching

`GoogleCloudHttpRequest.body()` and `GoogleCloudHttpPart.body()` are cached — the underlying `InputStream` is read at most once per request lifecycle. Subsequent calls to `body()` return the same `ByteArray`.

`parts()` is also lazily initialized and cached on first access.

!!! warning
    Do not mix `setBody(String)` and `setBody(ByteArray)` on the same response. The Google Cloud Functions framework uses either the response `Writer` or `OutputStream` — calling both will throw an `IllegalStateException`.
