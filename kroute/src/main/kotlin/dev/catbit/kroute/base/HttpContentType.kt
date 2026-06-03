package dev.catbit.kroute.base

/**
 * Represents the value of a `Content-Type` header.
 *
 * @property type The primary type part of the media type (e.g., "application", "text").
 * @property subtype The subtype part of the media type (e.g., "json", "plain").
 * @property parameters Optional parameters associated with the media type (e.g., `charset=UTF-8`).
 */
data class HttpContentType(
    val type: String,
    val subtype: String,
    val parameters: Map<String, String> = emptyMap()
) {

    /**
     * The full string representation of this content type, including any parameters.
     */
    val value: String
        get() {
            val params = parameters.entries.joinToString("; ") { "${it.key}=${it.value}" }
            return if (params.isEmpty()) "$type/$subtype" else "$type/$subtype; $params"
        }

    override fun toString(): String = value

    /**
     * Returns a copy of this content type with an additional parameter.
     * If the parameter already exists with the same value (case-insensitive), returns `this` unchanged.
     */
    fun withParameter(name: String, value: String): HttpContentType {
        if (parameters[name]?.equals(value, ignoreCase = true) == true) return this
        return copy(parameters = parameters + (name to value))
    }

    /**
     * Returns a copy of this content type without any parameters.
     */
    fun withoutParameters(): HttpContentType =
        if (parameters.isEmpty()) this else copy(parameters = emptyMap())

    /**
     * Returns a copy of this content type with the charset parameter set.
     */
    fun withCharset(charset: String): HttpContentType = withParameter("charset", charset)

    /**
     * Checks if this content type matches the given [pattern], considering `*` as a wildcard
     * for both type and subtype, and matching all pattern parameters against this content type's parameters.
     *
     * The receiver must be at least as specific as the pattern:
     * ```
     * HttpContentType("a", "b").match(HttpContentType("a", "b").withParameter("foo", "bar")) == false
     * HttpContentType("a", "b").withParameter("foo", "bar").match(HttpContentType("a", "b")) == true
     * HttpContentType("a", "*").match(HttpContentType("a", "b")) == false
     * HttpContentType("a", "b").match(HttpContentType("a", "*")) == true
     * ```
     */
    fun match(pattern: HttpContentType): Boolean {
        if (pattern.type != "*" && !pattern.type.equals(type, ignoreCase = true)) return false
        if (pattern.subtype != "*" && !pattern.subtype.equals(subtype, ignoreCase = true)) return false
        for ((name, patternValue) in pattern.parameters) {
            if (patternValue != "*" && !patternValue.equals(parameters[name], ignoreCase = true)) return false
        }
        return true
    }

    /**
     * Checks if this content type matches the given [pattern] string.
     *
     * @throws IllegalArgumentException if [pattern] is not a valid content type string.
     */
    fun match(pattern: String): Boolean = match(parse(pattern))

    companion object {

        /** Represents `*&#47;*`, matching any content type. */
        val Any: HttpContentType = HttpContentType("*", "*")

        /**
         * Parses a `Content-Type` header string into an [HttpContentType].
         *
         * @throws IllegalArgumentException if [value] is not a valid content type format.
         */
        fun parse(value: String): HttpContentType {
            if (value.isBlank()) return Any

            val parts = value.split(";").map { it.trim() }
            val typeSubtype = parts.first()
            val slash = typeSubtype.indexOf('/')

            if (slash == -1) {
                if (typeSubtype.trim() == "*") return Any
                throw IllegalArgumentException("Invalid Content-Type format: $value")
            }

            val type = typeSubtype.take(slash).trim()
            val subtype = typeSubtype.substring(slash + 1).trim()

            if (type.isEmpty() || subtype.isEmpty() || subtype.contains('/')) {
                throw IllegalArgumentException("Invalid Content-Type format: $value")
            }

            val parameters = parts.drop(1)
                .mapNotNull { param ->
                    val eq = param.indexOf('=')
                    if (eq == -1) null else param.take(eq).trim() to param.substring(eq + 1).trim()
                }
                .toMap()

            return HttpContentType(type, subtype, parameters)
        }
    }

    /** Standard subtypes for the `application` media type. */
    object Application {
        const val TYPE = "application"
        val Any = HttpContentType(TYPE, "*")
        val Atom = HttpContentType(TYPE, "atom+xml")
        val Cbor = HttpContentType(TYPE, "cbor")
        val Json = HttpContentType(TYPE, "json")
        val HalJson = HttpContentType(TYPE, "hal+json")
        val JavaScript = HttpContentType(TYPE, "javascript")
        val OctetStream = HttpContentType(TYPE, "octet-stream")
        val Rss = HttpContentType(TYPE, "rss+xml")
        val Soap = HttpContentType(TYPE, "soap+xml")
        val Xml = HttpContentType(TYPE, "xml")
        val Yaml = HttpContentType(TYPE, "yaml")
        val Zip = HttpContentType(TYPE, "zip")
        val GZip = HttpContentType(TYPE, "gzip")
        val FormUrlEncoded = HttpContentType(TYPE, "x-www-form-urlencoded")
        val Pdf = HttpContentType(TYPE, "pdf")
        val Xlsx = HttpContentType(TYPE, "vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        val Docx = HttpContentType(TYPE, "vnd.openxmlformats-officedocument.wordprocessingml.document")
        val Pptx = HttpContentType(TYPE, "vnd.openxmlformats-officedocument.presentationml.presentation")
        val ProtoBuf = HttpContentType(TYPE, "protobuf")
        val Wasm = HttpContentType(TYPE, "wasm")
        val ProblemJson = HttpContentType(TYPE, "problem+json")
        val ProblemXml = HttpContentType(TYPE, "problem+xml")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `audio` media type. */
    object Audio {
        const val TYPE = "audio"
        val Any = HttpContentType(TYPE, "*")
        val MP4 = HttpContentType(TYPE, "mp4")
        val MPEG = HttpContentType(TYPE, "mpeg")
        val OGG = HttpContentType(TYPE, "ogg")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `image` media type. */
    object Image {
        const val TYPE = "image"
        val Any = HttpContentType(TYPE, "*")
        val APNG = HttpContentType(TYPE, "apng")
        val AVIF = HttpContentType(TYPE, "avif")
        val BMP = HttpContentType(TYPE, "bmp")
        val GIF = HttpContentType(TYPE, "gif")
        val HEIC = HttpContentType(TYPE, "heic")
        val HEIF = HttpContentType(TYPE, "heif")
        val JPEG = HttpContentType(TYPE, "jpeg")
        val PNG = HttpContentType(TYPE, "png")
        val SVG = HttpContentType(TYPE, "svg+xml")
        val TIFF = HttpContentType(TYPE, "tiff")
        val WEBP = HttpContentType(TYPE, "webp")
        val XIcon = HttpContentType(TYPE, "x-icon")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `message` media type. */
    object Message {
        const val TYPE = "message"
        val Any = HttpContentType(TYPE, "*")
        val Http = HttpContentType(TYPE, "http")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `multipart` media type. */
    object MultiPart {
        const val TYPE = "multipart"
        val Any = HttpContentType(TYPE, "*")
        val Mixed = HttpContentType(TYPE, "mixed")
        val Alternative = HttpContentType(TYPE, "alternative")
        val Related = HttpContentType(TYPE, "related")
        val FormData = HttpContentType(TYPE, "form-data")
        val Signed = HttpContentType(TYPE, "signed")
        val Encrypted = HttpContentType(TYPE, "encrypted")
        val ByteRanges = HttpContentType(TYPE, "byteranges")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `text` media type. */
    object Text {
        const val TYPE = "text"
        val Any = HttpContentType(TYPE, "*")
        val Plain = HttpContentType(TYPE, "plain")
        val CSS = HttpContentType(TYPE, "css")
        val CSV = HttpContentType(TYPE, "csv")
        val Html = HttpContentType(TYPE, "html")
        val JavaScript = HttpContentType(TYPE, "javascript")
        val VCard = HttpContentType(TYPE, "vcard")
        val Xml = HttpContentType(TYPE, "xml")
        val EventStream = HttpContentType(TYPE, "event-stream")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `video` media type. */
    object Video {
        const val TYPE = "video"
        val Any = HttpContentType(TYPE, "*")
        val MPEG = HttpContentType(TYPE, "mpeg")
        val MP4 = HttpContentType(TYPE, "mp4")
        val OGG = HttpContentType(TYPE, "ogg")
        val QuickTime = HttpContentType(TYPE, "quicktime")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    /** Standard subtypes for the `font` media type. */
    object Font {
        const val TYPE = "font"
        val Any = HttpContentType(TYPE, "*")
        val Collection = HttpContentType(TYPE, "collection")
        val Otf = HttpContentType(TYPE, "otf")
        val Sfnt = HttpContentType(TYPE, "sfnt")
        val Ttf = HttpContentType(TYPE, "ttf")
        val Woff = HttpContentType(TYPE, "woff")
        val Woff2 = HttpContentType(TYPE, "woff2")

        operator fun contains(contentType: String): Boolean =
            contentType.startsWith("$TYPE/", ignoreCase = true)

        operator fun contains(contentType: HttpContentType): Boolean = contentType.match(Any)
    }

    private val textSubtypes = setOf(
        "json",
        "ld+json",
        "xml",
        "xhtml+xml",
        "rss+xml",
        "atom+xml",
        "x-www-form-urlencoded",
        "svg+xml"
    )

    /**
     * Returns `true` if this content type represents textual data (e.g., `text/\*` or common
     * text-based `application/` subtypes like `application/json`).
     */

    fun isTextType(): Boolean {
        if (type == "text") return true
        if (type == "application" && subtype.lowercase() in textSubtypes) return true
        return false
    }
}