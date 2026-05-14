package dev.catbit.kroute.google.cloud.extension.base

import dev.catbit.kroute.base.HttpResponse
import dev.catbit.kroute.base.HttpStatusCode
import com.google.cloud.functions.HttpResponse as GCPHttpResponse

class GoogleCloudHttpResponse(
    private val gcpResponse: GCPHttpResponse
) : HttpResponse {

    override fun setStatusCode(code: Int) {
        gcpResponse.setStatusCode(code)
    }

    override fun setStatusCode(code: Int, message: String) {
        gcpResponse.setStatusCode(code, message)
    }

    override fun setStatusCode(statusCode: HttpStatusCode) {
        gcpResponse.setStatusCode(statusCode.value, statusCode.description)
    }

    override fun setContentType(contentType: String) {
        gcpResponse.setContentType(contentType)
    }

    override fun appendHeader(header: String, value: String) {
        gcpResponse.appendHeader(header, value)
    }

    override fun headers(): MutableMap<String, MutableList<String>> = gcpResponse.headers

    override fun setBody(stringData: String) {
        gcpResponse.writer.write(stringData)
    }

    override fun setBody(data: ByteArray) {
        gcpResponse.outputStream.write(data)
    }
}