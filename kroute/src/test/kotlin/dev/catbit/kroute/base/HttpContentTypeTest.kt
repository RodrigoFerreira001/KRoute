package dev.catbit.kroute.base

import kotlin.test.*

class HttpContentTypeTest {

    // --- value / toString ---

    @Test
    fun `value returns type slash subtype without parameters`() {
        val ct = HttpContentType("application", "json")
        assertEquals("application/json", ct.value)
    }

    @Test
    fun `value includes parameters separated by semicolons`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))
        assertEquals("text/plain; charset=UTF-8", ct.value)
    }

    @Test
    fun `toString returns value`() {
        val ct = HttpContentType("text", "html")
        assertEquals(ct.value, ct.toString())
    }

    // --- withParameter ---

    @Test
    fun `withParameter adds a new parameter`() {
        val ct = HttpContentType("text", "plain").withParameter("charset", "UTF-8")
        assertEquals("UTF-8", ct.parameters["charset"])
    }

    @Test
    fun `withParameter returns same instance when parameter already exists with same value`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))
        assertSame(ct, ct.withParameter("charset", "UTF-8"))
    }

    @Test
    fun `withParameter returns same instance when value matches case-insensitively`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "utf-8"))
        assertSame(ct, ct.withParameter("charset", "UTF-8"))
    }

    @Test
    fun `withParameter replaces parameter with different value`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "ISO-8859-1"))
            .withParameter("charset", "UTF-8")
        assertEquals("UTF-8", ct.parameters["charset"])
    }

    // --- withoutParameters ---

    @Test
    fun `withoutParameters removes all parameters`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))
        val stripped = ct.withoutParameters()
        assertTrue(stripped.parameters.isEmpty())
        assertEquals("text/plain", stripped.value)
    }

    @Test
    fun `withoutParameters returns same instance when already empty`() {
        val ct = HttpContentType("text", "plain")
        assertSame(ct, ct.withoutParameters())
    }

    // --- withCharset ---

    @Test
    fun `withCharset adds charset parameter`() {
        val ct = HttpContentType.Text.Plain.withCharset("UTF-8")
        assertEquals("UTF-8", ct.parameters["charset"])
        assertEquals("text/plain; charset=UTF-8", ct.value)
    }

    // --- match(HttpContentType) ---

    @Test
    fun `match returns true for identical types`() {
        val ct = HttpContentType("application", "json")
        assertTrue(ct.match(HttpContentType("application", "json")))
    }

    @Test
    fun `match is case-insensitive`() {
        val ct = HttpContentType("Application", "JSON")
        assertTrue(ct.match(HttpContentType("application", "json")))
    }

    @Test
    fun `match returns true when pattern subtype is wildcard`() {
        val ct = HttpContentType("application", "json")
        assertTrue(ct.match(HttpContentType("application", "*")))
    }

    @Test
    fun `match returns true when pattern type is wildcard`() {
        val ct = HttpContentType("application", "json")
        assertTrue(ct.match(HttpContentType("*", "*")))
    }

    @Test
    fun `match returns false when pattern subtype is concrete and does not match`() {
        val ct = HttpContentType("application", "json")
        assertFalse(ct.match(HttpContentType("application", "xml")))
    }

    @Test
    fun `match returns false when receiver subtype is wildcard and pattern is concrete`() {
        val ct = HttpContentType("application", "*")
        assertFalse(ct.match(HttpContentType("application", "json")))
    }

    @Test
    fun `match returns true when receiver has parameter that pattern requires`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))
        assertTrue(ct.match(HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))))
    }

    @Test
    fun `match returns false when receiver lacks parameter required by pattern`() {
        val ct = HttpContentType("text", "plain")
        assertFalse(ct.match(HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))))
    }

    @Test
    fun `match returns true when receiver has extra parameters not in pattern`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8", "format" to "fixed"))
        assertTrue(ct.match(HttpContentType("text", "plain")))
    }

    @Test
    fun `match returns true when pattern parameter value is wildcard`() {
        val ct = HttpContentType("text", "plain", mapOf("charset" to "UTF-8"))
        assertTrue(ct.match(HttpContentType("text", "plain", mapOf("charset" to "*"))))
    }

    // --- match(String) ---

    @Test
    fun `match string delegates to parse and match`() {
        val ct = HttpContentType("application", "json")
        assertTrue(ct.match("application/json"))
        assertTrue(ct.match("application/*"))
        assertFalse(ct.match("text/plain"))
    }

    // --- parse ---

    @Test
    fun `parse returns Any for blank string`() {
        assertEquals(HttpContentType.Any, HttpContentType.parse("  "))
    }

    @Test
    fun `parse returns Any for bare wildcard`() {
        assertEquals(HttpContentType.Any, HttpContentType.parse("*"))
    }

    @Test
    fun `parse handles simple type and subtype`() {
        val ct = HttpContentType.parse("application/json")
        assertEquals("application", ct.type)
        assertEquals("json", ct.subtype)
        assertTrue(ct.parameters.isEmpty())
    }

    @Test
    fun `parse handles parameters`() {
        val ct = HttpContentType.parse("text/plain; charset=UTF-8")
        assertEquals("text", ct.type)
        assertEquals("plain", ct.subtype)
        assertEquals("UTF-8", ct.parameters["charset"])
    }

    @Test
    fun `parse handles multiple parameters`() {
        val ct = HttpContentType.parse("multipart/form-data; boundary=abc123; charset=UTF-8")
        assertEquals("abc123", ct.parameters["boundary"])
        assertEquals("UTF-8", ct.parameters["charset"])
    }

    @Test
    fun `parse throws on missing slash`() {
        assertFailsWith<IllegalArgumentException> { HttpContentType.parse("applicationjson") }
    }

    @Test
    fun `parse throws on empty subtype`() {
        assertFailsWith<IllegalArgumentException> { HttpContentType.parse("application/") }
    }

    @Test
    fun `parse throws on double slash in subtype`() {
        assertFailsWith<IllegalArgumentException> { HttpContentType.parse("application/json/extra") }
    }

    // --- Any ---

    @Test
    fun `Any matches every content type`() {
        assertTrue(HttpContentType("application", "json").match(HttpContentType.Any))
        assertTrue(HttpContentType("text", "html").match(HttpContentType.Any))
        assertTrue(HttpContentType("image", "png").match(HttpContentType.Any))
    }

    // --- Category contains(String) ---

    @Test
    fun `Application contains matching string`() {
        assertTrue("application/json" in HttpContentType.Application)
        assertTrue("APPLICATION/JSON" in HttpContentType.Application)
        assertFalse("text/plain" in HttpContentType.Application)
    }

    @Test
    fun `Audio contains matching string`() {
        assertTrue("audio/mpeg" in HttpContentType.Audio)
        assertFalse("video/mp4" in HttpContentType.Audio)
    }

    @Test
    fun `Image contains matching string`() {
        assertTrue("image/png" in HttpContentType.Image)
        assertFalse("application/json" in HttpContentType.Image)
    }

    @Test
    fun `Text contains matching string`() {
        assertTrue("text/html" in HttpContentType.Text)
        assertFalse("application/json" in HttpContentType.Text)
    }

    @Test
    fun `Video contains matching string`() {
        assertTrue("video/mp4" in HttpContentType.Video)
        assertFalse("audio/mpeg" in HttpContentType.Video)
    }

    @Test
    fun `MultiPart contains matching string`() {
        assertTrue("multipart/form-data" in HttpContentType.MultiPart)
        assertFalse("application/json" in HttpContentType.MultiPart)
    }

    @Test
    fun `Message contains matching string`() {
        assertTrue("message/http" in HttpContentType.Message)
        assertFalse("text/plain" in HttpContentType.Message)
    }

    @Test
    fun `Font contains matching string`() {
        assertTrue("font/woff2" in HttpContentType.Font)
        assertFalse("text/plain" in HttpContentType.Font)
    }

    // --- Category contains(HttpContentType) ---

    @Test
    fun `Application contains HttpContentType of same type`() {
        assertTrue(HttpContentType.Application.Json in HttpContentType.Application)
        assertFalse(HttpContentType.Text.Plain in HttpContentType.Application)
    }

    @Test
    fun `Text contains HttpContentType of same type`() {
        assertTrue(HttpContentType.Text.Html in HttpContentType.Text)
        assertFalse(HttpContentType.Application.Json in HttpContentType.Text)
    }

    @Test
    fun `Image contains HttpContentType of same type`() {
        assertTrue(HttpContentType.Image.PNG in HttpContentType.Image)
        assertFalse(HttpContentType.Application.Json in HttpContentType.Image)
    }

    // --- isTextType ---

    @Test
    fun `isTextType returns true for text type`() {
        assertTrue(HttpContentType.Text.Plain.isTextType())
        assertTrue(HttpContentType.Text.Html.isTextType())
    }

    @Test
    fun `isTextType returns true for text-based application subtypes`() {
        assertTrue(HttpContentType.Application.Json.isTextType())
        assertTrue(HttpContentType.Application.Xml.isTextType())
        assertTrue(HttpContentType.Application.Atom.isTextType())
        assertTrue(HttpContentType.Application.Rss.isTextType())
        assertTrue(HttpContentType.Application.FormUrlEncoded.isTextType())
    }

    @Test
    fun `isTextType returns false for binary types`() {
        assertFalse(HttpContentType.Application.OctetStream.isTextType())
        assertFalse(HttpContentType.Image.PNG.isTextType())
        assertFalse(HttpContentType.Video.MP4.isTextType())
        assertFalse(HttpContentType.Application.Pdf.isTextType())
    }

    // --- Well-known constants sanity checks ---

    @Test
    fun `well-known constants have correct values`() {
        assertEquals("application/json", HttpContentType.Application.Json.value)
        assertEquals("text/plain", HttpContentType.Text.Plain.value)
        assertEquals("image/png", HttpContentType.Image.PNG.value)
        assertEquals("multipart/form-data", HttpContentType.MultiPart.FormData.value)
        assertEquals("video/mp4", HttpContentType.Video.MP4.value)
        assertEquals("font/woff2", HttpContentType.Font.Woff2.value)
        assertEquals("audio/mpeg", HttpContentType.Audio.MPEG.value)
        assertEquals("message/http", HttpContentType.Message.Http.value)
    }
}
