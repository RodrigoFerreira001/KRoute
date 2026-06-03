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

Inside `handleRequest`, use `pathValue` or `nullablePathValue` to extract named parameters from the path:

```kotlin
object GetUserFunction : HttpFunction("/users/{id}", HttpMethod.Get) {
    override fun handleRequest(request: HttpRequest, response: HttpResponse) {
        val id by request.pathValue(this)             // required — throws if absent
        val filter by request.nullablePathValue(this) // optional — null if absent
    }
}
```

## Reading headers

=== "Delegate (recommended)"

    ```kotlin
    val authorization by request.headerValue()      // required — throws if absent
    val token by request.nullableHeaderValue()      // optional — null if absent
    ```

    Delegates map the property name to the header name automatically, using case-insensitive matching.
    A property named `authorization` reads the `"Authorization"` header regardless of casing.

=== "Direct access"

    ```kotlin
    val accept = request.header("Accept")              // required — throws if absent
    val etag = request.headerOrNull("If-None-Match")   // optional — null if absent
    ```

    Header lookup is case-insensitive. Prefer using `HttpHeaders` constants to avoid typos:

    ```kotlin
    val accept = request.header(HttpHeaders.Accept)
    val etag = request.headerOrNull(HttpHeaders.IfNoneMatch)
    ```

## Enriching requests

Middlewares can add headers to the request using `withHeader`. This returns a new `HttpRequest` with the additional header — the original is never mutated:

```kotlin
return proceed(request.withHeader("userId", userId))
```

To add multiple headers at once, use `withHeaders`:

```kotlin
return proceed(
    request.withHeaders(
        "userId" to userId,
        "userRole" to role
    )
)
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
    fun setContentType(contentType: HttpContentType)
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
// Using HttpContentType (recommended)
response.setContentType(HttpContentType.Application.Json)
response.setBody("""{"id": 1, "name": "Alice"}""")

// Or with a raw string
response.setContentType("application/json")
response.setBody("""{"id": 1, "name": "Alice"}""")

// Or as bytes
response.setBody(byteArrayOf(...))
```

## HttpContentType

`HttpContentType` represents a `Content-Type` header value with type, subtype, and optional parameters. Common types are available as constants organized by category:

```kotlin
HttpContentType.Application.Json          // application/json
HttpContentType.Application.Xml           // application/xml
HttpContentType.Application.OctetStream   // application/octet-stream
HttpContentType.Application.FormUrlEncoded // application/x-www-form-urlencoded
HttpContentType.Text.Plain                // text/plain
HttpContentType.Text.Html                 // text/html
HttpContentType.Text.EventStream          // text/event-stream
HttpContentType.MultiPart.FormData        // multipart/form-data
HttpContentType.Image.PNG                 // image/png
HttpContentType.Image.JPEG                // image/jpeg
```

You can add parameters, such as charset:

```kotlin
val contentType = HttpContentType.Text.Plain.withCharset("UTF-8")
// text/plain; charset=UTF-8
```

Parse from a string or match against a pattern:

```kotlin
val parsed = HttpContentType.parse("application/json; charset=UTF-8")

// Wildcard matching — is this an application/* type?
parsed.match(HttpContentType.Application.Any) // true
```

Check category membership with the `in` operator:

```kotlin
if (HttpContentType.Application.Json in HttpContentType.Application) { ... }
if ("application/json" in HttpContentType.Application) { ... }
```

## HttpHeaders

`HttpHeaders` is an object that provides constants for standard HTTP header names, organized by category:

```kotlin
// General
HttpHeaders.CacheControl           // "Cache-Control"
HttpHeaders.ContentType            // "Content-Type"
HttpHeaders.ContentLength          // "Content-Length"

// Request
HttpHeaders.Authorization          // "Authorization"
HttpHeaders.Accept                 // "Accept"
HttpHeaders.Host                   // "Host"
HttpHeaders.Origin                 // "Origin"
HttpHeaders.UserAgent              // "User-Agent"

// Response
HttpHeaders.Location               // "Location"
HttpHeaders.ETag                   // "ETag"
HttpHeaders.SetCookie              // "Set-Cookie"
HttpHeaders.Vary                   // "Vary"

// CORS
HttpHeaders.AccessControlAllowOrigin    // "Access-Control-Allow-Origin"
HttpHeaders.AccessControlAllowMethods   // "Access-Control-Allow-Methods"
HttpHeaders.AccessControlAllowHeaders   // "Access-Control-Allow-Headers"
HttpHeaders.AccessControlAllowCredentials // "Access-Control-Allow-Credentials"
```

Use `HttpHeaders.isUnsafe(header)` to check whether a header is engine-controlled (`Transfer-Encoding`, `Upgrade`) and should not be set manually.

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
