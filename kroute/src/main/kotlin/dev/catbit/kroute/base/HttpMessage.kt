package dev.catbit.kroute.base

/**
 * Represents a generic HTTP message containing common elements of both requests and responses.
 *
 * This interface provides access to the shared structure of HTTP communication, including
 * headers, body content, and metadata such as content type and encoding.
 */
interface HttpMessage {

    /**
     * Returns the "Content-Type" header value of the message, if present.
     * Usually describes the media type of the body (e.g., "application/json").
     */
    fun contentType(): String?

    /**
     * Returns the size of the message body in bytes, as specified by the "Content-Length" header.
     */
    fun contentLength(): Long

    /**
     * Returns all HTTP headers for this message.
     *
     * Headers are represented as a map where each key can have multiple values,
     * following the standard that allows repeated header fields in HTTP.
     *
     * @return A map of header names to their corresponding list of values.
     */
    fun headers(): Map<String, List<String>>

    /**
     * Returns the name of the character encoding (charset) used in the body of this message.
     * Example: "UTF-8".
     */
    fun characterEncoding(): String?

    /**
     * Returns the raw bytes of the message body.
     */
    fun body(): ByteArray
}