package dev.catbit.kroute.path

internal data class PathSegment(
    val type: SegmentType,
    val value: String,
    val complexSeparator: String = ""
) {
    fun getSeparator(): String = when (type) {
        SegmentType.CUSTOM_VERB -> ":"
        SegmentType.END_BINDING -> ""
        else -> "/"
    }

    companion object {
        val WILDCARD = PathSegment(SegmentType.WILDCARD, "*")
        val PATH_WILDCARD = PathSegment(SegmentType.PATH_WILDCARD, "**")
        val END_BINDING = PathSegment(SegmentType.END_BINDING, "")
        
        fun wildcard(separator: String) = if (separator.isEmpty()) WILDCARD else PathSegment(SegmentType.WILDCARD, "*", separator)
    }
}
