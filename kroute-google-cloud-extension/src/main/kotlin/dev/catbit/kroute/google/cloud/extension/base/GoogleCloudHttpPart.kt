package dev.catbit.kroute.google.cloud.extension.base

import dev.catbit.kroute.base.HttpPart
import kotlin.jvm.optionals.getOrNull
import com.google.cloud.functions.HttpRequest.HttpPart as GCPHttpPart

class GoogleCloudHttpPart(
    private val gcpHttpPart: GCPHttpPart
) : HttpPart {
    override fun fileName(): String? = gcpHttpPart.fileName.getOrNull()
    override fun contentType(): String? = gcpHttpPart.contentType.getOrNull()
    override fun contentLength(): Long = gcpHttpPart.contentLength
    override fun headers(): Map<String, List<String>> = gcpHttpPart.headers
    override fun characterEncoding(): String? = gcpHttpPart.characterEncoding.getOrNull()
    private val _body: ByteArray by lazy { gcpHttpPart.inputStream.readAllBytes() }
    override fun body(): ByteArray = _body
}