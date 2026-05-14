package dev.catbit.kroute.google.cloud.extension.extensions

import dev.catbit.kroute.google.cloud.extension.base.GoogleCloudHttpResponse
import com.google.cloud.functions.HttpResponse as GCPHttpResponse

fun GCPHttpResponse.toHttpResponse() = GoogleCloudHttpResponse(this)
