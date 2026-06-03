package dev.catbit.kroute.base

/**
 * Standard HTTP header names.
 */
object HttpHeaders {

    // General
    const val CacheControl = "Cache-Control"
    const val Connection = "Connection"
    const val Date = "Date"
    const val Pragma = "Pragma"
    const val Trailer = "Trailer"
    const val TransferEncoding = "Transfer-Encoding"
    const val Upgrade = "Upgrade"
    const val Via = "Via"
    const val Warning = "Warning"

    // Request
    const val Accept = "Accept"
    const val AcceptEncoding = "Accept-Encoding"
    const val AcceptLanguage = "Accept-Language"
    const val AcceptRanges = "Accept-Ranges"
    const val Authorization = "Authorization"
    const val Cookie = "Cookie"
    const val Expect = "Expect"
    const val Forwarded = "Forwarded"
    const val From = "From"
    const val Host = "Host"
    const val IfMatch = "If-Match"
    const val IfModifiedSince = "If-Modified-Since"
    const val IfNoneMatch = "If-None-Match"
    const val IfRange = "If-Range"
    const val IfUnmodifiedSince = "If-Unmodified-Since"
    const val MaxForwards = "Max-Forwards"
    const val Origin = "Origin"
    const val ProxyAuthorization = "Proxy-Authorization"
    const val Range = "Range"
    const val Referrer = "Referer"
    const val UserAgent = "User-Agent"

    // Response
    const val Age = "Age"
    const val Allow = "Allow"
    const val AuthenticationInfo = "Authentication-Info"
    const val ContentDisposition = "Content-Disposition"
    const val ContentEncoding = "Content-Encoding"
    const val ContentLanguage = "Content-Language"
    const val ContentLength = "Content-Length"
    const val ContentLocation = "Content-Location"
    const val ContentRange = "Content-Range"
    const val ContentType = "Content-Type"
    const val ETag = "ETag"
    const val Expires = "Expires"
    const val LastModified = "Last-Modified"
    const val Link = "Link"
    const val Location = "Location"
    const val ProxyAuthenticate = "Proxy-Authenticate"
    const val ProxyAuthenticationInfo = "Proxy-Authentication-Info"
    const val RetryAfter = "Retry-After"
    const val Server = "Server"
    const val SetCookie = "Set-Cookie"
    const val StrictTransportSecurity = "Strict-Transport-Security"
    const val Vary = "Vary"
    const val WWWAuthenticate = "WWW-Authenticate"

    // CORS
    const val AccessControlAllowOrigin = "Access-Control-Allow-Origin"
    const val AccessControlAllowMethods = "Access-Control-Allow-Methods"
    const val AccessControlAllowCredentials = "Access-Control-Allow-Credentials"
    const val AccessControlAllowHeaders = "Access-Control-Allow-Headers"
    const val AccessControlRequestMethod = "Access-Control-Request-Method"
    const val AccessControlRequestHeaders = "Access-Control-Request-Headers"
    const val AccessControlExposeHeaders = "Access-Control-Expose-Headers"
    const val AccessControlMaxAge = "Access-Control-Max-Age"

    // Common non-standard
    const val XHttpMethodOverride = "X-Http-Method-Override"
    const val XForwardedFor = "X-Forwarded-For"
    const val XForwardedHost = "X-Forwarded-Host"
    const val XForwardedProto = "X-Forwarded-Proto"
    const val XForwardedPort = "X-Forwarded-Port"
    const val XForwardedServer = "X-Forwarded-Server"
    const val XRequestId = "X-Request-ID"
    const val XCorrelationId = "X-Correlation-ID"
    const val XTotalCount = "X-Total-Count"

    // Server-Sent Events
    const val LastEventId = "Last-Event-ID"

    /**
     * Returns `true` if [header] is controlled by the engine and should not be set manually.
     * Unsafe headers: [TransferEncoding], [Upgrade].
     */
    fun isUnsafe(header: String): Boolean =
        header.equals(TransferEncoding, ignoreCase = true) || header.equals(Upgrade, ignoreCase = true)
}
