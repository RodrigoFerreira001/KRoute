package dev.catbit.kroute.middleware

import dev.catbit.kroute.base.HttpStatusCode

data class HttpFunctionMiddlewareException(val statusCode: HttpStatusCode) : Throwable()