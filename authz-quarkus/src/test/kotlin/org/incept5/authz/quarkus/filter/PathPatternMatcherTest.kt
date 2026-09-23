package org.incept5.authz.quarkus.filter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathPatternMatcherTest {

    private fun matches(pattern: String, path: String) = PathPatternMatcher.compile(pattern).matches(path)

    @Test
    fun `pattern without wildcards matches only its exact path`() {
        assertTrue(matches("/health", "/health"))
        assertFalse(matches("/health", "/health/"))
        assertFalse(matches("/health", "/healthz"))
        assertFalse(matches("/health", "/api/health"))
    }

    @Test
    fun `star spans path separators`() {
        assertTrue(matches("/api/v1/public/*", "/api/v1/public/users"))
        assertTrue(matches("/api/v1/public/*", "/api/v1/public/users/u_1/detail"))
        assertTrue(matches("/api/v1/public/*", "/api/v1/public/"))
        assertFalse(matches("/api/v1/public/*", "/api/v1/private/users"))
    }

    @Test
    fun `segment matches exactly one non-empty segment`() {
        assertTrue(matches("/api/v1/users/{segment}", "/api/v1/users/u_1"))
        assertFalse(matches("/api/v1/users/{segment}", "/api/v1/users/u_1/totp"))
        assertFalse(matches("/api/v1/users/{segment}", "/api/v1/users/"))
        assertTrue(matches("/api/v1/users/{segment}/totp", "/api/v1/users/u_1/totp"))
    }

    @Test
    fun `dots are literal`() {
        assertTrue(matches("/api/v1/files/*.json", "/api/v1/files/report.json"))
        assertFalse(matches("/api/v1/files/*.json", "/api/v1/files/reportXjson"))
        // A regex-style pattern is NOT a regex: the dot is literal, so this only matches a dotted path.
        assertFalse(matches("/public/.*", "/public/foo"))
        assertTrue(matches("/public/.*", "/public/.hidden"))
    }

    @Test
    fun `regex metacharacters other than the wildcards are literal and never throw`() {
        // JAX-RS-style placeholders used to throw PatternSyntaxException("Illegal repetition").
        assertTrue(matches("/api/v1/users/{userId}/totp/*", "/api/v1/users/{userId}/totp/enrol"))
        assertFalse(matches("/api/v1/users/{userId}/totp/*", "/api/v1/users/u_1/totp/enrol"))
        // '+' used to be a quantifier.
        assertTrue(matches("/foo+bar/*", "/foo+bar/x"))
        assertFalse(matches("/foo+bar/*", "/foooobar/x"))
        // '[', '(', '?', '$', '|', '\' and an unbalanced bracket.
        assertTrue(matches("/x[1]/*", "/x[1]/y"))
        assertFalse(matches("/x[1]/*", "/x1/y"))
        assertTrue(matches("/a(b)?c|d\$e\\f", "/a(b)?c|d\$e\\f"))
        assertTrue(matches("/open[bracket/*", "/open[bracket/x"))
    }

    @Test
    fun `wildcards can be combined and appear at either end`() {
        assertTrue(matches("*/health", "/internal/health"))
        assertTrue(matches("/{segment}/{segment}", "/a/b"))
        assertFalse(matches("/{segment}/{segment}", "/a/b/c"))
        assertTrue(matches("/api/{segment}/items/*", "/api/v2/items/1/2"))
        assertTrue(matches("*", "/anything/at/all"))
    }

    @Test
    fun `compileAll preserves order`() {
        val compiled = PathPatternMatcher.compileAll(listOf("/a", "/b/*"))
        assertTrue(compiled[0].matches("/a"))
        assertTrue(compiled[1].matches("/b/c"))
    }
}
