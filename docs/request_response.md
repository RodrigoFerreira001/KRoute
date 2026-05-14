# Request & Response

## HttpRequest

`HttpRequest` is the interface representing an incoming HTTP request. It provides access to the method, path, headers, query parameters, body, and multipart parts.

```kotlin
interface HttpRequest {
    fun method(): String
    fun uri(): String
    fun path(): String
    fun query(): String?
    fun queryParameters(): Map<String, List<String>>
    fun queryParameter(paramName: String): String?
    fun parts(): Map<String, HttpPart>
    fun contentType(): String?
    fun contentLength(): Long
    fun headers(): Map<String, List<String>>
    fun characterEncoding(): String?
    fun body(): ByteArray
}
```

## Reading path parameters

Inside `handleRequest`, use `pathDelegate` or `nullablePathDelegate` to extract named parameters from the path:

```kotlin
object GetUserFunction : HttpFunction("/users/{id}", HttpMethod.Get) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        val id by request.pathDelegate(this)             // required — throws if absent
        val filter by request.nullablePathDelegate(this) // optional — null if absent
    }
}
```

Or access them directly:

```kotlin
val id = request.path("id")       // String? — null if not present
```

## Reading headers

=== "Delegate (recommended)"

    ```kotlin
    val authorization by request.headerDelegate()      // required — throws if absent
    val token by request.nullableHeaderDelegate()      // optional — null if absent
    ```

    Delegates map the property name to the header name automatically, lowercased by default.
    A property named `authorization` reads the `"authorization"` header.

    To disable lowercasing:
    ```kotlin
    val XCustomHeader by request.headerDelegate(setNameToLowerCase = false)
    ```

=== "Direct access"

    ```kotlin
    val accept = request.header("accept")              // required — throws if absent
    val etag = request.headerOrNull("if-none-match")   // optional — null if absent
    ```

## Enriching requests

Middlewares can add headers to the request using `withHeader`. This returns a new `HttpRequest` with the additional header — the original is never mutated:

```kotlin
return proceed(request.withHeader("userId", userId))
```

The enriched request is passed to subsequent middlewares and to `handleRequest`.

## HttpResponse

`HttpResponse` is the interface representing an outgoing HTTP response.

```kotlin
interface HttpResponse {
    fun setStatusCode(code: Int)
    fun setStatusCode(code: Int, message: String)
    fun setStatusCode(statusCode: HttpStatusCode)
    fun setContentType(contentType: String)
    fun appendHeader(header: String, value: String)
    fun headers(): MutableMap<String, MutableList<String>>
    fun setBody(stringData: String)
    fun setBody(data: ByteArray)
}
```

## Setting the status code

```kotlin
response.setStatusCode(HttpStatusCode.OK)
response.setStatusCode(HttpStatusCode.Created)
response.setStatusCode(HttpStatusCode.NotFound)

// Or with a raw code
response.setStatusCode(200)
response.setStatusCode(200, "All good")
```

## Setting the body

```kotlin
response.setContentType("application/json")
response.setBody("""{"id": 1, "name": "Alice"}""")

// Or as bytes
response.setBody(byteArrayOf(...))
```

## HttpStatusCode

`HttpStatusCode` is a data class with a numeric `value` and a `description`. Common codes are available as constants:

```kotlin
HttpStatusCode.OK                    // 200
HttpStatusCode.Created               // 201
HttpStatusCode.NoContent             // 204
HttpStatusCode.BadRequest            // 400
HttpStatusCode.Unauthorized          // 401
HttpStatusCode.Forbidden             // 403
HttpStatusCode.NotFound              // 404
HttpStatusCode.InternalServerError   // 500
```

You can also create custom status codes:

```kotlin
val CustomStatus = HttpStatusCode(418, "I'm a Teapot")
```

## HttpMethod

```kotlin
HttpMethod.Get
HttpMethod.Post
HttpMethod.Put
HttpMethod.Patch
HttpMethod.Delete
HttpMethod.Head
HttpMethod.Options
```
