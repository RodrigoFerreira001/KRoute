package dev.catbit.kroute.path

/**
 * Represents a path pattern for matching and instantiating URIs.
 *
 * Patterns support literals, wildcards (`*`, `**`), and variable bindings (`{name=sub-path}`).
 * For example: `v1/shelves/{shelf}/books/{book}`.
 */
class PathPattern private constructor(
    private val segments: List<PathSegment>,
    private val urlEncoding: Boolean = true
) {

    private val bindings: Map<String, PathSegment> = buildMap {
        segments.filter { it.type == SegmentType.BINDING }.forEach { seg ->
            if (put(seg.value, seg) != null) throw PathException("Duplicate binding '${seg.value}'")
        }
    }

    /**
     * Returns the set of variable names defined in this pattern.
     */
    val vars: Set<String> get() = bindings.keys

    /**
     * Returns the name of the variable if this pattern contains exactly one binding.
     */
    val singleVar: String? get() = vars.singleOrNull()

    /**
     * Returns a specificity score for this pattern.
     * Higher score means more specific — used to determine match priority.
     * LITERAL (+10) > WILDCARD/binding (+3) > PATH_WILDCARD (0)
     */
    val specificityScore: Int get() = segments.sumOf { seg ->
        when (seg.type) {
            SegmentType.LITERAL -> 10
            SegmentType.WILDCARD -> 3
            else -> 0
        }
    }

    /**
     * Returns true if the pattern ends with a literal segment.
     */
    val endsWithLiteral: Boolean get() = segments.lastOrNull()?.type == SegmentType.LITERAL

    /**
     * Returns true if the pattern ends with a custom verb.
     */
    val endsWithCustomVerb: Boolean get() = segments.lastOrNull()?.type == SegmentType.CUSTOM_VERB

    /**
     * Checks if this pattern matches the given path.
     */
    fun matches(path: String): Boolean = match(path) != null

    /**
     * Returns a pattern for the parent of this pattern.
     */
    fun parentPattern(): PathPattern {
        var i = segments.size - 1
        if (i < 0) throw PathException("Pattern is empty")
        if (segments[i].type == SegmentType.END_BINDING) {
            while (i > 0 && segments[--i].type != SegmentType.BINDING) { /* find start */ }
        }
        if (i <= 0) throw PathException("Pattern does not have a parent")
        return PathPattern(segments.subList(0, i), urlEncoding)
    }

    /**
     * Returns a pattern where all variable bindings have been replaced by wildcards.
     */
    fun withoutVars(): PathPattern {
        val syntax = buildString {
            var start = true
            segments.forEach { seg ->
                if (seg.type != SegmentType.BINDING && seg.type != SegmentType.END_BINDING) {
                    if (!start) append(seg.getSeparator()) else start = false
                    append(seg.value)
                }
            }
        }
        return create(syntax, urlEncoding)
    }

    /**
     * Returns a path pattern for the sub-path of the given variable.
     */
    fun subPattern(varName: String): PathPattern {
        val sub = mutableListOf<PathSegment>()
        var inBinding = false
        for (seg in segments) {
            if (seg.type == SegmentType.BINDING && seg.value == varName) {
                inBinding = true
            } else if (inBinding) {
                if (seg.type == SegmentType.END_BINDING) {
                    return create(toSyntax(sub, pretty = false), urlEncoding)
                } else {
                    sub.add(seg)
                }
            }
        }
        throw PathException("Variable '$varName' is undefined")
    }

    /**
     * Matches a path against this pattern, returning a map of variable names to their values.
     * Values are URL-decoded if [urlEncoding] is enabled.
     * Returns null if the path does not match.
     */
    fun match(path: String): Map<String, String>? = matchInternal(path, forceHostName = false)

    /**
     * Matches a full name (including hostname) against this pattern.
     */
    fun matchFromFullName(path: String): Map<String, String>? = matchInternal(path, forceHostName = true)

    private fun matchInternal(path: String, forceHostName: Boolean): Map<String, String>? {
        var currentPath = path
        val lastSeg = segments.lastOrNull() ?: return null

        if (lastSeg.type == SegmentType.CUSTOM_VERB) {
            val matcher = CUSTOM_VERB_REGEX.find(currentPath)
            if (matcher == null || decode(matcher.groupValues[1]) != lastSeg.value) return null
            currentPath = currentPath.substring(0, matcher.range.first)
        }

        val hostMatcher = HOSTNAME_REGEX.find(currentPath)
        val hasHost = hostMatcher != null
        if (hasHost) {
            currentPath = currentPath.replaceFirst(HOSTNAME_REGEX, "")
        }

        val input = currentPath.split('/').filter { it.isNotBlank() }.toMutableList()
        var inPos = 0
        val values = mutableMapOf<String, String>()

        if (hasHost || forceHostName) {
            if (input.isEmpty()) return null
            var hostName = input[inPos++]
            if (hasHost) {
                hostName = hostMatcher.groupValues[0] + hostName
            }
            values[HOSTNAME_VAR] = hostName
        }

        if (hasHost && segments.isNotEmpty()) {
            inPos = alignInput(input, inPos, segments[0])
        }

        return if (performMatch(input, inPos, segments, values)) {
            values.toMap()
        } else null
    }

    private fun alignInput(input: List<String>, inPos: Int, segment: PathSegment): Int {
        var pos = inPos
        val target = when (segment.type) {
            SegmentType.BINDING -> segment.value + "s"
            SegmentType.LITERAL -> segment.value
            else -> return pos
        }
        while (pos < input.size && input[pos] != target) {
            pos++
        }
        return if (segment.type == SegmentType.BINDING && pos < input.size) pos + 1 else pos
    }

    private fun performMatch(
        input: List<String>,
        startInPos: Int,
        segments: List<PathSegment>,
        values: MutableMap<String, String>
    ): Boolean {
        var inPos = startInPos
        var segPos = 0
        var currentVar: String? = null
        val modifiableInput = input.toMutableList()

        while (segPos < segments.size) {
            val seg = segments[segPos++]
            when (seg.type) {
                SegmentType.END_BINDING -> currentVar = null
                SegmentType.BINDING -> currentVar = seg.value
                SegmentType.CUSTOM_VERB -> {} // Handled upfront
                SegmentType.LITERAL, SegmentType.WILDCARD -> {
                    if (inPos >= modifiableInput.size) return false
                    var next = decode(modifiableInput[inPos++])
                    if (seg.type == SegmentType.LITERAL && seg.value != next) return false
                    
                    if (seg.type == SegmentType.WILDCARD && seg.complexSeparator.isNotEmpty()) {
                        val idx = next.indexOf(seg.complexSeparator)
                        if (idx >= 0) {
                            modifiableInput.add(inPos, next.substring(idx + 1))
                            next = next.substring(0, idx)
                        } else return false
                    }
                    
                    if (currentVar != null) {
                        values[currentVar] = if (values[currentVar] == null) next else "${values[currentVar]}/$next"
                    }
                }
                SegmentType.PATH_WILDCARD -> {
                    var segsToMatch = 0
                    for (i in segPos until segments.size) {
                        if (segments[i].type != SegmentType.BINDING && segments[i].type != SegmentType.END_BINDING && segments[i].type != SegmentType.CUSTOM_VERB) {
                            segsToMatch++
                        }
                    }
                    var available = (modifiableInput.size - inPos) - segsToMatch
                    val varName = currentVar ?: throw PathException("PATH_WILDCARD outside binding")
                    if (available == 0 && !values.containsKey(varName)) {
                        values[varName] = ""
                    }
                    while (available-- > 0) {
                        val decoded = decode(modifiableInput[inPos++])
                        values[varName] = values[varName]?.let { "$it/$decoded" } ?: decoded
                    }
                }
            }
        }
        return inPos == modifiableInput.size
    }

    /**
     * Instantiates the pattern using the provided variables.
     */
    fun instantiate(values: Map<String, String>): String = instantiateInternal(values)

    /**
     * Instantiates the pattern using positional parameters (vararg).
     */
    fun instantiate(vararg keysAndValues: String): String {
        val map = mutableMapOf<String, String>()
        for (i in keysAndValues.indices step 2) {
            if (i + 1 < keysAndValues.size) {
                map[keysAndValues[i]] = keysAndValues[i + 1]
            }
        }
        return instantiate(map)
    }

    private fun instantiateInternal(values: Map<String, String>): String {
        return buildString {
            values[HOSTNAME_VAR]?.let {
                append(it).append('/')
            }

            var skip = false
            var continueLast = true
            var prevSeparator = ""
            val iterator = segments.listIterator()

            while (iterator.hasNext()) {
                val seg = iterator.next()
                if (!skip && !continueLast) {
                    val separator = if (prevSeparator.isEmpty() || !iterator.hasNext()) seg.getSeparator() else prevSeparator
                    append(separator)
                    prevSeparator = if (seg.complexSeparator.isEmpty()) seg.getSeparator() else seg.complexSeparator
                }
                continueLast = false

                when (seg.type) {
                    SegmentType.BINDING -> {
                        val varName = seg.value
                        val value = values[varName] ?: throw PathException("Unbound variable '$varName'")

                        val next = iterator.next()
                        val nextNext = iterator.next()
                        val pathEscape = next.type == SegmentType.PATH_WILDCARD || nextNext.type != SegmentType.END_BINDING

                        // Move iterator back
                        iterator.previous(); iterator.previous()

                        if (!pathEscape) {
                            append(encode(value))
                        } else {
                            append(value.split('/').joinToString("/") { encode(it) })
                        }
                        skip = true
                    }
                    SegmentType.END_BINDING -> {
                        if (!skip) append('}')
                        skip = false
                    }
                    else -> if (!skip) append(seg.value)
                }
            }
        }
    }

    private fun encode(text: String): String {
        if (!urlEncoding) {
            if (text.contains("/")) throw PathException("Invalid character '/' in path section '$text'")
            return text
        }
        return UrlCodec.encode(text)
    }

    private fun decode(text: String) = if (urlEncoding) UrlCodec.decode(text) else text

    override fun toString(): String = toSyntax(segments, pretty = true)
    fun toRawString(): String = toSyntax(segments, pretty = false)

    private fun toSyntax(segments: List<PathSegment>, pretty: Boolean): String {
        return buildString {
            var continueLast = true
            val iterator = segments.listIterator()

            while (iterator.hasNext()) {
                val seg = iterator.next()
                if (!continueLast) append(seg.getSeparator())
                continueLast = false

                when (seg.type) {
                    SegmentType.BINDING -> {
                        if (pretty && seg.value.startsWith("$")) {
                            val inner = iterator.next()
                            append(inner.value)
                            iterator.next()
                            continue
                        }
                        append('{').append(seg.value)
                        if (pretty && consumeIfMatches(iterator, SegmentType.WILDCARD, SegmentType.END_BINDING)) {
                            append('}')
                            continue
                        }
                        append('=')
                        continueLast = true
                    }
                    SegmentType.END_BINDING -> append('}')
                    else -> append(seg.value)
                }
            }
        }
    }

    private fun consumeIfMatches(it: ListIterator<PathSegment>, vararg types: SegmentType): Boolean {
        val start = it.nextIndex()
        for (type in types) {
            if (!it.hasNext() || it.next().type != type) {
                while (it.nextIndex() > start) it.previous()
                return false
            }
        }
        return true
    }

    override fun equals(other: Any?): Boolean = other is PathPattern && segments == other.segments
    override fun hashCode(): Int = segments.hashCode()

    companion object {
        const val HOSTNAME_VAR = "\$hostname"
        
        private val CUSTOM_VERB_REGEX = ":([^/*}{=]+)$".toRegex()
        private val HOSTNAME_REGEX = "^(\\w+:)?//".toRegex()
        private val MULTIPLE_DELIMITER_REGEX = "\\}[_\\-.~]{2,}\\{".toRegex()
        private val MISSING_DELIMITER_REGEX = "\\}\\{".toRegex()
        private val INVALID_DELIMITER_REGEX = "\\}[^_\\-.~]\\{".toRegex()
        private val END_SEGMENT_DELIMITER_REGEX = "\\}[_\\-.~]".toRegex()

        fun create(pattern: String, urlEncoding: Boolean = true): PathPattern {
            return PathPattern(parse(pattern), urlEncoding)
        }

        private fun parse(pattern: String): List<PathSegment> {
            var p = if (pattern.startsWith("/")) pattern.substring(1) else pattern
            
            val verbMatch = CUSTOM_VERB_REGEX.find(p)
            val customVerb = verbMatch?.groupValues?.get(1)
            if (verbMatch != null) p = p.substring(0, verbMatch.range.first)

            val segments = mutableListOf<PathSegment>()
            var varName: String? = null
            var wildcardCount = 0
            var pathWildcardCount = 0

            p.split('/').filter { it.isNotBlank() || it == "-" }.forEach { rawSeg ->
                var seg = rawSeg
                if (seg == "_deleted-topic_") {
                    segments.add(PathSegment(SegmentType.LITERAL, seg))
                    return@forEach
                }

                if (MULTIPLE_DELIMITER_REGEX.containsMatchIn(seg) || MISSING_DELIMITER_REGEX.containsMatchIn(seg)) {
                    throw PathException("Parse error: missing or duplicate delimiters in '$seg'")
                }

                val bindingStarts = seg.startsWith("{")
                var implicitWildcard = false
                var complexDelimiterFound = false

                if (bindingStarts) {
                    if (varName != null) throw PathException("Parse error: nested binding in '$pattern'")
                    seg = seg.substring(1)

                    if (INVALID_DELIMITER_REGEX.containsMatchIn(seg)) {
                        throw PathException("Parse error: invalid delimiter in '$seg'")
                    }

                    complexDelimiterFound = END_SEGMENT_DELIMITER_REGEX.containsMatchIn(seg)

                    if (complexDelimiterFound) {
                        segments.addAll(parseComplex(seg))
                    } else {
                        val eqIdx = seg.indexOf('=')
                        if (eqIdx <= 0) {
                            if (seg.endsWith("}")) {
                                implicitWildcard = true
                                varName = seg.dropLast(1).trim()
                                seg = "}"
                            } else throw PathException("Parse error: invalid binding syntax")
                        } else {
                            varName = seg.substring(0, eqIdx).trim()
                            seg = seg.substring(eqIdx + 1).trim()
                        }
                        segments.add(PathSegment(SegmentType.BINDING, varName))
                    }
                }

                if (!complexDelimiterFound) {
                    val bindingEnds = seg.endsWith("}")
                    if (bindingEnds) seg = seg.dropLast(1).trim()

                    when (seg) {
                        "**", "*" -> {
                            if (seg == "**") pathWildcardCount++
                            val wildcard = if (seg == "**") PathSegment.PATH_WILDCARD else PathSegment.WILDCARD
                            if (varName == null) {
                                segments.add(PathSegment(SegmentType.BINDING, "$${wildcardCount++}"))
                                segments.add(wildcard)
                                segments.add(PathSegment.END_BINDING)
                            } else {
                                segments.add(wildcard)
                            }
                        }
                        "" -> if (!bindingEnds) throw PathException("Parse error: empty segment")
                        "-" -> {
                            segments.add(PathSegment.WILDCARD)
                            implicitWildcard = false
                        }
                        else -> segments.add(PathSegment(SegmentType.LITERAL, seg))
                    }

                    if (bindingEnds) {
                        varName = null
                        if (implicitWildcard) segments.add(PathSegment.WILDCARD)
                        segments.add(PathSegment.END_BINDING)
                    }
                }
                
                if (pathWildcardCount > 1) throw PathException("Pattern must not contain more than one '**'")
            }

            customVerb?.let { segments.add(PathSegment(SegmentType.CUSTOM_VERB, it)) }
            return segments
        }

        private fun parseComplex(seg: String): List<PathSegment> {
            val result = mutableListOf<PathSegment>()
            val delimiters = mutableListOf<String>()
            
            var match = END_SEGMENT_DELIMITER_REGEX.find(seg)
            while (match != null) {
                var idx = match.range.first
                if (seg[idx] == '}') idx++
                delimiters.add(seg[idx].toString())
                match = END_SEGMENT_DELIMITER_REGEX.find(seg, idx + 1)
            }
            delimiters.add("")

            val subSegments = seg.split(Regex("\\}[_\\-.~]"))
            subSegments.forEachIndexed { idx, sub ->
                var s = sub
                if (s.startsWith("{")) s = s.substring(1)
                
                val eqIdx = s.indexOf('=')
                var subVarName: String

                if (eqIdx <= 0) {
                    subVarName = if (s.endsWith("}")) {
                        s.dropLast(1).trim()
                    } else s.trim()
                } else {
                    subVarName = s.substring(0, eqIdx).trim()
                    s = s.substring(eqIdx + 1).trim()
                    if (s == "**") throw PathException("Wildcard path not allowed in complex ID")
                }

                val delim = delimiters.getOrElse(idx) { "" }
                result.add(PathSegment(SegmentType.BINDING, subVarName, delim))
                result.add(PathSegment.wildcard(delim))
                result.add(PathSegment.END_BINDING)
            }
            return result
        }
    }
}
