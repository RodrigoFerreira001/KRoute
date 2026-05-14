package dev.catbit.kroute.base

/**
 * Represents an incoming HTTP request.
 *
 * Provides access to the request's metadata, such as method, URI, path, and parameters,
 * as well as its body and headers via the [HttpMessage] interface.
 */
interface HttpRequest : HttpMessage {

    /**
     * Returns the name of the HTTP method with which this request was made (e.g., "GET", "POST").
     */
    fun method(): String

    /**
     * Returns the full URI of the request, including the scheme, host, port, path, and query string.
     */
    fun uri(): String

    /**
     * Returns the path component of the URI (e.g., "/api/users").
     */
    fun path(): String

    /**
     * Returns the raw query string that was included in the request URI after the path.
     * Example: "id=123&name=foo".
     *
     * @return The query string or null if the URI contains no query string.
     */
    fun query(): String?

    /**
     * Returns all query parameters for this request.
     *
     * Parameters are represented as a map where each key can have multiple values
     * to support repeated parameters in the URL (e.g., `?tag=kotlin&tag=jvm`).
     *
     * @return A map of parameter names to their corresponding list of values.
     */
    fun queryParameters(): Map<String, List<String>>

    /**
     * A convenience method to retrieve the first value of a specific query parameter.
     *
     * @param paramName The name of the query parameter.
     * @return The first value associated with the name, or null if not present.
     */
    fun queryParameter(paramName: String): String?

    /**
     * Returns a map of all parts included in a multipart request.
     *
     * This is used for accessing file uploads and form fields in "multipart/form-data" requests.
     *
     * @return A map where the key is the name of the part and the value is the [HttpPart] object.
     */
    fun parts(): Map<String, HttpPart>
}