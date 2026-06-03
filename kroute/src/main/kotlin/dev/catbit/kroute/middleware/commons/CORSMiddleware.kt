package dev.catbit.kroute.middleware.commons

import dev.catbit.kroute.base.HttpHeaders
import dev.catbit.kroute.base.HttpMethod
import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.base.HttpStatusCode
import dev.catbit.kroute.extensions.headerOrNull
import dev.catbit.kroute.middleware.HttpFunctionMiddleware

class CORSMiddleware(
    private val options: CorsOptions = CorsOptions()
) : HttpFunctionMiddleware {

    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        val requestOrigin = request.headerOrNull(HttpHeaders.Origin)

        // Access-Control-Allow-Origin
        when (val o = options.origin) {
            is CorsOrigin.Any -> response.appendHeader(HttpHeaders.AccessControlAllowOrigin, "*")
            is CorsOrigin.None -> Unit
            is CorsOrigin.Single -> {
                response.appendHeader(HttpHeaders.AccessControlAllowOrigin, o.origin)
                response.appendHeader(HttpHeaders.Vary, HttpHeaders.Origin)
            }

            is CorsOrigin.Multiple -> {
                if (requestOrigin != null && o.origins.contains(requestOrigin)) {
                    response.appendHeader(HttpHeaders.AccessControlAllowOrigin, requestOrigin)
                    response.appendHeader(HttpHeaders.Vary, HttpHeaders.Origin)
                }
            }

            is CorsOrigin.Predicate -> {
                if (o.check(requestOrigin)) {
                    val value = requestOrigin ?: "*"
                    response.appendHeader(HttpHeaders.AccessControlAllowOrigin, value)
                    if (requestOrigin != null) response.appendHeader(HttpHeaders.Vary, HttpHeaders.Origin)
                }
            }
        }

        // Access-Control-Allow-Credentials
        if (options.credentials) {
            response.appendHeader(HttpHeaders.AccessControlAllowCredentials, "true")
        }

        // Access-Control-Expose-Headers
        if (options.exposedHeaders.isNotEmpty()) {
            response.appendHeader(HttpHeaders.AccessControlExposeHeaders, options.exposedHeaders.joinToString(", "))
        }

        // Preflight
        if (request.method() == HttpMethod.Options.value) {
            response.appendHeader(HttpHeaders.AccessControlAllowMethods, options.methods.joinToString(", ") { it.value })

            val allowed = options.allowedHeaders
                ?: request.headerOrNull(HttpHeaders.AccessControlRequestHeaders)?.split(",")?.map { it.trim() }
                ?: emptyList()

            if (allowed.isNotEmpty()) {
                response.appendHeader(HttpHeaders.AccessControlAllowHeaders, allowed.joinToString(", "))
            }

            if (options.maxAge != null) {
                response.appendHeader(HttpHeaders.AccessControlMaxAge, options.maxAge.toString())
            }

            halt(options.optionsSuccessStatus)
        }

        return proceed(request)
    }
}

sealed class CorsOrigin {
    /** Allow any origin — sets `Access-Control-Allow-Origin: *`. */
    data object Any : CorsOrigin()

    /** Block CORS — does not set `Access-Control-Allow-Origin`. */
    data object None : CorsOrigin()

    /** Allow a single specific origin. */
    data class Single(val origin: String) : CorsOrigin()

    /** Allow a list of specific origins. Reflects the request origin when it matches. */
    data class Multiple(val origins: List<String>) : CorsOrigin()

    /** Allow origins matching the given predicate. Reflects the request origin when it matches. */
    data class Predicate(val check: (origin: String?) -> Boolean) : CorsOrigin()
}

data class CorsOptions(
    val origin: CorsOrigin = CorsOrigin.Any,
    val methods: List<HttpMethod> = listOf(
        HttpMethod.Get,
        HttpMethod.Head,
        HttpMethod.Put,
        HttpMethod.Patch,
        HttpMethod.Post,
        HttpMethod.Delete
    ),
    /** When null, reflects the value of `Access-Control-Request-Headers`. */
    val allowedHeaders: List<String>? = null,
    val exposedHeaders: List<String> = emptyList(),
    val credentials: Boolean = false,
    val maxAge: Int? = null,
    val optionsSuccessStatus: HttpStatusCode = HttpStatusCode.NoContent
) {
    init {
        require(!(credentials && origin is CorsOrigin.Any)) {
            "credentials=true cannot be used with CorsOrigin.Any — browsers reject wildcard origin with credentials"
        }
    }
}