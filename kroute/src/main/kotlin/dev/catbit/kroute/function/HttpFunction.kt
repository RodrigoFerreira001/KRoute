package dev.catbit.kroute.function

import dev.catbit.kroute.base.HttpMethod
import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.middleware.HttpFunctionMiddleware
import dev.catbit.kroute.path.PathPattern

abstract class HttpFunction(
    private val path: String,
    private val httpMethod: HttpMethod,
    val middlewares: List<HttpFunctionMiddleware> = listOf()
) {
    internal val specificityScore: Int
        get() = PathPattern.create(path).specificityScore

    internal fun matches(
        request: HttpRequest
    ) = httpMethod.value == request.method() && PathPattern.create(path).matches(request.path())

    abstract fun handleRequest(request: HttpRequest, response: HttpResponse)

    fun HttpRequest.path(pathId: String): String? {
        return PathPattern.create(this@HttpFunction.path)
            .match(this.path())
            ?.get(pathId)
    }
}
