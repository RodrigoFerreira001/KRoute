package dev.catbit.kroute.delegates

import kotlin.reflect.KProperty

class NullableHeaderDelegate(
    private val headers: Map<String, List<String>>,
    private val setNameToLowerCase: Boolean = true
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        val name = if (setNameToLowerCase) {
            property.name.lowercase()
        } else property.name

        return headers[name]?.firstOrNull()
    }
}