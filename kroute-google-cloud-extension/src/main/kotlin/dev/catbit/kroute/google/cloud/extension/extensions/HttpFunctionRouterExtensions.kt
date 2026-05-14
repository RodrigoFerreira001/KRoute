package dev.catbit.kroute.google.cloud.extension.extensions

import dev.catbit.kroute.router.HttpFunctionRouter
import com.google.cloud.functions.HttpRequest as GCPHttpRequest
import com.google.cloud.functions.HttpResponse as GCPHttpResponse

fun HttpFunctionRouter.route(
    request: GCPHttpRequest,
    response: GCPHttpResponse
) {
    route(
        request = request.toHttpRequest(),
        response = response.toHttpResponse()
    )
}