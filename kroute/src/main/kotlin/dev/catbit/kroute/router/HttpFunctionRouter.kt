package dev.catbit.kroute.router

import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.base.HttpStatusCode
import dev.catbit.kroute.function.HttpFunction
import dev.catbit.kroute.middleware.HttpFunctionMiddleware
import dev.catbit.kroute.middleware.HttpFunctionMiddlewareException

class HttpFunctionRouter(
    functions: List<HttpFunction>,
    private val onError: (Throwable) -> Unit = {},
    private val defaultMiddlewares: List<HttpFunctionMiddleware> = listOf(),
    private val preRoutingMiddlewares: List<HttpFunctionMiddleware> = listOf()
) {
    private val functions = functions.sortedByDescending { it.specificityScore }

    fun route(
        request: HttpRequest,
        response: HttpResponse
    ) {
        try {
            with(HttpFunctionMiddleware.Scope()) {

                val preRoutingRequest = preRoutingMiddlewares.fold(request) { req, middleware ->
                    with(middleware) {
                        intercept(
                            request = req,
                            response = response
                        )
                    }
                }

                with(functions.first { it.matches(preRoutingRequest) }) {
                    val updatedRequest = (defaultMiddlewares + middlewares).fold(preRoutingRequest) { req, middleware ->
                        with(middleware) {
                            intercept(
                                request = req,
                                response = response
                            )
                        }
                    }

                    handleRequest(updatedRequest, response)
                }
            }
        } catch (e: HttpFunctionMiddlewareException) {
            onError(e)
            response.setStatusCode(e.statusCode)
        } catch (e: NoSuchElementException) {
            onError(e)
            response.setStatusCode(HttpStatusCode.NotFound)
        } catch (e: Throwable) {
            onError(e)
            response.setStatusCode(HttpStatusCode.InternalServerError)
        }
    }
}





