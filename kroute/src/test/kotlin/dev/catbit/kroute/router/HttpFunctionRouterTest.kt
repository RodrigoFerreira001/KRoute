package dev.catbit.kroute.router

import dev.catbit.kroute.base.*
import dev.catbit.kroute.function.HttpFunction
import dev.catbit.kroute.middleware.HttpFunctionMiddleware
import dev.catbit.kroute.middleware.HttpFunctionMiddlewareException
import kotlin.test.*

class HttpFunctionRouterTest {

    // --- Test doubles ---

    private fun fakeRequest(method: String, path: String): HttpRequest = object : HttpRequest {
        override fun method() = method
        override fun path() = path
        override fun uri() = path
        override fun query() = null
        override fun queryParameters() = emptyMap<String, List<String>>()
        override fun queryParameter(paramName: String) = null
        override fun parts() = emptyMap<String, HttpPart>()
        override fun contentType() = null
        override fun contentLength() = 0L
        override fun headers() = emptyMap<String, List<String>>()
        override fun characterEncoding() = null
        override fun body() = ByteArray(0)
    }

    private class FakeResponse : HttpResponse {
        var lastStatusCode: HttpStatusCode? = null
        override fun setStatusCode(code: Int) { lastStatusCode = HttpStatusCode(code, "") }
        override fun setStatusCode(code: Int, message: String) { lastStatusCode = HttpStatusCode(code, message) }
        override fun setStatusCode(statusCode: HttpStatusCode) { lastStatusCode = statusCode }
        override fun setContentType(contentType: String) {}
        override fun appendHeader(header: String, value: String) {}
        override fun headers() = mutableMapOf<String, MutableList<String>>()
        override fun setBody(stringData: String) {}
        override fun setBody(data: ByteArray) {}
    }

    private fun function(method: String, path: String, handler: (HttpRequest) -> Unit = {}): HttpFunction =
        object : HttpFunction(path, HttpMethod(method)) {
            override fun handleRequest(request: HttpRequest, response: HttpResponse) = handler(request)
        }

    // --- Tests ---

    @Test
    fun `routes to matching function`() {
        var called = false
        val router = HttpFunctionRouter(listOf(function("GET", "users/{id}") { called = true }))
        router.route(fakeRequest("GET", "users/42"), FakeResponse())
        assertTrue(called)
    }

    @Test
    fun `returns 404 when no function matches`() {
        val router = HttpFunctionRouter(listOf(function("GET", "users/{id}")))
        val response = FakeResponse()
        router.route(fakeRequest("GET", "orders/42"), response)
        assertEquals(HttpStatusCode.NotFound, response.lastStatusCode)
    }

    @Test
    fun `returns 404 when method does not match`() {
        val router = HttpFunctionRouter(listOf(function("GET", "users/{id}")))
        val response = FakeResponse()
        router.route(fakeRequest("POST", "users/42"), response)
        assertEquals(HttpStatusCode.NotFound, response.lastStatusCode)
    }

    @Test
    fun `returns 500 on unexpected exception`() {
        val broken = function("GET", "users/{id}") { throw RuntimeException("boom") }
        val router = HttpFunctionRouter(listOf(broken))
        val response = FakeResponse()
        router.route(fakeRequest("GET", "users/1"), response)
        assertEquals(HttpStatusCode.InternalServerError, response.lastStatusCode)
    }

    @Test
    fun `calls onError on failure`() {
        val errors = mutableListOf<Throwable>()
        val broken = function("GET", "fail") { throw RuntimeException("boom") }
        val router = HttpFunctionRouter(listOf(broken), onError = { errors += it })
        router.route(fakeRequest("GET", "fail"), FakeResponse())
        assertEquals(1, errors.size)
    }

    @Test
    fun `specificity - literal beats param for same prefix`() {
        var matched = ""
        val paramRoute   = function("GET", "users/{id}")   { matched = "param" }
        val literalRoute = function("GET", "users/admin")  { matched = "literal" }

        // Register param first — specificity sort should still pick literal
        val router = HttpFunctionRouter(listOf(paramRoute, literalRoute))
        router.route(fakeRequest("GET", "users/admin"), FakeResponse())
        assertEquals("literal", matched)
    }

    @Test
    fun `specificity - param beats path wildcard`() {
        var matched = ""
        val wildcardRoute = function("GET", "users/**")    { matched = "wildcard" }
        val paramRoute    = function("GET", "users/{id}")  { matched = "param" }

        val router = HttpFunctionRouter(listOf(wildcardRoute, paramRoute))
        router.route(fakeRequest("GET", "users/42"), FakeResponse())
        assertEquals("param", matched)
    }

    @Test
    fun `specificity - longer specific path beats shorter`() {
        var matched = ""
        val short = function("GET", "users/{id}")          { matched = "short" }
        val long  = function("GET", "users/{id}/profile")  { matched = "long" }

        val router = HttpFunctionRouter(listOf(short, long))
        router.route(fakeRequest("GET", "users/42/profile"), FakeResponse())
        assertEquals("long", matched)
    }

    @Test
    fun `pre-routing middleware can halt request`() {
        val authMiddleware = object : HttpFunctionMiddleware {
            override fun HttpFunctionMiddleware.Scope.intercept(request: HttpRequest, response: HttpResponse): HttpRequest {
                halt(HttpStatusCode.Unauthorized)
            }
        }
        val router = HttpFunctionRouter(
            functions = listOf(function("GET", "secure")),
            preRoutingMiddlewares = listOf(authMiddleware)
        )
        val response = FakeResponse()
        router.route(fakeRequest("GET", "secure"), response)
        assertEquals(HttpStatusCode.Unauthorized, response.lastStatusCode)
    }

    @Test
    fun `pre-routing middleware propagates modified request to function`() {
        var receivedPath = ""
        val rewriteMiddleware = object : HttpFunctionMiddleware {
            override fun HttpFunctionMiddleware.Scope.intercept(request: HttpRequest, response: HttpResponse): HttpRequest {
                return proceed(object : HttpRequest by request {
                    override fun path() = "users/42"
                })
            }
        }
        val router = HttpFunctionRouter(
            functions = listOf(function("GET", "users/{id}") { receivedPath = it.path() }),
            preRoutingMiddlewares = listOf(rewriteMiddleware)
        )
        router.route(fakeRequest("GET", "users/42"), FakeResponse())
        assertEquals("users/42", receivedPath)
    }

    @Test
    fun `function-level middleware halts with correct status`() {
        val blockingMiddleware = object : HttpFunctionMiddleware {
            override fun HttpFunctionMiddleware.Scope.intercept(request: HttpRequest, response: HttpResponse): HttpRequest {
                halt(HttpStatusCode.Forbidden)
            }
        }
        val fn = object : HttpFunction("admin", HttpMethod("GET"), listOf(blockingMiddleware)) {
            override fun handleRequest(request: HttpRequest, response: HttpResponse) {}
        }
        val response = FakeResponse()
        HttpFunctionRouter(listOf(fn)).route(fakeRequest("GET", "admin"), response)
        assertEquals(HttpStatusCode.Forbidden, response.lastStatusCode)
    }
}
