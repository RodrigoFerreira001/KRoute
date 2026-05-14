package dev.catbit.kroute.google.cloud.extension.extensions

import dev.catbit.kroute.google.cloud.extension.base.GoogleCloudHttpPart
import com.google.cloud.functions.HttpRequest.HttpPart as GCPHttpPart

fun GCPHttpPart.toHttpPart() = GoogleCloudHttpPart(this)