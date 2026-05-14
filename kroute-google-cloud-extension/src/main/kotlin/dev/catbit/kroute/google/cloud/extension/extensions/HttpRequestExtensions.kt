package dev.catbit.kroute.google.cloud.extension.extensions

import dev.catbit.kroute.google.cloud.extension.base.GoogleCloudHttpRequest
import com.google.cloud.functions.HttpRequest as GCPHttpRequest

fun GCPHttpRequest.toHttpRequest() = GoogleCloudHttpRequest(this)