package dev.catbit.kroute.path

internal enum class SegmentType {
    LITERAL, CUSTOM_VERB, WILDCARD, PATH_WILDCARD, BINDING, END_BINDING
}
