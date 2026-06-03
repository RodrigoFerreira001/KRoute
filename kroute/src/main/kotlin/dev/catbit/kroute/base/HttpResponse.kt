package dev.catbit.kroute.base

/**
 * Represents an outgoing HTTP response.
 *
 * This interface provides methods to define the status code, headers, and body
 * that will be sent back to the client.
 */
interface HttpResponse {

    /**
     * Sets the HTTP status code for this response.
     *
     * @param code The numeric status code (e.g., 200, 404).
     */
    fun setStatusCode(code: Int)

    /**
     * Sets the HTTP status code and a custom reason phrase for this response.
     *
     * @param code The numeric status code.
     * @param message A custom description for the status (e.g., "Success", "Not Found").
     */
    fun setStatusCode(code: Int, message: String)

    /**
     * Sets the HTTP status code using a predefined [HttpStatusCode] enum.
     */
    fun setStatusCode(statusCode: HttpStatusCode)

    /**
     * Sets the "Content-Type" header for the response.
     *
     * @param contentType The media type string (e.g., "application/json", "text/html").
     */
    fun setContentType(contentType: String)

    /**
     * Sets the "Content-Type" header for the response using an [HttpContentType] instance.
     *
     * @param contentType The content type to set (e.g., [HttpContentType.Application.Json]).
     */
    fun setContentType(contentType: HttpContentType)

    /**
     * Appends a value to an HTTP header. If the header already exists, the new value
     * will be added to the list of values for that header name.
     *
     * @param header The name of the header.
     * @param value The value to append.
     */
    fun appendHeader(header: String, value: String)

    /**
     * Returns a mutable map of all headers currently set for this response.
     *
     * Modifying this map directly will affect the response headers.
     *
     * @return A mutable map of header names to their corresponding list of values.
     */
    fun headers(): MutableMap<String, MutableList<String>>

    /**
     * Sets the response body as a string.
     * This method will automatically handle the conversion to bytes and may update
     * headers like "Content-Length" and "Content-Type".
     *
     * @param stringData The string to be used as the response body.
     */
    fun setBody(stringData: String)

    /**
     * Sets the response body as a raw byte array.
     *
     * @param data The byte array to be used as the response body.
     */
    fun setBody(data: ByteArray)
}