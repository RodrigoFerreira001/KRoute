package dev.catbit.kroute.google.cloud.extension.base

import dev.catbit.kroute.base.HttpPart
import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.google.cloud.extension.extensions.toHttpPart
import kotlin.jvm.optionals.getOrNull
import com.google.cloud.functions.HttpRequest as GCPHttpRequest

class GoogleCloudHttpRequest(
    private val gcpRequest: GCPHttpRequest
) : HttpRequest {

    override fun method(): String = gcpRequest.method

    override fun uri(): String = gcpRequest.uri

    override fun path(): String = gcpRequest.path

    override fun query(): String? = gcpRequest.query.getOrNull()

    override fun queryParameters(): Map<String, List<String>> = gcpRequest.queryParameters

    override fun queryParameter(paramName: String): String? = gcpRequest.getFirstQueryParameter(paramName).getOrNull()

    private val _parts: Map<String, HttpPart> by lazy { gcpRequest.parts.mapValues { it.value.toHttpPart() } }
    override fun parts(): Map<String, HttpPart> = _parts

    override fun contentType(): String? = gcpRequest.contentType.getOrNull()

    override fun contentLength(): Long = gcpRequest.contentLength

    override fun headers(): Map<String, List<String>> = gcpRequest.headers

    override fun characterEncoding(): String? = gcpRequest.characterEncoding.getOrNull()

    private val _body: ByteArray by lazy { gcpRequest.inputStream.readAllBytes() }
    override fun body(): ByteArray = _body
}