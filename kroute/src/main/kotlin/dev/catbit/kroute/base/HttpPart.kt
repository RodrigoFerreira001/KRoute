package dev.catbit.kroute.base

/**
 * Represents a single part of a multipart HTTP request (e.g., a file upload or a form field).
 *
 * Each part behaves like a miniature [HttpMessage], containing its own headers and body.
 */
interface HttpPart : HttpMessage {

    /**
     * Returns the file name associated with this part if it was sent as a file upload.
     *
     * @return The original filename from the "Content-Disposition" header, or null if not applicable.
     */
    fun fileName(): String?
}