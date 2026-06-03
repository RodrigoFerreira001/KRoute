package dev.catbit.kroute.delegates

import kotlin.reflect.KProperty

class NullableHeaderDelegate(
    private val headers: Map<String, List<String>>,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return headers.entries.firstOrNull {
            it.key.equals(property.name, ignoreCase = true)
        }?.value?.firstOrNull()
    }
}