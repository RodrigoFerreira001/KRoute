package dev.catbit.kroute.middleware.commons

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
        val requestOrigin = request.headerOrNull("origin")

        // Access-Control-Allow-Origin
        when (val o = options.origin) {
            is CorsOrigin.Any -> response.appendHeader("Access-Control-Allow-Origin", "*")
            is CorsOrigin.None -> Unit
            is CorsOrigin.Single -> {
                response.appendHeader("Access-Control-Allow-Origin", o.origin)
                response.appendHeader("Vary", "Origin")
            }

            is CorsOrigin.Multiple -> {
                if (requestOrigin != null && o.origins.contains(requestOrigin)) {
                    response.appendHeader("Access-Control-Allow-Origin", requestOrigin)
                    response.appendHeader("Vary", "Origin")
                }
            }

            is CorsOrigin.Predicate -> {
                if (o.check(requestOrigin)) {
                    val value = requestOrigin ?: "*"
                    response.appendHeader("Access-Control-Allow-Origin", value)
                    if (requestOrigin != null) response.appendHeader("Vary", "Origin")
                }
            }
        }

        // Access-Control-Allow-Credentials
        if (options.credentials) {
            response.appendHeader("Access-Control-Allow-Credentials", "true")
        }

        // Access-Control-Expose-Headers
        if (options.exposedHeaders.isNotEmpty()) {
            response.appendHeader("Access-Control-Expose-Headers", options.exposedHeaders.joinToString(", "))
        }

        // Preflight
        if (request.method() == HttpMethod.Options.value) {
            response.appendHeader("Access-Control-Allow-Methods", options.methods.joinToString(", "))

            val allowed = options.allowedHeaders
                ?: request.headerOrNull("access-control-request-headers")?.split(",")?.map { it.trim() }
                ?: emptyList()

            if (allowed.isNotEmpty()) {
                response.appendHeader("Access-Control-Allow-Headers", allowed.joinToString(", "))
            }

            if (options.maxAge != null) {
                response.appendHeader("Access-Control-Max-Age", options.maxAge.toString())
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
)