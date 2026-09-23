package org.incept5.authz.quarkus.filter

// Contribute to the set of path patterns that should be ignored by the authz filter.
//
// These are NOT full regexes — PathPatternMatcher (shared with the MFA skip list) matches every
// character literally and understands two wildcards only:
//   - a star matches any run of characters, including "/". e.g. "/auth/" followed by a star
//     ignores every sub-path of /auth/.
//   - "{segment}" matches exactly one non-empty path segment. e.g.
//     "/api/v1/payment-sessions/{segment}" ignores "/api/v1/payment-sessions/ps_123" but NOT
//     "/api/v1/payment-sessions/ps_123/enhanced".
//
// A pattern with neither wildcard matches only its exact literal path (e.g. "/auth/public-keys").
//
// NOTE: line comments, not a KDoc block — a "/" immediately followed by a star opens a nested
// block comment in Kotlin and would swallow the closing delimiter.
interface IgnoreAuthzFilterProvider {

    fun ignoreRegexes(): List<String>

}