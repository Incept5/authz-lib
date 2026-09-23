package org.incept5.authz.quarkus.filter

/**
 * Shared path-pattern matcher used by both the authz ignore list ([FilterDecision]) and the
 * MFA-skip list ([AssuranceLevelFilter]). These are NOT full regexes — two wildcards only:
 *
 *  - `*` matches any run of characters, including `/` (mapped to `.*`).
 *  - `{segment}` matches exactly one non-empty path segment (mapped to `[^/]+`), so
 *    `/api/v1/users/{segment}` matches `/api/v1/users/u_1` but not `/api/v1/users/u_1/totp`.
 *
 * A pattern with neither wildcard matches only its exact literal path.
 */
object PathPatternMatcher {

    private const val SEGMENT = "{segment}"

    // A sentinel that cannot appear in a URL path, so the dot-escaping pass leaves it untouched.
    private const val SEGMENT_PLACEHOLDER = " SEG "

    fun matches(pattern: String, path: String): Boolean {
        // Direct match.
        if (pattern == path) return true

        // Wildcard patterns.
        if (pattern.contains("*") || pattern.contains(SEGMENT)) {
            val regex = pattern
                .replace(SEGMENT, SEGMENT_PLACEHOLDER)  // shield {segment} from dot-escaping
                .replace(".", "\\.")                    // escape dots
                .replace("*", ".*")                     // convert * to .*
                .replace(SEGMENT_PLACEHOLDER, "[^/]+")   // one non-empty segment
                .let { "^$it$" }                        // anchor
                .toRegex()
            return regex.matches(path)
        }

        return false
    }
}
