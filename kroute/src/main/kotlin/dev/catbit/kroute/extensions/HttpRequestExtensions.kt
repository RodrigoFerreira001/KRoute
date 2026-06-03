package dev.catbit.kroute.extensions

import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.delegates.HeaderDelegate
import dev.catbit.kroute.delegates.NullableHeaderDelegate
import dev.catbit.kroute.delegates.NullablePathDelegate
import dev.catbit.kroute.delegates.PathDelegate
import dev.catbit.kroute.function.HttpFunction

fun HttpRequest.header(headerName: String): String =
    headers().entries.first { it.key.equals(headerName, ignoreCase = true) }.value.first()

fun HttpRequest.headerOrNull(headerName: String): String? =
    headers().entries.firstOrNull { it.key.equals(headerName, ignoreCase = true) }?.value?.firstOrNull()

fun HttpRequest.withHeader(key: String, value: String): HttpRequest {
    val updatedHeaders = headers() + mapOf(key to listOf(value))
    return object : HttpRequest by this {
        override fun headers() = updatedHeaders
    }
}

fun HttpRequest.withHeaders(vararg pairs: Pair<String, String>): HttpRequest {
    val updatedHeaders = headers() + pairs.asSequence().associate { it.first to listOf(it.second) }
    return object : HttpRequest by this {
        override fun headers() = updatedHeaders
    }
}

fun HttpRequest.nullableHeaderValue() = NullableHeaderDelegate(headers())

fun HttpRequest.headerValue() = HeaderDelegate(headers())

fun HttpRequest.pathValue(
    function: HttpFunction
) = PathDelegate(function, this)

fun HttpRequest.nullablePathValue(
    function: HttpFunction
) = NullablePathDelegate(function, this)