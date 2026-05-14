# Routing

## HttpFunction

Every route is a class or object that extends `HttpFunction`:

```kotlin
abstract class HttpFunction(
    path: String,
    httpMethod: HttpMethod,
    middlewares: List<HttpFunctionMiddleware> = listOf()
)
```

| Parameter | Description |
|---|---|
| `path` | The path pattern for this route (see [Path Patterns](#path-patterns)) |
| `httpMethod` | The HTTP method (`HttpMethod.Get`, `.Post`, `.Put`, `.Patch`, `.Delete`, etc.) |
| `middlewares` | Middlewares scoped to this route, running after `defaultMiddlewares` |

## HttpFunctionRouter

The router matches an incoming request against all registered functions and dispatches it through the middleware pipeline.

```kotlin
class HttpFunctionRouter(
    functions: List<HttpFunction>,
    onError: (Throwable) -> Unit = {},
    defaultMiddlewares: List<HttpFunctionMiddleware> = listOf(),
    preRoutingMiddlewares: List<HttpFunctionMiddleware> = listOf()
)
```

### Middleware pipeline

Requests flow through three layers in order:

```
preRoutingMiddlewares           Runs before route matching (e.g. CORS, logging)
        ↓
[route matched]
        ↓
defaultMiddlewares              Runs for every matched function (e.g. authentication)
        ↓
function.middlewares            Per-route middleware
        ↓
handleRequest()
```

### Error handling

| Scenario | Status Code |
|---|---|
| Middleware calls `halt(statusCode)` | The given status code |
| No route matches the request | `404 Not Found` |
| Unhandled exception in `handleRequest` | `500 Internal Server Error` |

The `onError` callback fires on every failure — use it for centralized logging:

```kotlin
HttpFunctionRouter(
    functions = listOf(...),
    onError = { throwable -> logger.error("Request failed", throwable) }
)
```

## Path Patterns

KRoute supports a rich path pattern syntax for matching and extracting values from URIs.

### Literals

Exact segment match.

```
/users/admin    matches   /users/admin
```

### Named parameters `{name}`

Matches a single path segment and binds it to a name.

```
/users/{id}     matches   /users/42
                          /users/alice
```

### Single wildcard `*`

Matches any single segment without binding it to a name.

```
/users/*        matches   /users/42
                          /users/anything
```

### Path wildcard `**`

Matches one or more segments.

```
/files/**       matches   /files/report.pdf
                          /files/2024/q1/report.pdf
```

### Multiple parameters

```
/v1/{shelf}/books/{book}    matches   /v1/science/books/cosmos
```

### Custom verb `{id}:verb`

Matches a resource action in the style of Google AIP.

```
/users/{id}:activate    matches   /users/42:activate
```

### Complex ID `{a}~{b}`

Matches compound identifiers separated by a delimiter.

```
/v1/{user}~{project}    matches   /v1/alice~my-repo
```

## Specificity-based routing

When multiple patterns match the same request, KRoute automatically selects the most specific one — regardless of the registration order.

Each pattern has a specificity score:

| Segment type | Score |
|---|---|
| Literal | +10 |
| Named parameter `{id}` | +3 |
| Path wildcard `**` | +0 |

**Example:**

```
/users/admin      score 20    ← wins for GET /users/admin
/users/{id}       score 13
/users/**         score 10
/**               score 0     ← catch-all fallback
```

!!! note
    In case of a tie, the registration order is used as a tiebreaker.
