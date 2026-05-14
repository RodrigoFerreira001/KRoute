package dev.catbit.kroute.path

import kotlin.test.*

class PathPatternTest {

    @Test
    fun `test basic literal matching`() {
        val pattern = PathPattern.create("v1/shelves")
        assertTrue(pattern.matches("v1/shelves"))
        assertFalse(pattern.matches("v1/books"))
        assertFalse(pattern.matches("v2/shelves"))
    }

    @Test
    fun `test simple variable binding`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}/books/{book}")
        val match = pattern.match("v1/shelves/s1/books/b1")
        
        assertNotNull(match)
        assertEquals("s1", match["shelf"])
        assertEquals("b1", match["book"])
    }

    @Test
    fun `test positional wildcards`() {
        val pattern = PathPattern.create("shelves/*/books/*")
        val match = pattern.match("shelves/s1/books/b1")
        
        assertNotNull(match)
        assertEquals("s1", match["$0"])
        assertEquals("b1", match["$1"])
    }

    @Test
    fun `test path wildcard`() {
        val pattern = PathPattern.create("v1/{name=shelves/**}/notes")
        val match = pattern.match("v1/shelves/s1/sub/books/notes")
        
        assertNotNull(match)
        assertEquals("shelves/s1/sub/books", match["name"])
    }

    @Test
    fun `test complex resource identifiers`() {
        // Syntax for complex IDs: {user_a}~{user_b}
        val pattern = PathPattern.create("v1/comparisons/{id1}~{id2}")
        val match = pattern.match("v1/comparisons/user1~user2")
        
        assertNotNull(match)
        assertEquals("user1", match["id1"])
        assertEquals("user2", match["id2"])
    }

    @Test
    fun `test url encoding and decoding`() {
        val pattern = PathPattern.create("v1/{name}")
        
        // Match with encoded space
        val match = pattern.match("v1/hello%20world")
        assertNotNull(match)
        assertEquals("hello world", match["name"])
        
        // Instantiate with special characters
        val instantiated = pattern.instantiate("name", "a/b c")
        // 'a/b c' -> 'a%2Fb%20c' (if segments are escaped) 
        // Note: Our PathPattern implementation splits by '/' for instantiation if it's a multi-segment var, 
        // but for a simple var it escapes the whole thing.
        assertEquals("v1/a%2Fb%20c", instantiated)
    }

    @Test
    fun `test custom verb`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}:undelete")
        
        assertTrue(pattern.matches("v1/shelves/s1:undelete"))
        assertFalse(pattern.matches("v1/shelves/s1"))
        
        val match = pattern.match("v1/shelves/s1:undelete")
        assertNotNull(match)
        assertEquals("s1", match["shelf"])
    }

    @Test
    fun `test hostname matching`() {
        val pattern = PathPattern.create("shelves/{shelf}")
        
        val match = pattern.match("//example.com/shelves/s1")
        assertNotNull(match)
        assertEquals("//example.com", match[PathPattern.HOSTNAME_VAR])
        assertEquals("s1", match["shelf"])
    }

    @Test
    fun `test parent and sub patterns`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}/books/{book}")
        
        assertEquals("v1/shelves/{shelf}/books", pattern.parentPattern().toString())
        assertEquals("*", pattern.subPattern("shelf").toString())
    }

    @Test
    fun `test invalid patterns`() {
        assertFailsWith<PathException> {
            PathPattern.create("v1/{name=**}/{other=**}") // Only one path wildcard allowed
        }
        assertFailsWith<PathException> {
            PathPattern.create("v1/{name}/{name}") // Duplicate bindings
        }
    }

    @Test
    fun `test path wildcard matches empty`() {
        val pattern = PathPattern.create("{name=**}")
        val match = pattern.match("")
        assertNotNull(match)
        assertEquals("", match["name"])
    }

    @Test
    fun `test withoutVars replaces bindings with wildcards`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}/books/{book}")
        assertEquals("v1/shelves/*/books/*", pattern.withoutVars().toString())
    }

    @Test
    fun `test singleVar`() {
        assertNull(PathPattern.create("v1/shelves/{shelf}/books/{book}").singleVar)
        assertEquals("shelf", PathPattern.create("v1/shelves/{shelf}").singleVar)
    }

    @Test
    fun `test endsWithLiteral and endsWithCustomVerb`() {
        assertTrue(PathPattern.create("v1/shelves").endsWithLiteral)
        assertFalse(PathPattern.create("v1/{shelf}").endsWithLiteral)
        assertTrue(PathPattern.create("v1/shelves/{shelf}:undelete").endsWithCustomVerb)
        assertFalse(PathPattern.create("v1/shelves").endsWithCustomVerb)
    }

    @Test
    fun `test instantiate with vararg`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}/books/{book}")
        assertEquals("v1/shelves/s1/books/b1", pattern.instantiate("shelf", "s1", "book", "b1"))
    }

    @Test
    fun `test instantiate throws on unbound variable`() {
        val pattern = PathPattern.create("v1/shelves/{shelf}")
        assertFailsWith<PathException> {
            pattern.instantiate(emptyMap())
        }
    }

    @Test
    fun `test specificity score`() {
        // Literals beat params, params beat wildcards
        val literal   = PathPattern.create("users/admin")
        val param     = PathPattern.create("users/{id}")
        val wildcard  = PathPattern.create("users/**")
        val catchAll  = PathPattern.create("**")

        assertTrue(literal.specificityScore > param.specificityScore)
        assertTrue(param.specificityScore > wildcard.specificityScore)
        assertTrue(wildcard.specificityScore > catchAll.specificityScore)

        // More specific path (more segments) beats shorter
        val longer = PathPattern.create("users/{id}/profile")
        assertTrue(longer.specificityScore > param.specificityScore)
    }
}

class UrlCodecTest {
    
    @Test
    fun `test encoding`() {
        assertEquals("hello%20world", UrlCodec.encode("hello world"))
        assertEquals("a-b_c.d~e", UrlCodec.encode("a-b_c.d~e")) // Unreserved characters
        assertEquals("%21%40%23%24", UrlCodec.encode("!@#$"))
    }

    @Test
    fun `test decoding`() {
        assertEquals("hello world", UrlCodec.decode("hello%20world"))
        assertEquals("hello+world", UrlCodec.decode("hello+world")) // + is literal in path context
        assertEquals("hello world", UrlCodec.decode("hello+world", plusAsSpace = true))
        assertEquals("a/b", UrlCodec.decode("a%2Fb"))
    }

    @Test
    fun `test unicode encoding-decoding`() {
        val original = "café"
        val encoded = UrlCodec.encode(original)
        assertEquals("caf%C3%A9", encoded)
        assertEquals(original, UrlCodec.decode(encoded))
    }
}
