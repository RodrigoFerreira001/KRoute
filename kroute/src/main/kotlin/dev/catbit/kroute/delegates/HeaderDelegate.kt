package dev.catbit.kroute.delegates

import kotlin.reflect.KProperty

class HeaderDelegate(
    private val headers: Map<String, List<String>>
)  {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return headers.entries.first {
            it.key.equals(property.name, ignoreCase = true)
        }.value.first()
    }
}