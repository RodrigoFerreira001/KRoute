package dev.catbit.kroute.middleware

import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.base.HttpStatusCode

interface HttpFunctionMiddleware {

    fun Scope.intercept(request: HttpRequest, response: HttpResponse): HttpRequest

    class Scope {
        fun proceed(request: HttpRequest): HttpRequest = request

        fun halt(statusCode: HttpStatusCode): Nothing {
            throw HttpFunctionMiddlewareException(statusCode)
        }
    }
}