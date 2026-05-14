package dev.catbit.kroute.middleware.commons

import dev.catbit.kroute.base.HttpMethod
import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.base.HttpStatusCode
import dev.catbit.kroute.middleware.HttpFunctionMiddleware

val CORSMiddleware = object : HttpFunctionMiddleware {

    override fun HttpFunctionMiddleware.Scope.intercept(
        request: HttpRequest,
        response: HttpResponse
    ): HttpRequest {
        response.appendHeader("Access-Control-Allow-Origin", "*")

        return if (request.method() == HttpMethod.Options.value) {
            with(response) {
                appendHeader("Access-Control-Allow-Methods", "*")
                appendHeader("Access-Control-Allow-Headers", "*")
                appendHeader("Access-Control-Max-Age", "86400")
            }
            halt(HttpStatusCode.NoContent)
        } else proceed(request)
    }
}