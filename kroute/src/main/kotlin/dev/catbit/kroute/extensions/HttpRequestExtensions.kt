package dev.catbit.kroute.extensions

import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.delegates.HeaderDelegate
import dev.catbit.kroute.delegates.NullableHeaderDelegate
import dev.catbit.kroute.delegates.NullablePathDelegate
import dev.catbit.kroute.delegates.PathDelegate
import dev.catbit.kroute.function.HttpFunction

fun HttpRequest.header(headerName: String): String? = headers().getValue(headerName).first()

fun HttpRequest.headerOrNull(headerName: String): String? = headers()[headerName]?.firstOrNull()

fun HttpRequest.withHeader(key: String, value: String): HttpRequest {
    val updatedHeaders = headers() + mapOf(key to listOf(value))
    return object : HttpRequest by this {
        override fun headers() = updatedHeaders
    }
}

fun HttpRequest.nullableHeaderDelegate(
    setNameToLowerCase: Boolean = true
) = NullableHeaderDelegate(headers(), setNameToLowerCase)

fun HttpRequest.headerDelegate(
    setNameToLowerCase: Boolean = true
) = HeaderDelegate(headers(), setNameToLowerCase)

fun HttpRequest.pathDelegate(
    function: HttpFunction
) = PathDelegate(function, this)

fun HttpRequest.nullablePathDelegate(
    function: HttpFunction
) = NullablePathDelegate(function, this)