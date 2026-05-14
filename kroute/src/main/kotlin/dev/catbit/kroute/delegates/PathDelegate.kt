package dev.catbit.kroute.delegates

import dev.catbit.kroute.base.HttpRequest
import dev.catbit.kroute.function.HttpFunction
import kotlin.reflect.KProperty

class PathDelegate(
    private val function: HttpFunction,
    private val request: HttpRequest
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return with(function) {
            request.path(property.name)
                ?: throw IllegalArgumentException("${property.name} not present on path ${request.path()}")
        }
    }
}